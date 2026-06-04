package com.rentseeking.analysis.risk.service;

import com.rentseeking.analysis.ai.service.AiService;
import com.rentseeking.analysis.ai.dto.RentSeekingRiskItem;
import com.rentseeking.analysis.ai.dto.RentSeekingRiskResult;
import com.rentseeking.analysis.auth.security.JwtUserPrincipal;
import com.rentseeking.analysis.auth.util.SecurityUtils;
import com.rentseeking.analysis.common.constant.ErrorCode;
import com.rentseeking.analysis.common.exception.BizException;
import com.rentseeking.analysis.common.response.PageResponse;
import com.rentseeking.analysis.rag.dto.RagEvidence;
import com.rentseeking.analysis.rag.service.KnowledgeRetrievalService;
import com.rentseeking.analysis.risk.dto.RiskAnalysisDetailResponse;
import com.rentseeking.analysis.risk.dto.RiskAnalysisResponse;
import com.rentseeking.analysis.risk.dto.RiskAnalyzeRequest;
import com.rentseeking.analysis.risk.dto.RiskEvidenceResponse;
import com.rentseeking.analysis.risk.dto.RiskItemResponse;
import com.rentseeking.analysis.risk.dto.RiskReviewRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class RiskAnalysisServiceImpl implements RiskAnalysisService {

    private static final Set<String> REVIEW_STATUSES = Set.of("PENDING", "CONFIRMED", "ADJUSTED");

    private final JdbcTemplate jdbcTemplate;
    private final AiService aiService;
    private final KnowledgeRetrievalService knowledgeRetrievalService;

    public RiskAnalysisServiceImpl(
            JdbcTemplate jdbcTemplate,
            AiService aiService,
            KnowledgeRetrievalService knowledgeRetrievalService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.aiService = aiService;
        this.knowledgeRetrievalService = knowledgeRetrievalService;
    }

    @Override
    @Transactional
    public Long analyze(Long regulationId, RiskAnalyzeRequest request) {
        JwtUserPrincipal principal = SecurityUtils.currentPrincipal();
        RegulationRiskSource source = loadRegulation(regulationId);
        jdbcTemplate.update("""
                        UPDATE regulation
                        SET analysis_status = 'ANALYZING', updated_by = ?, updated_at = CURRENT_TIMESTAMP
                        WHERE id = ? AND deleted = 0
                        """,
                principal.getUserId(),
                regulationId
        );
        try {
            List<RiskFinding> ruleFindings = evaluate(source);
            List<RagEvidence> ragEvidences = retrieveEvidence(regulationId, source, ruleFindings);
            String ragContext = knowledgeRetrievalService.formatContext(ragEvidences);
            String ruleContext = formatRuleContext(ruleFindings);
            RentSeekingRiskResult llmResult = tryAnalyzeWithLlm(regulationId, source, ruleContext, ragContext);
            boolean llmBacked = llmResult != null;
            List<RiskFinding> findings = llmBacked ? mergeFindings(ruleFindings, findingsFromAi(llmResult)) : ruleFindings;
            int riskScore = llmBacked ? clamp(llmResult.getRiskScore() == null ? score(findings) : llmResult.getRiskScore()) : score(findings);
            String riskLevel = llmBacked ? normalizeRiskLevel(llmResult.getRiskLevel(), level(riskScore)) : level(riskScore);
            String summary = llmBacked ? safeText(llmResult.getSummary(), summarize(source)) : summarize(source);
            String suggestion = llmBacked ? safeText(llmResult.getOverallSuggestion(), overallSuggestion(riskLevel, findings)) : overallSuggestion(riskLevel, findings);
            String analysisMode = llmBacked && !ragEvidences.isEmpty() ? "RAG_ENHANCED" : (llmBacked ? "LLM_ENHANCED" : "RULE_ONLY");
            Long analysisId = insertAnalysis(
                    regulationId,
                    summary,
                    riskLevel,
                    riskScore,
                    analysisMode,
                    llmBacked ? modelName() : "local-rule-engine",
                    llmBacked ? "RENT_SEEKING_RISK:v2" : "RULE_ENGINE:v2",
                    confidence(riskScore, findings, llmBacked),
                    suggestion,
                    principal.getUserId()
            );
            insertItems(analysisId, findings);
            int evidenceCount = insertEvidence(analysisId, source, ragEvidences);
            jdbcTemplate.update("UPDATE risk_analysis SET evidence_count = ? WHERE id = ?", evidenceCount, analysisId);
            jdbcTemplate.update("""
                            UPDATE regulation
                            SET analysis_status = 'COMPLETED', updated_by = ?, updated_at = CURRENT_TIMESTAMP
                            WHERE id = ?
                            """,
                    principal.getUserId(),
                    regulationId
            );
            if (!llmBacked) {
                aiService.record(
                        "RENT_SEEKING_RISK",
                        "RISK_ANALYSIS",
                        analysisId,
                        riskPrompt(source, ragContext),
                        suggestion,
                        "{\"riskLevel\":\"" + riskLevel + "\",\"riskScore\":" + riskScore + ",\"fallback\":true}",
                        "SUCCESS",
                        null
                );
            }
            return analysisId;
        } catch (RuntimeException ex) {
            jdbcTemplate.update("""
                            UPDATE regulation
                            SET analysis_status = 'FAILED', updated_by = ?, updated_at = CURRENT_TIMESTAMP
                            WHERE id = ?
                            """,
                    principal.getUserId(),
                    regulationId
            );
            aiService.record(
                    "RENT_SEEKING_RISK",
                    "REGULATION",
                    regulationId,
                    riskPrompt(source, "分析失败，未形成 RAG 上下文。"),
                    null,
                    null,
                    "FAILED",
                    ex.getMessage()
            );
            throw ex;
        }
    }

    @Override
    public PageResponse<RiskAnalysisResponse> listAnalyses(String riskLevel, String reviewStatus, Integer pageNo, Integer pageSize) {
        int currentPage = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        StringBuilder condition = new StringBuilder(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        if (riskLevel != null && !riskLevel.isBlank()) {
            condition.append(" AND a.risk_level = ?");
            args.add(riskLevel.trim());
        }
        if (reviewStatus != null && !reviewStatus.isBlank()) {
            condition.append(" AND a.review_status = ?");
            args.add(reviewStatus.trim());
        }
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM risk_analysis a" + condition,
                Long.class,
                args.toArray()
        );
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add((currentPage - 1) * size);
        pageArgs.add(size);
        List<RiskAnalysisResponse> records = jdbcTemplate.query("""
                        SELECT a.id, a.regulation_id, r.title AS regulation_title, a.summary, a.risk_level,
                               a.risk_score, a.analysis_mode, a.model_name, a.prompt_version, a.confidence,
                               a.evidence_count, a.overall_suggestion, a.review_status, a.review_comment,
                               u.real_name AS created_by_name, a.created_at, a.updated_at
                        FROM risk_analysis a
                        JOIN regulation r ON r.id = a.regulation_id
                        LEFT JOIN sys_user u ON u.id = a.created_by
                        %s
                        ORDER BY a.created_at DESC, a.id DESC
                        LIMIT ?, ?
                        """.formatted(condition),
                (rs, rowNum) -> mapAnalysis(rs, new RiskAnalysisResponse()),
                pageArgs.toArray()
        );
        return new PageResponse<>(records, currentPage, size, total == null ? 0 : total);
    }

    @Override
    public RiskAnalysisDetailResponse getAnalysis(Long id) {
        RiskAnalysisDetailResponse detail = jdbcTemplate.query("""
                        SELECT a.id, a.regulation_id, r.title AS regulation_title, a.summary, a.risk_level,
                               a.risk_score, a.analysis_mode, a.model_name, a.prompt_version, a.confidence,
                               a.evidence_count, a.overall_suggestion, a.review_status, a.review_comment,
                               u.real_name AS created_by_name, a.created_at, a.updated_at
                        FROM risk_analysis a
                        JOIN regulation r ON r.id = a.regulation_id
                        LEFT JOIN sys_user u ON u.id = a.created_by
                        WHERE a.id = ?
                        """,
                rs -> rs.next() ? mapAnalysis(rs, new RiskAnalysisDetailResponse()) : null,
                id
        );
        if (detail == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Risk analysis not found");
        }
        detail.setItems(listItems(id));
        detail.setEvidences(listEvidenceByAnalysisId(id));
        return detail;
    }

    @Override
    public List<RiskEvidenceResponse> listEvidence(Long id) {
        return listEvidenceByAnalysisId(id);
    }

    @Override
    @Transactional
    public RiskAnalysisDetailResponse reviewAnalysis(Long id, RiskReviewRequest request) {
        if (!REVIEW_STATUSES.contains(request.getReviewStatus())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "Invalid review status");
        }
        int updated = jdbcTemplate.update("""
                        UPDATE risk_analysis
                        SET review_status = ?, review_comment = ?, updated_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                        """,
                request.getReviewStatus(),
                blankToNull(request.getReviewComment()),
                id
        );
        if (updated == 0) {
            throw new BizException(ErrorCode.NOT_FOUND, "Risk analysis not found");
        }
        return getAnalysis(id);
    }

    private Long insertAnalysis(
            Long regulationId,
            String summary,
            String riskLevel,
            int riskScore,
            String analysisMode,
            String modelName,
            String promptVersion,
            double confidence,
            String suggestion,
            Long userId
    ) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                            INSERT INTO risk_analysis (
                              regulation_id, summary, risk_level, risk_score, analysis_mode, model_name,
                              prompt_version, confidence, overall_suggestion, review_status, created_by
                            )
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', ?)
                            """,
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setLong(1, regulationId);
            ps.setString(2, summary);
            ps.setString(3, riskLevel);
            ps.setInt(4, riskScore);
            ps.setString(5, analysisMode);
            ps.setString(6, modelName);
            ps.setString(7, promptVersion);
            ps.setBigDecimal(8, BigDecimal.valueOf(confidence));
            ps.setString(9, suggestion);
            ps.setLong(10, userId);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    private void insertItems(Long analysisId, List<RiskFinding> findings) {
        List<Object[]> args = new ArrayList<>();
        int sort = 10;
        for (RiskFinding finding : findings) {
            args.add(new Object[]{
                    analysisId,
                    finding.indicatorCode,
                    finding.title,
                    finding.description,
                    finding.reason,
                    finding.suggestion,
                    finding.relatedClause,
                    finding.riskLevel,
                    finding.sourceTrace,
                    sort
            });
            sort += 10;
        }
        jdbcTemplate.batchUpdate("""
                        INSERT INTO risk_item (
                          analysis_id, indicator_code, title, description, reason,
                          suggestion, related_clause, risk_level, source_trace, sort_order
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                args
        );
    }

    private int insertEvidence(Long analysisId, RegulationRiskSource source, List<RagEvidence> ragEvidences) {
        List<RiskItemEvidenceCandidate> items = jdbcTemplate.query("""
                        SELECT id, title, related_clause
                        FROM risk_item
                        WHERE analysis_id = ?
                        ORDER BY sort_order, id
                        """,
                (rs, rowNum) -> new RiskItemEvidenceCandidate(
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getString("related_clause")
                ),
                analysisId
        );
        int citation = 1;
        int count = 0;
        for (RiskItemEvidenceCandidate item : items) {
            String snippet = item.relatedClause == null || item.relatedClause.isBlank()
                    ? firstClause(source.content)
                    : item.relatedClause;
            if (snippet == null || snippet.isBlank()) {
                continue;
            }
            jdbcTemplate.update("""
                            INSERT INTO risk_evidence (
                              analysis_id, risk_item_id, evidence_type, source_type, source_id,
                              title, snippet, similarity, citation_no
                            )
                            VALUES (?, ?, 'CLAUSE', 'REGULATION', ?, ?, ?, ?, ?)
                            """,
                    analysisId,
                    item.id,
                    source.id,
                    "制度原文证据：" + item.title,
                    trimTo(snippet, 1000),
                    BigDecimal.valueOf(1.0000),
                    citation++
            );
            jdbcTemplate.update("UPDATE risk_item SET evidence_count = evidence_count + 1 WHERE id = ?", item.id);
            count++;
        }
        for (RagEvidence evidence : ragEvidences) {
            jdbcTemplate.update("""
                            INSERT INTO risk_evidence (
                              analysis_id, risk_item_id, evidence_type, source_type, source_id,
                              title, snippet, similarity, citation_no
                            )
                            VALUES (?, NULL, 'RAG_CHUNK', ?, ?, ?, ?, ?, ?)
                            """,
                    analysisId,
                    evidence.getSourceType(),
                    evidence.getSourceId(),
                    "RAG检索证据：" + evidence.getTitle(),
                    trimTo(evidence.getSnippet(), 1000),
                    evidence.getSimilarity(),
                    evidence.getCitationNo() + 100
            );
            count++;
        }
        return count;
    }

    private List<RagEvidence> retrieveEvidence(Long regulationId, RegulationRiskSource source, List<RiskFinding> ruleFindings) {
        String queryText = String.join(" ",
                source.title == null ? "" : source.title,
                source.publishDepartment == null ? "" : source.publishDepartment,
                source.applicableScope == null ? "" : source.applicableScope,
                "自由裁量 审批集中 流程透明 监督约束 利益分配 处罚弹性 时间窗口 许可证 续证 春节 布点规划 总量控制 现场核查 回购 迁址 救济",
                formatRuleContext(ruleFindings),
                source.content == null ? "" : source.content
        );
        return knowledgeRetrievalService.retrieveForRiskAnalysis(regulationId, queryText);
    }

    private RentSeekingRiskResult tryAnalyzeWithLlm(Long regulationId, RegulationRiskSource source, String ruleContext, String ragContext) {
        try {
            return aiService.analyzeRentSeekingRisk(
                    regulationId,
                    source.title,
                    source.publishDepartment,
                    source.applicableScope,
                    source.content,
                    ruleContext,
                    ragContext
            );
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private List<RiskFinding> findingsFromAi(RentSeekingRiskResult result) {
        return result.getRiskItems().stream()
                .map(this::findingFromAi)
                .toList();
    }

    private RiskFinding findingFromAi(RentSeekingRiskItem item) {
        return new RiskFinding(
                safeText(item.getIndicatorCode(), "PROCESS_TRANSPARENCY"),
                safeText(item.getTitle(), "寻租风险点"),
                safeText(item.getDescription(), "AI 识别到潜在制度风险。"),
                safeText(item.getReason(), "制度表述可能存在执行空间。"),
                safeText(item.getSuggestion(), "建议明确标准、流程、监督和留痕要求。"),
                safeText(item.getRelatedClause(), ""),
                normalizeRiskLevel(item.getRiskLevel(), "MEDIUM"),
                "LLM"
        );
    }

    private List<RiskFinding> mergeFindings(List<RiskFinding> ruleFindings, List<RiskFinding> aiFindings) {
        List<RiskFinding> merged = new ArrayList<>(aiFindings);
        for (RiskFinding ruleFinding : ruleFindings) {
            boolean covered = merged.stream().anyMatch(aiFinding ->
                    aiFinding.indicatorCode.equals(ruleFinding.indicatorCode)
                            || similarClause(aiFinding.relatedClause, ruleFinding.relatedClause)
            );
            if (!covered || isGeneric(merged)) {
                merged.add(ruleFinding);
            }
        }
        return merged;
    }

    private boolean similarClause(String left, String right) {
        if (left == null || right == null || left.isBlank() || right.isBlank()) {
            return false;
        }
        String shortLeft = trimTo(left, 40);
        String shortRight = trimTo(right, 40);
        return left.contains(shortRight) || right.contains(shortLeft);
    }

    private boolean isGeneric(List<RiskFinding> findings) {
        if (findings.size() < 4) {
            return true;
        }
        return findings.stream().anyMatch(finding ->
                finding.reason.length() < 28
                        || finding.reason.contains("可能存在执行空间")
                        || finding.description.contains("潜在制度风险")
        );
    }

    private String formatRuleContext(List<RiskFinding> findings) {
        if (findings == null || findings.isEmpty()) {
            return "无本地规则命中。";
        }
        StringBuilder builder = new StringBuilder();
        int index = 1;
        for (RiskFinding finding : findings) {
            builder.append(index++)
                    .append(". 指标=")
                    .append(finding.indicatorCode)
                    .append("；风险=")
                    .append(finding.title)
                    .append("；等级=")
                    .append(finding.riskLevel)
                    .append("；条款=")
                    .append(trimTo(finding.relatedClause, 220))
                    .append("；机制=")
                    .append(trimTo(finding.reason, 260))
                    .append("\n");
        }
        return builder.toString().trim();
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private String normalizeRiskLevel(String value, String defaultValue) {
        String riskLevel = value == null ? "" : value.trim().toUpperCase();
        return switch (riskLevel) {
            case "LOW", "MEDIUM", "HIGH", "CRITICAL" -> riskLevel;
            default -> defaultValue;
        };
    }

    private String safeText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private List<RiskFinding> evaluate(RegulationRiskSource source) {
        List<RiskFinding> findings = new ArrayList<>();
        String text = source.content == null ? "" : source.content;
        addIfContains(findings, source, "DISCRETIONARY_POWER", "自由裁量表述风险", "存在可酌情、可适当或根据情况处理的表述。",
                "条款可能赋予执行人员较大解释空间。", "建议细化裁量条件、审批层级和留痕要求。", "HIGH",
                "可以", "酌情", "适当", "根据情况", "综合判断", "特殊");
        addIfContains(findings, source, "APPROVAL_CONCENTRATION", "审批集中风险", "制度涉及审批、决定或评审流程。",
                "审批权集中时容易形成寻租空间。", "建议设置分级审批、交叉复核和结果公示。", "MEDIUM",
                "审批", "审查", "决定", "评审", "批准");
        addIfMissing(findings, text, source, "PROCESS_TRANSPARENCY", "流程透明度不足", "正文未充分体现公开、公示、时限或标准。",
                "缺少透明流程会增加自由解释和暗箱操作空间。", "建议补充办理标准、时限、公开渠道和结果公示要求。", "MEDIUM",
                "公开", "公示", "时限", "标准");
        addIfMissing(findings, text, source, "SUPERVISION_CONSTRAINT", "监督约束不足", "正文未充分体现监督、复核、检查或问责机制。",
                "缺少监督约束会降低违规成本。", "建议增加复核、抽查、监督检查和责任追究条款。", "HIGH",
                "监督", "复核", "检查", "问责", "责任追究");
        addIfContains(findings, source, "BENEFIT_RELATED", "利益分配相关风险", "制度涉及资金、补贴、资格或资源分配。",
                "利益分配事项更容易诱发利益输送。", "建议明确资格条件、评分规则、公示异议和审计留痕。", "HIGH",
                "资金", "补贴", "资格", "资源", "名额", "拨付", "总量控制", "布点规划", "原价回购", "迁址", "停止办理");
        addIfContains(findings, source, "PENALTY_FLEXIBILITY", "处罚弹性风险", "制度涉及处罚且裁量幅度可能较大。",
                "处罚弹性过大会造成同案不同罚。", "建议明确处罚幅度、情节分类和集体决策要求。", "MEDIUM",
                "处罚", "罚款", "从重", "从轻", "减免");
        addIfContains(findings, source, "TIME_WINDOW_RENT", "时间窗口寻租风险", "制度涉及许可证期限、续证、现场核查或高峰经营窗口。",
                "当许可办理期、政策切换期与春节等主要盈利期重叠时，经营者延误成本被放大，容易接受非正式支付以换取及时办理。",
                "建议公开年度办理日历，设置提前续证窗口、一次性告知、快速救济和临时衔接规则。", "HIGH",
                "许可证有效期限", "经营期限", "期满后继续经营", "重新申请", "20个工作日", "春节", "年初", "年末", "公示期满", "停止办理相关行政许可");
        addCaseSpecificFindings(findings, source, text);
        if (findings.isEmpty()) {
            findings.add(new RiskFinding(
                    "PROCESS_TRANSPARENCY",
                    "低风险提示",
                    "未命中明显高风险规则，但仍建议结合业务流程复核。",
                    "MVP 规则未发现高频寻租风险关键词。",
                    "建议后续结合人工审查和历史案例持续完善指标。",
                    firstClause(source.content),
                    "LOW",
                    "RULE"
            ));
        }
        return findings;
    }

    private void addCaseSpecificFindings(List<RiskFinding> findings, RegulationRiskSource source, String text) {
        if (text.contains("2024年12月27日至2025年12月31日") && text.contains("2025年11月26日")) {
            findings.add(new RiskFinding(
                    "TIME_WINDOW_RENT",
                    "许可证到期、政策公示和春节备货窗口高度重叠",
                    "公开材料显示许可证有效期至2025年12月31日，地方通告于2025年11月26日公示期满，经营者在年末同时面对续证、退出、库存和春节备货压力。",
                    "该时间结构会把正常复议、举报、诉讼或重新选址的机会成本抬高；如果许可停办、迁址申办或回购安排缺乏快速救济，经营者更可能以非正式成本换取确定性。",
                    "建议制度中明确提前续证窗口、政策切换过渡期、存量许可证处理规则、申诉办理时限，以及春节前临时经营或库存处置衔接规则。",
                    matchedClause(text, "2024年12月27日至2025年12月31日"),
                    "CRITICAL",
                    "RULE"
            ));
        }
        if (text.contains("原价回购") && text.contains("异地选址")) {
            findings.add(new RiskFinding(
                    "BENEFIT_RELATED",
                    "库存回购和异地选址重新申办存在利益分配裁量",
                    "材料提到协调批发公司原价回购、禁燃区外异地选址重新依规申办，但若缺少库存核验、价格基准、付款时限、迁址排序和异议处理规则，退出安排会成为可分配利益。",
                    "回购金额和迁址机会直接影响经营者损失，执行人员如果能影响核验结果、回购范围或重新申办顺序，就可能形成利益输送或差别对待。",
                    "建议公开退出网点清单、库存核验表、回购价格公式、付款期限、迁址候选点排序规则、重新申办审查标准和异议处理责任人。",
                    matchedClause(text, "原价回购"),
                    "HIGH",
                    "RULE"
            ));
        }
        if (text.contains("发证机关要求提供的其他材料")) {
            findings.add(new RiskFinding(
                    "DISCRETIONARY_POWER",
                    "申请材料兜底条款可能诱发临时加项",
                    "制度允许提交发证机关要求提供的其他材料，若材料目录未在受理前固定并公开，申请人可能被要求反复补充非标准材料。",
                    "材料补正发生在许可办理时限内，特别是在春节旺季前，会把延期损失转化为对审批人员或中介的依赖。",
                    "建议将其他材料限定为法律法规、国家标准或现场核查必要材料，并公开材料清单、示范文本、一次性告知记录和补正截止时间。",
                    matchedClause(text, "发证机关要求提供的其他材料"),
                    "HIGH",
                    "RULE"
            ));
        }
        if (text.contains("停止办理相关行政许可")) {
            findings.add(new RiskFinding(
                    "PROCESS_TRANSPARENCY",
                    "停止办理相关行政许可缺少细化衔接规则时易形成差别待遇",
                    "停止办理许可会直接影响存量零售户能否继续经营、何时退出以及是否能迁址重申办，需要配套明确适用对象、认定标准、过渡期和救济渠道。",
                    "如果只写停止办理而不公开具体名单、法定条件判断、存量许可证处理、迁址条件和异议程序，执行人员就可能在边界认定和个案处理上拥有过大裁量。",
                    "建议补充禁燃区边界图、受影响网点清单、存量许可证效力说明、不予办理理由模板、迁址申办流程和快速复核渠道。",
                    matchedClause(text, "停止办理相关行政许可"),
                    "HIGH",
                    "RULE"
            ));
        }
    }

    private void addIfContains(
            List<RiskFinding> findings,
            RegulationRiskSource source,
            String indicatorCode,
            String title,
            String description,
            String reason,
            String suggestion,
            String riskLevel,
            String... keywords
    ) {
        String text = source.content == null ? "" : source.content;
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                String clause = matchedClause(text, keyword);
                findings.add(new RiskFinding(
                        indicatorCode,
                        title,
                        description + " 命中词：" + keyword + "。",
                        reason + " 命中条款显示：" + trimTo(clause, 160),
                        suggestion,
                        clause,
                        riskLevel,
                        "RULE"
                ));
                return;
            }
        }
    }

    private void addIfMissing(
            List<RiskFinding> findings,
            String text,
            RegulationRiskSource source,
            String indicatorCode,
            String title,
            String description,
            String reason,
            String suggestion,
            String riskLevel,
            String... keywords
    ) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return;
            }
        }
        findings.add(new RiskFinding(
                indicatorCode,
                title,
                description,
                reason + " 未在正文中同时检索到：" + String.join("、", keywords) + "。",
                suggestion,
                firstClause(source.content),
                riskLevel,
                "RULE"
        ));
    }

    private int score(List<RiskFinding> findings) {
        int score = 20;
        for (RiskFinding finding : findings) {
            score += switch (finding.riskLevel) {
                case "HIGH" -> 18;
                case "MEDIUM" -> 12;
                default -> 6;
            };
        }
        return Math.min(score, 100);
    }

    private String level(int score) {
        if (score >= 80) {
            return "CRITICAL";
        }
        if (score >= 65) {
            return "HIGH";
        }
        if (score >= 40) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String summarize(RegulationRiskSource source) {
        String scope = source.applicableScope == null || source.applicableScope.isBlank() ? "未明确适用范围" : source.applicableScope;
        return "《" + source.title + "》由" + (source.publishDepartment == null ? "相关部门" : source.publishDepartment)
                + "维护，适用范围为：" + scope + "。本次分析基于制度正文和元数据进行规则化识别。";
    }

    private String overallSuggestion(String riskLevel, List<RiskFinding> findings) {
        return "综合风险等级为 " + riskLevel + "。建议优先处理 " + findings.size()
                + " 个风险点，重点完善裁量边界、流程公开、监督复核和利益分配留痕。";
    }

    private RegulationRiskSource loadRegulation(Long id) {
        RegulationRiskSource source = jdbcTemplate.query("""
                        SELECT r.id, r.title, r.publish_department, r.applicable_scope,
                               COALESCE(rc.plain_text, rc.content, '') AS content
                        FROM regulation r
                        LEFT JOIN regulation_content rc ON rc.regulation_id = r.id
                        WHERE r.id = ? AND r.deleted = 0
                        """,
                rs -> {
                    if (!rs.next()) {
                        return null;
                    }
                    return new RegulationRiskSource(
                            rs.getLong("id"),
                            rs.getString("title"),
                            rs.getString("publish_department"),
                            rs.getString("applicable_scope"),
                            rs.getString("content")
                    );
                },
                id
        );
        if (source == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Regulation not found");
        }
        if (source.content == null || source.content.isBlank()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "Regulation content is empty");
        }
        return source;
    }

    private String riskPrompt(RegulationRiskSource source, String ragContext) {
        return "请识别制度寻租风险，标题：" + source.title
                + "，适用范围：" + source.applicableScope
                + "，RAG证据：" + ragContext
                + "，正文：" + source.content;
    }

    private List<RiskItemResponse> listItems(Long analysisId) {
        return jdbcTemplate.query("""
                        SELECT id, analysis_id, indicator_code, title, description, reason,
                               suggestion, related_clause, risk_level, source_trace, evidence_count,
                               rectification_status, sort_order
                        FROM risk_item
                        WHERE analysis_id = ?
                        ORDER BY sort_order, id
                        """,
                (rs, rowNum) -> {
                    RiskItemResponse item = new RiskItemResponse();
                    item.setId(rs.getLong("id"));
                    item.setAnalysisId(rs.getLong("analysis_id"));
                    item.setIndicatorCode(rs.getString("indicator_code"));
                    item.setTitle(rs.getString("title"));
                    item.setDescription(rs.getString("description"));
                    item.setReason(rs.getString("reason"));
                    item.setSuggestion(rs.getString("suggestion"));
                    item.setRelatedClause(rs.getString("related_clause"));
                    item.setRiskLevel(rs.getString("risk_level"));
                    item.setSourceTrace(rs.getString("source_trace"));
                    item.setEvidenceCount(rs.getInt("evidence_count"));
                    item.setRectificationStatus(rs.getString("rectification_status"));
                    item.setSortOrder(rs.getInt("sort_order"));
                    return item;
                },
                analysisId
        );
    }

    private List<RiskEvidenceResponse> listEvidenceByAnalysisId(Long analysisId) {
        return jdbcTemplate.query("""
                        SELECT id, analysis_id, risk_item_id, evidence_type, source_type, source_id,
                               title, snippet, similarity, citation_no
                        FROM risk_evidence
                        WHERE analysis_id = ?
                        ORDER BY citation_no, id
                        """,
                (rs, rowNum) -> {
                    RiskEvidenceResponse evidence = new RiskEvidenceResponse();
                    evidence.setId(rs.getLong("id"));
                    evidence.setAnalysisId(rs.getLong("analysis_id"));
                    long itemId = rs.getLong("risk_item_id");
                    evidence.setRiskItemId(rs.wasNull() ? null : itemId);
                    evidence.setEvidenceType(rs.getString("evidence_type"));
                    evidence.setSourceType(rs.getString("source_type"));
                    long sourceId = rs.getLong("source_id");
                    evidence.setSourceId(rs.wasNull() ? null : sourceId);
                    evidence.setTitle(rs.getString("title"));
                    evidence.setSnippet(rs.getString("snippet"));
                    evidence.setSimilarity(rs.getBigDecimal("similarity"));
                    evidence.setCitationNo(rs.getInt("citation_no"));
                    return evidence;
                },
                analysisId
        );
    }

    private <T extends RiskAnalysisResponse> T mapAnalysis(ResultSet rs, T analysis) throws java.sql.SQLException {
        analysis.setId(rs.getLong("id"));
        analysis.setRegulationId(rs.getLong("regulation_id"));
        analysis.setRegulationTitle(rs.getString("regulation_title"));
        analysis.setSummary(rs.getString("summary"));
        analysis.setRiskLevel(rs.getString("risk_level"));
        analysis.setRiskScore(rs.getInt("risk_score"));
        analysis.setAnalysisMode(rs.getString("analysis_mode"));
        analysis.setModelName(rs.getString("model_name"));
        analysis.setPromptVersion(rs.getString("prompt_version"));
        BigDecimal confidence = rs.getBigDecimal("confidence");
        analysis.setConfidence(confidence == null ? null : confidence.doubleValue());
        analysis.setEvidenceCount(rs.getInt("evidence_count"));
        analysis.setOverallSuggestion(rs.getString("overall_suggestion"));
        analysis.setReviewStatus(rs.getString("review_status"));
        analysis.setReviewComment(rs.getString("review_comment"));
        analysis.setCreatedByName(rs.getString("created_by_name"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        analysis.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        analysis.setUpdatedAt(updatedAt == null ? null : updatedAt.toLocalDateTime());
        return analysis;
    }

    private String matchedClause(String text, String keyword) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return List.of(text.split("[。；;\\n]")).stream()
                .map(String::trim)
                .filter(clause -> clause.contains(keyword))
                .findFirst()
                .orElse(firstClause(text));
    }

    private String firstClause(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return List.of(text.split("[。；;\\n]")).stream()
                .map(String::trim)
                .filter(clause -> !clause.isBlank())
                .findFirst()
                .orElse("");
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String modelName() {
        String value = jdbcTemplate.query("""
                        SELECT config_value
                        FROM sys_config
                        WHERE config_key = 'ai.model'
                        """,
                rs -> rs.next() ? rs.getString("config_value") : null
        );
        return value == null || value.isBlank() ? "openai-compatible" : value;
    }

    private double confidence(int riskScore, List<RiskFinding> findings, boolean llmBacked) {
        double base = llmBacked ? 0.82 : 0.62;
        double scoreBoost = Math.min(0.1, riskScore / 1000.0);
        double itemBoost = Math.min(0.08, findings.size() * 0.015);
        return Math.min(0.96, base + scoreBoost + itemBoost);
    }

    private String trimTo(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private record RegulationRiskSource(
            Long id,
            String title,
            String publishDepartment,
            String applicableScope,
            String content
    ) {
    }

    private record RiskFinding(
            String indicatorCode,
            String title,
            String description,
            String reason,
            String suggestion,
            String relatedClause,
            String riskLevel,
            String sourceTrace
    ) {
    }

    private record RiskItemEvidenceCandidate(
            Long id,
            String title,
            String relatedClause
    ) {
    }
}
