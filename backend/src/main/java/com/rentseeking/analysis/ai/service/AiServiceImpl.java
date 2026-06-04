package com.rentseeking.analysis.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentseeking.analysis.ai.dto.AiRecordResponse;
import com.rentseeking.analysis.ai.dto.CaseAnalysisRequest;
import com.rentseeking.analysis.ai.dto.CaseAnalysisResponse;
import com.rentseeking.analysis.ai.dto.RegulationQaRequest;
import com.rentseeking.analysis.ai.dto.RegulationQaResponse;
import com.rentseeking.analysis.ai.dto.RentSeekingRiskItem;
import com.rentseeking.analysis.ai.dto.RentSeekingRiskResult;
import com.rentseeking.analysis.auth.security.JwtUserPrincipal;
import com.rentseeking.analysis.auth.util.SecurityUtils;
import com.rentseeking.analysis.common.config.AiProperties;
import com.rentseeking.analysis.common.constant.ErrorCode;
import com.rentseeking.analysis.common.exception.BizException;
import com.rentseeking.analysis.common.response.PageResponse;
import com.rentseeking.analysis.rag.dto.RagEvidence;
import com.rentseeking.analysis.rag.service.KnowledgeRetrievalService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

@Service
public class AiServiceImpl implements AiService {

    private final JdbcTemplate jdbcTemplate;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final KnowledgeRetrievalService knowledgeRetrievalService;

    public AiServiceImpl(
            JdbcTemplate jdbcTemplate,
            AiProperties aiProperties,
            ObjectMapper objectMapper,
            KnowledgeRetrievalService knowledgeRetrievalService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
        this.knowledgeRetrievalService = knowledgeRetrievalService;
    }

    @Override
    public PageResponse<AiRecordResponse> listRecords(
            String sceneCode,
            String businessType,
            String status,
            Integer pageNo,
            Integer pageSize
    ) {
        int currentPage = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        StringBuilder condition = new StringBuilder(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        if (sceneCode != null && !sceneCode.isBlank()) {
            condition.append(" AND a.scene_code = ?");
            args.add(sceneCode.trim());
        }
        if (businessType != null && !businessType.isBlank()) {
            condition.append(" AND a.business_type = ?");
            args.add(businessType.trim());
        }
        if (status != null && !status.isBlank()) {
            condition.append(" AND a.status = ?");
            args.add(status.trim());
        }
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ai_analysis_record a" + condition,
                Long.class,
                args.toArray()
        );
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add((currentPage - 1) * size);
        pageArgs.add(size);
        List<AiRecordResponse> records = jdbcTemplate.query("""
                        SELECT a.id, a.scene_code, a.business_type, a.business_id, a.model_name,
                               a.prompt, a.response_text, a.parsed_json, a.status, a.error_message,
                               u.real_name AS created_by_name, a.created_at
                        FROM ai_analysis_record a
                        LEFT JOIN sys_user u ON u.id = a.created_by
                        %s
                        ORDER BY a.created_at DESC, a.id DESC
                        LIMIT ?, ?
                        """.formatted(condition),
                (rs, rowNum) -> mapRecord(rs),
                pageArgs.toArray()
        );
        return new PageResponse<>(records, currentPage, size, total == null ? 0 : total);
    }

    @Override
    public RegulationQaResponse answerRegulationQuestion(Long regulationId, RegulationQaRequest request) {
        RegulationAiSource source = loadRegulation(regulationId);
        String template = loadTemplate("REGULATION_QA");
        String prompt = template + "\n标题：" + source.title + "\n正文：" + source.content + "\n问题：" + request.getQuestion();
        String answer = localQaAnswer(source, request.getQuestion());
        Long recordId = record(
                "REGULATION_QA",
                "REGULATION",
                regulationId,
                prompt,
                answer,
                "{\"fallback\":true}",
                "SUCCESS",
                null
        );
        RegulationQaResponse response = new RegulationQaResponse();
        response.setRegulationId(regulationId);
        response.setQuestion(request.getQuestion());
        response.setAnswer(answer);
        response.setRecordId(recordId);
        response.setFallback(true);
        return response;
    }

    @Override
    public CaseAnalysisResponse analyzeCase(CaseAnalysisRequest request) {
        String caseTitle = request.getCaseTitle().trim();
        String caseDescription = request.getCaseDescription().trim();
        String analysisType = request.getAnalysisType() == null ? "CASE_ANALYSIS" : request.getAnalysisType().trim().toUpperCase(Locale.ROOT);
        boolean regulationIteration = "REGULATION_ITERATION".equals(analysisType);
        boolean jointAnalysis = request.getJointAnalysis() == null || request.getJointAnalysis();
        boolean externalDiscovery = request.getExternalDiscovery() == null || request.getExternalDiscovery();
        RegulationSetBrief primarySet = regulationIteration && request.getRegulationSetId() == null ? null : resolveRegulationSet(request.getRegulationSetId());
        RegulationSetBrief externalSet = regulationIteration && request.getExternalRegulationSetId() != null
                ? resolveRegulationSet(request.getExternalRegulationSetId())
                : null;
        List<CaseAnalysisResponse.FetchedSource> fetchedSources = fetchSources(request.getSourceUrls());
        String selectedRegulationContext = loadSelectedRegulationContext(request.getRelatedRegulationIds(), primarySet == null ? null : primarySet.id());
        String primarySetOverview = jointAnalysis ? loadRegulationSetOverview(primarySet == null ? null : primarySet.id()) : "";
        String caseQuery = caseTitle + "\n" + caseDescription;
        List<RagEvidence> primaryEvidences = knowledgeRetrievalService.retrieveForCaseAnalysis(caseQuery, primarySet == null ? null : primarySet.id());
        String primaryRagContext = knowledgeRetrievalService.formatContext(primaryEvidences);
        String externalQuery = externalConstraintQuery(caseTitle, caseDescription, selectedRegulationContext, primarySetOverview);
        List<RagEvidence> externalEvidences = jointAnalysis && externalDiscovery
                ? (externalSet == null
                ? knowledgeRetrievalService.retrieveForCaseAnalysisOutsideSet(externalQuery, primarySet == null ? null : primarySet.id())
                : knowledgeRetrievalService.retrieveForCaseAnalysis(externalQuery, externalSet.id()))
                : List.of();
        String externalRagContext = knowledgeRetrievalService.formatContext(externalEvidences);
        String prompt = regulationIteration
                ? regulationIterationPrompt(caseTitle, caseDescription, primarySet, externalSet, selectedRegulationContext, primarySetOverview, fetchedSources, primaryRagContext, externalRagContext, externalDiscovery)
                : jointAnalysis
                ? jointCaseAnalysisPrompt(caseTitle, caseDescription, primarySet, selectedRegulationContext, primarySetOverview, fetchedSources, primaryRagContext, externalRagContext)
                : caseAnalysisPrompt(caseTitle, caseDescription, selectedRegulationContext, fetchedSources, primaryRagContext);
        CaseAnalysisResponse fallback = regulationIteration
                ? fallbackRegulationIteration(caseTitle, caseDescription, primarySet, primaryEvidences, externalEvidences, externalDiscovery)
                : jointAnalysis
                ? fallbackJointCaseAnalysis(caseTitle, caseDescription, primarySet, primaryEvidences, externalEvidences)
                : fallbackCaseAnalysis(caseTitle, caseDescription);
        CaseAnalysisResponse response;
        String responseText = null;
        boolean llmBacked = false;
        Long recordId = null;
        if (isLlmConfigured()) {
            try {
                responseText = complete(prompt);
                response = parseCaseAnalysis(responseText);
                llmBacked = true;
                mergeMissingCaseFields(response, fallback);
                if (jointAnalysis) {
                    mergeMissingJointFields(response, fallback);
                }
                if (regulationIteration) {
                    mergeMissingIterationFields(response, fallback);
                }
                recordId = record(
                        "CASE_RENT_SEEKING_ANALYSIS",
                        "CASE",
                        null,
                        prompt,
                        responseText,
                        objectMapper.writeValueAsString(response),
                        "SUCCESS",
                        null
                );
            } catch (BizException ex) {
                record("CASE_RENT_SEEKING_ANALYSIS", "CASE", null, prompt, responseText, null, "FAILED", ex.getMessage());
                response = fallback;
            } catch (Exception ex) {
                record("CASE_RENT_SEEKING_ANALYSIS", "CASE", null, prompt, responseText, null, "FAILED", ex.getMessage());
                response = fallback;
            }
        } else {
            response = fallback;
        }
        response.setCaseTitle(caseTitle);
        response.setJointAnalysis(jointAnalysis);
        response.setPrimaryRegulationSetId(primarySet == null ? null : primarySet.id());
        response.setPrimaryRegulationSetName(primarySet == null ? "" : primarySet.name());
        boolean hasRagEvidence = !primaryEvidences.isEmpty() || !externalEvidences.isEmpty();
        response.setAnalysisMode(regulationIteration
                ? (llmBacked && hasRagEvidence ? "ITERATION_RAG_ENHANCED" : (llmBacked ? "ITERATION_LLM_ENHANCED" : "ITERATION_RULE_ONLY"))
                : jointAnalysis
                ? (llmBacked && hasRagEvidence ? "JOINT_RAG_ENHANCED" : (llmBacked ? "JOINT_LLM_ENHANCED" : "JOINT_RULE_ONLY"))
                : (llmBacked && hasRagEvidence ? "RAG_ENHANCED" : (llmBacked ? "LLM_ENHANCED" : "RULE_ONLY")));
        response.setFallback(!llmBacked);
        response.setRecordId(recordId);
        response.setFetchedSources(fetchedSources);
        response.setEvidenceSnippets(jointAnalysis
                ? evidenceSnippets(primaryEvidences, externalEvidences)
                : evidenceSnippets(primaryEvidences));
        if (!llmBacked) {
            try {
                recordId = record(
                        "CASE_RENT_SEEKING_ANALYSIS",
                        "CASE",
                        null,
                        prompt,
                        response.getSummary(),
                        objectMapper.writeValueAsString(response),
                        "SUCCESS",
                        null
                );
                response.setRecordId(recordId);
            } catch (Exception ignored) {
                // The analysis response should still be returned if audit serialization fails.
            }
        }
        return response;
    }

    @Override
    public String explainConflict(Long businessId, String conflictType, String mainClause, String compareClause) {
        String template = loadTemplate("CONFLICT_EXPLANATION");
        String prompt = template + "\n冲突类型：" + conflictType + "\n主制度：" + mainClause + "\n对比制度：" + compareClause;
        String response = switch (conflictType) {
            case "DUPLICATE" -> "两段条款文本高度相似，可能存在重复规定，建议合并表述或明确适用边界。";
            case "SCOPE_CONFLICT" -> "两份制度的适用范围表述不一致，执行时可能导致适用对象或业务边界判断差异。";
            case "SUBJECT_CONFLICT" -> "两份制度涉及的发布或责任主体不同，建议核实权责归属和审批链条。";
            default -> "两段条款存在潜在不一致，建议结合上位制度和实际业务流程进行人工复核。";
        };
        record(
                "CONFLICT_EXPLANATION",
                "CONFLICT_ITEM",
                businessId,
                prompt,
                response,
                "{\"fallback\":true}",
                "SUCCESS",
                null
        );
        return response;
    }

    @Override
    public RentSeekingRiskResult analyzeRentSeekingRisk(
            Long regulationId,
            String title,
            String publishDepartment,
            String applicableScope,
            String content,
            String ruleContext,
            String ragContext
    ) {
        String prompt = riskAnalysisPrompt(title, publishDepartment, applicableScope, content, ruleContext, ragContext);
        if (!isLlmConfigured()) {
            throw new BizException(ErrorCode.AI_SERVICE_ERROR, "LLM service is not configured");
        }
        try {
            String responseText = complete(prompt);
            RentSeekingRiskResult result = parseRiskResult(responseText);
            String parsedJson = objectMapper.writeValueAsString(result);
            record(
                    "RENT_SEEKING_RISK",
                    "REGULATION",
                    regulationId,
                    prompt,
                    responseText,
                    parsedJson,
                    "SUCCESS",
                    null
            );
            return result;
        } catch (BizException ex) {
            record("RENT_SEEKING_RISK", "REGULATION", regulationId, prompt, null, null, "FAILED", ex.getMessage());
            throw ex;
        } catch (RuntimeException ex) {
            record("RENT_SEEKING_RISK", "REGULATION", regulationId, prompt, null, null, "FAILED", ex.getMessage());
            throw new BizException(ErrorCode.AI_SERVICE_ERROR, "LLM service call failed");
        } catch (Exception ex) {
            record("RENT_SEEKING_RISK", "REGULATION", regulationId, prompt, null, null, "FAILED", ex.getMessage());
            throw new BizException(ErrorCode.AI_SERVICE_ERROR, "LLM response parse failed");
        }
    }

    @Override
    public Long record(
            String sceneCode,
            String businessType,
            Long businessId,
            String prompt,
            String responseText,
            String parsedJson,
            String status,
            String errorMessage
    ) {
        Long userId = null;
        try {
            JwtUserPrincipal principal = SecurityUtils.currentPrincipal();
            userId = principal.getUserId();
        } catch (BizException ignored) {
            // System-triggered fallback records may not have a user context.
        }
        String modelName = configValue("ai.model", aiProperties.getModel());
        if (modelName == null || modelName.isBlank()) {
            modelName = "local-fallback";
        }
        KeyHolder keyHolder = new GeneratedKeyHolder();
        Long createdBy = userId;
        String finalModelName = modelName;
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                            INSERT INTO ai_analysis_record (
                              scene_code, business_type, business_id, model_name, prompt, response_text,
                              parsed_json, status, error_message, created_by
                            )
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, sceneCode);
            ps.setString(2, businessType);
            if (businessId == null) {
                ps.setObject(3, null);
            } else {
                ps.setLong(3, businessId);
            }
            ps.setString(4, finalModelName);
            ps.setString(5, prompt);
            ps.setString(6, responseText);
            ps.setString(7, parsedJson);
            ps.setString(8, status);
            ps.setString(9, errorMessage);
            if (createdBy == null) {
                ps.setObject(10, null);
            } else {
                ps.setLong(10, createdBy);
            }
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    private RegulationSetBrief resolveRegulationSet(Long regulationSetId) {
        if (regulationSetId != null) {
            return jdbcTemplate.query("""
                            SELECT id, set_name
                            FROM regulation_set
                            WHERE id = ? AND enabled = 1 AND deleted = 0
                            """,
                    rs -> rs.next() ? new RegulationSetBrief(rs.getLong("id"), rs.getString("set_name")) : null,
                    regulationSetId
            );
        }
        return jdbcTemplate.query("""
                        SELECT id, set_name
                        FROM regulation_set
                        WHERE enabled = 1 AND deleted = 0
                        ORDER BY is_default DESC, updated_at DESC, id DESC
                        LIMIT 1
                        """,
                rs -> rs.next() ? new RegulationSetBrief(rs.getLong("id"), rs.getString("set_name")) : null
        );
    }

    private String loadRegulationSetOverview(Long regulationSetId) {
        if (regulationSetId == null) {
            return "未指定主规章集 A。";
        }
        List<RegulationCaseSource> sources = jdbcTemplate.query("""
                        SELECT r.id, r.title, r.publish_department, COALESCE(rc.plain_text, rc.content, '') AS content
                        FROM regulation r
                        LEFT JOIN regulation_content rc ON rc.regulation_id = r.id
                        WHERE r.deleted = 0
                          AND r.status = 'ACTIVE'
                          AND r.regulation_set_id = ?
                        ORDER BY r.code, r.id
                        LIMIT 60
                        """,
                (rs, rowNum) -> new RegulationCaseSource(
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getString("publish_department"),
                        rs.getString("content")
                ),
                regulationSetId
        );
        if (sources.isEmpty()) {
            return "主规章集 A 暂无有效制度。";
        }
        StringBuilder builder = new StringBuilder();
        int no = 1;
        for (RegulationCaseSource source : sources) {
            builder.append("[A")
                    .append(no++)
                    .append("] ")
                    .append(source.title)
                    .append("，发布机关：")
                    .append(nullToEmpty(source.publishDepartment))
                    .append("\n")
                    .append(truncate(source.content, 260))
                    .append("\n\n");
        }
        return builder.toString().trim();
    }

    private String externalConstraintQuery(
            String caseTitle,
            String caseDescription,
            String selectedRegulationContext,
            String primarySetOverview
    ) {
        return """
                %s
                %s

                从主规章集 A 的寻租风险出发，检索 A 之外的上位法、程序约束、资金管理、信息公开、监督问责、组织职责、评审回避、诚信惩戒、审计监察等外部约束制度。
                重点风险词：审批裁量、评审请托、专家回避、预算调剂、经费拨付、验收评价、信息公开、救济时效、监督问责、利益输送、处罚弹性、时间窗口。

                A 内用户选择制度：
                %s

                A 内规章概览：
                %s
                """.formatted(
                caseTitle,
                caseDescription,
                selectedRegulationContext,
                primarySetOverview
        );
    }

    private String loadSelectedRegulationContext(List<Long> relatedRegulationIds, Long regulationSetId) {
        List<Long> ids = relatedRegulationIds == null ? List.of() : relatedRegulationIds.stream()
                .filter(id -> id != null)
                .distinct()
                .limit(20)
                .toList();
        if (ids.isEmpty()) {
            return "未指定制度，系统将仅使用全库 RAG 检索结果与案例事实。";
        }
        String placeholders = String.join(",", ids.stream().map(id -> "?").toList());
        String setCondition = regulationSetId == null ? "" : " AND r.regulation_set_id = ?";
        List<Object> args = new ArrayList<>(ids);
        if (regulationSetId != null) {
            args.add(regulationSetId);
        }
        List<RegulationCaseSource> sources = jdbcTemplate.query("""
                        SELECT r.id, r.title, r.publish_department, COALESCE(rc.plain_text, rc.content, '') AS content
                        FROM regulation r
                        LEFT JOIN regulation_content rc ON rc.regulation_id = r.id
                        WHERE r.deleted = 0 AND r.id IN (%s)
                          %s
                        ORDER BY r.updated_at DESC, r.id DESC
                        """.formatted(placeholders, setCondition),
                (rs, rowNum) -> new RegulationCaseSource(
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getString("publish_department"),
                        rs.getString("content")
                ),
                args.toArray()
        );
        if (sources.isEmpty()) {
            return "指定制度未找到或已删除。";
        }
        StringBuilder builder = new StringBuilder();
        for (RegulationCaseSource source : sources) {
            builder.append("【制度")
                    .append(source.id)
                    .append("】")
                    .append(source.title)
                    .append("，发布机关：")
                    .append(nullToEmpty(source.publishDepartment))
                    .append("\n")
                    .append(truncate(source.content, 500))
                    .append("\n\n");
        }
        return builder.toString().trim();
    }

    private List<CaseAnalysisResponse.FetchedSource> fetchSources(List<String> sourceUrls) {
        List<String> urls = sourceUrls == null ? List.of() : sourceUrls.stream()
                .map(url -> url == null ? "" : url.trim())
                .filter(url -> !url.isBlank())
                .distinct()
                .limit(8)
                .toList();
        if (urls.isEmpty()) {
            return List.of();
        }
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        List<CaseAnalysisResponse.FetchedSource> results = new ArrayList<>();
        for (String url : urls) {
            results.add(fetchSource(client, url));
        }
        return results;
    }

    private CaseAnalysisResponse.FetchedSource fetchSource(HttpClient client, String url) {
        CaseAnalysisResponse.FetchedSource source = new CaseAnalysisResponse.FetchedSource();
        source.setUrl(url);
        try {
            URI uri = URI.create(url);
            if (!Set.of("http", "https").contains(uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT))) {
                source.setStatus("FAILED");
                source.setMessage("仅支持 http/https 官方公开链接");
                return source;
            }
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(12))
                    .header("User-Agent", "rent-seeking-analysis/0.1")
                    .GET()
                    .build();
            HttpResponse<String> response = sendString(client, request);
            int statusCode = response.statusCode();
            if (statusCode < 200 || statusCode >= 300) {
                source.setStatus("FAILED");
                source.setMessage("HTTP " + statusCode);
                return source;
            }
            String contentType = response.headers().firstValue("content-type").orElse("").toLowerCase(Locale.ROOT);
            if (contentType.contains("pdf")) {
                source.setTitle(fileNameOf(uri));
                source.setStatus("NEED_MANUAL_IMPORT");
                source.setMessage("已识别 PDF 链接，当前自动抓取不解析 PDF 正文，请下载后导入正文或粘贴关键条款。");
                return source;
            }
            String body = response.body() == null ? "" : response.body();
            String title = extractHtmlTitle(body);
            String plainText = plainTextOf(body);
            source.setTitle(title.isBlank() ? fileNameOf(uri) : title);
            source.setSnippet(truncate(plainText, 4000));
            source.setStatus(plainText.isBlank() ? "NEED_MANUAL_IMPORT" : "FETCHED");
            source.setMessage(isOfficialSource(uri)
                    ? "已抓取官方公开来源正文片段"
                    : "已抓取公开来源片段，域名需人工核验是否为权威来源");
        } catch (IllegalArgumentException ex) {
            source.setStatus("FAILED");
            source.setMessage("URL 格式无效");
        } catch (Exception ex) {
            source.setStatus("FAILED");
            source.setMessage(truncate(ex.getMessage(), 160));
        }
        return source;
    }

    private String caseAnalysisPrompt(
            String caseTitle,
            String caseDescription,
            String selectedRegulationContext,
            List<CaseAnalysisResponse.FetchedSource> fetchedSources,
            String ragContext
    ) {
        String template = loadTemplate("CASE_RENT_SEEKING_ANALYSIS");
        return """
                %s

                你要把“单项制度问答”升级成“围绕一个真实事件举一反三寻找相关制度的寻租风险分析”。
                请只基于以下材料作答：案例事实、用户选择制度、自动抓取公开来源、RAG 检索证据。
                如果证据不足，不要编造事实；要明确列出需要用户手动导入的法律公文、政策材料、组织架构和职责分工。

                分析要求：
                1. 找出与案例相关的上位法、部门规章、地方规范性文件、标准、通告、事故调查或组织职责材料。
                2. 每个风险点必须写清楚：权力节点、经营者处境、可交换利益、为什么制度安排放大寻租激励、可执行整改动作。
                3. 特别关注时间窗口寻租：许可有效期、续证受理期、旺季盈利期、救济周期之间是否错配。
                4. 特别关注组织链条：制定政策机关、许可机关、现场核查机关、属地街道、公安、市场监管、纪检监察、批发企业等角色是否边界清楚。
                5. relatedDocuments 和 organizationNodes 不得为空；证据不足时用 importStatus 标记“建议导入”。
                6. riskItems 至少 5 条，标题必须具体，不能写“审批风险”“监督风险”这类泛泛表达。
                7. 未有官方调查、监察或司法材料支撑时，不要把具体人员或机关定性为受贿、敲诈、徇私舞弊等违法犯罪，只描述制度激励、寻租空间和待核验事实。

                必须返回合法 JSON，不要 Markdown：
                {
                  "summary": "案例关联制度链摘要",
                  "riskLevel": "LOW|MEDIUM|HIGH|CRITICAL",
                  "riskScore": 0-100,
                  "relatedDocuments": [
                    {
                      "title": "公文或制度名称",
                      "issuingBody": "发布机关",
                      "sourceUrl": "官方来源 URL 或空字符串",
                      "relevance": "与案例和寻租风险的具体关系",
                      "importStatus": "已导入|已抓取|建议导入|需人工核验"
                    }
                  ],
                  "organizationNodes": [
                    {
                      "organization": "组织或岗位",
                      "level": "国家|省级|市级|区县|街道|企业|其他",
                      "role": "在案例制度链中的职责",
                      "riskResponsibility": "可能形成寻租空间的权力或责任边界",
                      "evidenceNeed": "还需导入或核验的职责依据"
                    }
                  ],
                  "riskItems": [
                    {
                      "indicatorCode": "DISCRETIONARY_POWER|APPROVAL_CONCENTRATION|PROCESS_TRANSPARENCY|SUPERVISION_CONSTRAINT|BENEFIT_RELATED|PENALTY_FLEXIBILITY|TIME_WINDOW_RENT",
                      "title": "具体风险点标题",
                      "mechanism": "权力节点 + 经营者处境 + 可交换利益",
                      "reason": "制度或组织安排为何放大寻租激励，并引用证据编号",
                      "evidence": "关联条款、材料或待补证据",
                      "suggestion": "可执行制度修订或流程控制动作",
                      "riskLevel": "LOW|MEDIUM|HIGH|CRITICAL"
                    }
                  ],
                  "manualImportSuggestions": [
                    {
                      "category": "法律公文|地方政策|组织架构|事故调查|行政许可清单|其他",
                      "title": "建议导入名称",
                      "suggestedSource": "建议来源",
                      "reason": "为什么缺它会影响分析"
                    }
                  ]
                }

                案例标题：
                %s

                案例事实：
                %s

                用户选择制度正文：
                %s

                自动抓取公开来源：
                %s

                RAG 检索证据：
                %s
                """.formatted(
                template,
                caseTitle,
                caseDescription,
                selectedRegulationContext,
                fetchedSourcesContext(fetchedSources),
                ragContext
        );
    }

    private String jointCaseAnalysisPrompt(
            String caseTitle,
            String caseDescription,
            RegulationSetBrief primarySet,
            String selectedRegulationContext,
            String primarySetOverview,
            List<CaseAnalysisResponse.FetchedSource> fetchedSources,
            String primaryRagContext,
            String externalRagContext
    ) {
        String template = loadTemplate("CASE_RENT_SEEKING_ANALYSIS");
        String primarySetName = primarySet == null ? "未指定规章集 A" : primarySet.name();
        return """
                %s

                你现在执行“案例-规章集联合寻租风险推理”。
                输入包含两条线：
                A. 案例事实线：从真实案例抽取权力节点、利益节点、时间窗口、机会成本差和异常激励。
                B. 规章集线：以用户指定规章集 A 为主分析对象，先推理 A 内部的所有主要寻租风险，再检索 A 之外能约束这些风险的外部制度 B。

                严格边界：
                1. 规章集 A 是主分析对象，不要把外部制度 B 说成已经物理合并进 A。
                2. 外部制度 B 只能作为“外部约束证据集”，用于判断 A 的风险是否被上位法、程序法、资金规则、监督问责或组织职责部分约束。
                3. 必须输出“案例触发型风险”“制度内生型风险”“制度闭环缺口”三类结论。
                4. 不要无限扩张制度链。最多表达两轮迭代：第一轮 A 内部风险，第二轮 A 外部约束召回；若证据不足，停止并列出需人工导入材料。
                5. 未有官方调查、监察或司法材料支撑时，不要把具体人员或机关定性为受贿、敲诈、徇私舞弊等违法犯罪，只描述制度激励、寻租空间和待核验事实。

                必须返回合法 JSON，不要 Markdown。除原有字段外，还必须包含联合分析字段：
                {
                  "summary": "案例和规章集 A 的综合摘要",
                  "jointSummary": "说明案例事实、A 内部风险、外部约束 B 和仍未闭合风险之间的关系",
                  "riskLevel": "LOW|MEDIUM|HIGH|CRITICAL",
                  "riskScore": 0-100,
                  "relatedDocuments": [
                    {"title": "制度名称", "issuingBody": "发布机关", "sourceUrl": "URL或空", "relevance": "与案例/A/B的具体关系", "importStatus": "已导入|已抓取|建议导入|需人工核验"}
                  ],
                  "organizationNodes": [
                    {"organization": "组织或岗位", "level": "国家|省级|市级|区县|街道|企业|其他", "role": "职责", "riskResponsibility": "风险责任边界", "evidenceNeed": "需补证据"}
                  ],
                  "riskItems": [
                    {"indicatorCode": "DISCRETIONARY_POWER|APPROVAL_CONCENTRATION|PROCESS_TRANSPARENCY|SUPERVISION_CONSTRAINT|BENEFIT_RELATED|PENALTY_FLEXIBILITY|TIME_WINDOW_RENT", "title": "具体风险点", "mechanism": "权力节点+主体处境+可交换利益", "reason": "为什么放大寻租激励并引用证据", "evidence": "关联条款/材料", "suggestion": "可执行整改动作", "riskLevel": "LOW|MEDIUM|HIGH|CRITICAL"}
                  ],
                  "internalRisks": [
                    {"source": "案例触发|规章集A内生|案例-A交叉", "riskType": "风险类型", "riskTitle": "A内部风险标题", "mechanism": "A内部如何形成寻租激励", "relatedRegulations": "A内相关制度", "riskLevel": "LOW|MEDIUM|HIGH|CRITICAL"}
                  ],
                  "caseRegulationMappings": [
                    {"caseFact": "案例事实或时间窗口", "matchedRegulation": "A内匹配制度", "matchedRisk": "对应风险", "gapType": "制度缺失|执行缺失|衔接缺失|证据不足", "explanation": "映射解释"}
                  ],
                  "externalConstraints": [
                    {"sourceSet": "外部规章集或证据来源", "documentTitle": "外部制度B", "constraintType": "上位依据|程序约束|资金规则|监督问责|组织职责|信息公开|其他", "constrainsRisk": "约束A中的哪个风险", "constraintMechanism": "如何约束", "sufficiency": "充分约束|部分约束|仅提供问责|证据不足", "importAdvice": "是否建议正式导入/保持为证据"}
                  ],
                  "closureGaps": [
                    {"gapTitle": "闭环缺口", "missingLink": "A与B之间缺少什么衔接", "remainingRisk": "补强后仍存在的寻租激励", "recommendedAction": "制度衔接或流程改造动作", "priority": "LOW|MEDIUM|HIGH|CRITICAL"}
                  ],
                  "iterationTraces": [
                    {"round": 1, "objective": "A内部风险抽取", "query": "检索或推理输入摘要", "evidenceCount": 0, "stopReason": "继续/停止原因"},
                    {"round": 2, "objective": "外部约束B召回", "query": "外部约束查询", "evidenceCount": 0, "stopReason": "停止条件"}
                  ],
                  "manualImportSuggestions": [
                    {"category": "法律公文|地方政策|组织架构|事故调查|行政许可清单|资金管理|其他", "title": "建议导入名称", "suggestedSource": "建议来源", "reason": "为什么缺它会影响分析"}
                  ]
                }

                案例标题：
                %s

                案例事实：
                %s

                主规章集 A：
                %s

                用户选择的 A 内制度正文：
                %s

                A 内规章概览：
                %s

                自动抓取公开来源：
                %s

                A 内 RAG 证据：
                %s

                A 外部约束证据 B：
                %s
                """.formatted(
                template,
                caseTitle,
                caseDescription,
                primarySetName,
                selectedRegulationContext,
                primarySetOverview,
                fetchedSourcesContext(fetchedSources),
                primaryRagContext,
                externalRagContext
        );
    }

    private String regulationIterationPrompt(
            String taskTitle,
            String taskDescription,
            RegulationSetBrief primarySet,
            RegulationSetBrief externalSet,
            String selectedRegulationContext,
            String primarySetOverview,
            List<CaseAnalysisResponse.FetchedSource> fetchedSources,
            String primaryRagContext,
            String externalRagContext,
            boolean externalDiscovery
    ) {
        String template = loadTemplate("CASE_RENT_SEEKING_ANALYSIS");
        String primarySetName = primarySet == null ? "未指定主规章集，可能由可信法规 URL 提供正文" : primarySet.name();
        String externalSourceName = externalSet == null ? "未指定外部规章集，可能由联网检索或用户 URL 提供" : externalSet.name();
        String roundInstruction = externalDiscovery
                ? "本次执行第二轮：以第一轮内部风险和制约措施为线索，排查外部规章集、联网候选来源或用户确认 URL 中是否存在可匹配条文。"
                : "本次只执行第一轮：分析主规章或可信 URL 的内部寻租风险，并推理制约措施；不要主动完成外部约束匹配。";
        return """
                %s

                你现在执行“规章迭代寻租分析”，不是案例扩展分析。
                %s

                工作边界：
                1. 主规章集或可信法规 URL 是第一轮分析对象，先识别其内部寻租风险。
                2. 每个内部风险都必须推理出可执行的制约措施，并写入 controlMeasures。
                3. 如果 externalDiscovery=false，应在第一轮结果处停止，提示需要用户确认后再进入第二轮。
                4. 如果 externalDiscovery=true，只能围绕第一轮制约措施检索外部规章条文，判断其是否能匹配、补强或仍不足。
                5. 互联网上的公文、法律和规章只能标记为“需人工核验/建议导入”，不要声称已自动导入。

                必须返回合法 JSON，不要 Markdown：
                {
                  "summary": "本轮规章迭代分析摘要",
                  "jointSummary": "第一轮内部风险、制约措施和第二轮外部匹配之间的关系",
                  "riskLevel": "LOW|MEDIUM|HIGH|CRITICAL",
                  "riskScore": 0-100,
                  "relatedDocuments": [
                    {"title": "制度名称", "issuingBody": "发布机关", "sourceUrl": "URL或空", "relevance": "与本轮分析的关系", "importStatus": "已导入|已抓取|建议导入|需人工核验"}
                  ],
                  "organizationNodes": [
                    {"organization": "组织或岗位", "level": "国家|省级|市级|区县|街道|企业|其他", "role": "职责", "riskResponsibility": "风险责任边界", "evidenceNeed": "需补证据"}
                  ],
                  "riskItems": [
                    {"indicatorCode": "DISCRETIONARY_POWER|APPROVAL_CONCENTRATION|PROCESS_TRANSPARENCY|SUPERVISION_CONSTRAINT|BENEFIT_RELATED|PENALTY_FLEXIBILITY|TIME_WINDOW_RENT", "title": "内部风险点", "mechanism": "权力节点+主体处境+可交换利益", "reason": "为什么放大寻租激励并引用证据", "evidence": "关联条款/材料", "suggestion": "制约措施摘要", "riskLevel": "LOW|MEDIUM|HIGH|CRITICAL"}
                  ],
                  "internalRisks": [
                    {"source": "规章内部", "riskType": "风险类型", "riskTitle": "内部风险标题", "mechanism": "内部条款如何形成寻租激励", "relatedRegulations": "主规章或 URL", "riskLevel": "LOW|MEDIUM|HIGH|CRITICAL"}
                  ],
                  "controlMeasures": [
                    {"riskTitle": "对应内部风险", "controlObjective": "制约目标", "measure": "可执行制约措施", "evidenceNeed": "需要外部规章证明什么", "externalSearchHint": "第二轮检索线索", "priority": "LOW|MEDIUM|HIGH|CRITICAL"}
                  ],
                  "externalConstraints": [
                    {"sourceSet": "外部规章集或联网来源", "documentTitle": "外部制度", "constraintType": "上位依据|程序约束|资金规则|监督问责|组织职责|信息公开|其他", "constrainsRisk": "匹配的内部风险或制约措施", "constraintMechanism": "匹配条文如何约束", "sufficiency": "充分约束|部分约束|仅提供问责|证据不足", "importAdvice": "是否建议正式导入/保持为证据"}
                  ],
                  "closureGaps": [
                    {"gapTitle": "仍未闭合缺口", "missingLink": "缺少的制度衔接", "remainingRisk": "剩余寻租激励", "recommendedAction": "下一步制度动作", "priority": "LOW|MEDIUM|HIGH|CRITICAL"}
                  ],
                  "iterationTraces": [
                    {"round": 1, "objective": "内部风险与制约措施", "query": "输入摘要", "evidenceCount": 0, "stopReason": "继续或停止原因"},
                    {"round": 2, "objective": "外部约束条文匹配", "query": "外部检索线索", "evidenceCount": 0, "stopReason": "继续或停止原因"}
                  ],
                  "manualImportSuggestions": [
                    {"category": "法律公文|地方政策|组织架构|行政许可清单|资金管理|其他", "title": "建议导入名称", "suggestedSource": "建议来源", "reason": "为什么缺它会影响分析"}
                  ]
                }

                任务标题：
                %s

                任务说明：
                %s

                主规章对象：
                %s

                外部来源选择：
                %s

                用户选择的主规章正文：
                %s

                主规章集概览：
                %s

                用户确认或联网抓取的公开来源：
                %s

                主规章 RAG 证据：
                %s

                外部规章/约束证据：
                %s
                """.formatted(
                template,
                roundInstruction,
                taskTitle,
                taskDescription,
                primarySetName,
                externalSourceName,
                selectedRegulationContext,
                primarySetOverview,
                fetchedSourcesContext(fetchedSources),
                primaryRagContext,
                externalRagContext
        );
    }

    private String fetchedSourcesContext(List<CaseAnalysisResponse.FetchedSource> fetchedSources) {
        if (fetchedSources == null || fetchedSources.isEmpty()) {
            return "用户未附加已确认公开链接。若本次证据不足，请在 manualImportSuggestions 中列出应导入的官方材料；页面可使用“联网发现”先检索候选来源，再确认导入外部约束规章集。";
        }
        StringBuilder builder = new StringBuilder();
        int no = 1;
        for (CaseAnalysisResponse.FetchedSource source : fetchedSources) {
            builder.append("[公开来源")
                    .append(no++)
                    .append("] ")
                    .append(nullToEmpty(source.getTitle()))
                    .append("\nURL=")
                    .append(source.getUrl())
                    .append("\n状态=")
                    .append(source.getStatus())
                    .append("，说明=")
                    .append(nullToEmpty(source.getMessage()))
                    .append("\n")
                    .append(truncate(source.getSnippet(), 700))
                    .append("\n\n");
        }
        return builder.toString().trim();
    }

    private CaseAnalysisResponse parseCaseAnalysis(String responseText) throws Exception {
        JsonNode root = objectMapper.readTree(extractJsonObject(responseText));
        CaseAnalysisResponse response = new CaseAnalysisResponse();
        response.setSummary(text(root, "summary", "已完成案例关联制度寻租风险分析。"));
        response.setRiskLevel(normalizeRiskLevel(text(root, "riskLevel", "HIGH")));
        response.setRiskScore(clamp(root.path("riskScore").asInt(75), 0, 100));
        response.setRelatedDocuments(parseRelatedDocuments(root.path("relatedDocuments")));
        response.setOrganizationNodes(parseOrganizationNodes(root.path("organizationNodes")));
        response.setRiskItems(parseCaseRiskItems(root.path("riskItems"), response.getRiskLevel()));
        response.setJointSummary(text(root, "jointSummary", ""));
        response.setInternalRisks(parseInternalRisks(root.path("internalRisks")));
        response.setControlMeasures(parseControlMeasures(root.path("controlMeasures")));
        response.setCaseRegulationMappings(parseCaseRegulationMappings(root.path("caseRegulationMappings")));
        response.setExternalConstraints(parseExternalConstraints(root.path("externalConstraints")));
        response.setClosureGaps(parseClosureGaps(root.path("closureGaps")));
        response.setIterationTraces(parseIterationTraces(root.path("iterationTraces")));
        response.setManualImportSuggestions(parseManualImportSuggestions(root.path("manualImportSuggestions")));
        return response;
    }

    private List<CaseAnalysisResponse.RelatedDocument> parseRelatedDocuments(JsonNode nodes) {
        List<CaseAnalysisResponse.RelatedDocument> documents = new ArrayList<>();
        if (nodes.isArray()) {
            for (JsonNode node : nodes) {
                documents.add(document(
                        text(node, "title", "待核验制度材料"),
                        text(node, "issuingBody", ""),
                        text(node, "sourceUrl", ""),
                        text(node, "relevance", "LLM 识别为相关制度材料。"),
                        text(node, "importStatus", "需人工核验")
                ));
            }
        }
        return documents;
    }

    private List<CaseAnalysisResponse.OrganizationNode> parseOrganizationNodes(JsonNode nodes) {
        List<CaseAnalysisResponse.OrganizationNode> organizations = new ArrayList<>();
        if (nodes.isArray()) {
            for (JsonNode node : nodes) {
                organizations.add(organization(
                        text(node, "organization", "待核验组织"),
                        text(node, "level", "其他"),
                        text(node, "role", ""),
                        text(node, "riskResponsibility", ""),
                        text(node, "evidenceNeed", "")
                ));
            }
        }
        return organizations;
    }

    private List<CaseAnalysisResponse.CaseRiskItem> parseCaseRiskItems(JsonNode nodes, String defaultRiskLevel) {
        List<CaseAnalysisResponse.CaseRiskItem> items = new ArrayList<>();
        if (nodes.isArray()) {
            for (JsonNode node : nodes) {
                items.add(caseRisk(
                        text(node, "indicatorCode", "PROCESS_TRANSPARENCY"),
                        text(node, "title", "待核验风险点"),
                        text(node, "mechanism", "需补充权力节点、经营者处境和可交换利益。"),
                        text(node, "reason", "证据不足，需人工补充制度依据。"),
                        text(node, "evidence", "待补充证据"),
                        text(node, "suggestion", "补充制度原文、组织职责和流程留痕后复核。"),
                        normalizeRiskLevel(text(node, "riskLevel", defaultRiskLevel))
                ));
            }
        }
        return items;
    }

    private List<CaseAnalysisResponse.InternalRisk> parseInternalRisks(JsonNode nodes) {
        List<CaseAnalysisResponse.InternalRisk> items = new ArrayList<>();
        if (nodes.isArray()) {
            for (JsonNode node : nodes) {
                items.add(internalRisk(
                        text(node, "source", "规章集 A 内部推理"),
                        text(node, "riskType", "PROCESS_TRANSPARENCY"),
                        text(node, "riskTitle", "待核验内部风险"),
                        text(node, "mechanism", "需补充权力节点、利益节点和制度触发条件。"),
                        text(node, "relatedRegulations", "待补充关联制度"),
                        normalizeRiskLevel(text(node, "riskLevel", "MEDIUM"))
                ));
            }
        }
        return items;
    }

    private List<CaseAnalysisResponse.ControlMeasure> parseControlMeasures(JsonNode nodes) {
        List<CaseAnalysisResponse.ControlMeasure> items = new ArrayList<>();
        if (nodes.isArray()) {
            for (JsonNode node : nodes) {
                items.add(controlMeasure(
                        text(node, "riskTitle", "待匹配风险"),
                        text(node, "controlObjective", "压缩自由裁量和非正式交易空间。"),
                        text(node, "measure", "补充公开、留痕、回避、复核和问责条款。"),
                        text(node, "evidenceNeed", "需要外部制度或条款证据支撑。"),
                        text(node, "externalSearchHint", "上位法 程序约束 信息公开 监督问责"),
                        normalizeRiskLevel(text(node, "priority", "MEDIUM"))
                ));
            }
        }
        return items;
    }

    private List<CaseAnalysisResponse.CaseRegulationMapping> parseCaseRegulationMappings(JsonNode nodes) {
        List<CaseAnalysisResponse.CaseRegulationMapping> items = new ArrayList<>();
        if (nodes.isArray()) {
            for (JsonNode node : nodes) {
                items.add(caseRegulationMapping(
                        text(node, "caseFact", "待核验案例事实"),
                        text(node, "matchedRegulation", "待匹配制度"),
                        text(node, "matchedRisk", "待匹配风险"),
                        text(node, "gapType", "制度缺口"),
                        text(node, "explanation", "需补充事实和制度证据后复核。")
                ));
            }
        }
        return items;
    }

    private List<CaseAnalysisResponse.ExternalConstraint> parseExternalConstraints(JsonNode nodes) {
        List<CaseAnalysisResponse.ExternalConstraint> items = new ArrayList<>();
        if (nodes.isArray()) {
            for (JsonNode node : nodes) {
                items.add(externalConstraint(
                        text(node, "sourceSet", "外部约束证据集 B"),
                        text(node, "documentTitle", "待核验外部制度"),
                        text(node, "constraintType", "监督问责"),
                        text(node, "constrainsRisk", "待匹配风险"),
                        text(node, "constraintMechanism", "需补充外部制度如何约束 A 的风险。"),
                        text(node, "sufficiency", "部分约束"),
                        text(node, "importAdvice", "作为本次分析证据集保留，人工确认后再决定是否导入。")
                ));
            }
        }
        return items;
    }

    private List<CaseAnalysisResponse.ClosureGap> parseClosureGaps(JsonNode nodes) {
        List<CaseAnalysisResponse.ClosureGap> items = new ArrayList<>();
        if (nodes.isArray()) {
            for (JsonNode node : nodes) {
                items.add(closureGap(
                        text(node, "gapTitle", "待核验闭环缺口"),
                        text(node, "missingLink", "缺少制度衔接"),
                        text(node, "remainingRisk", "外部约束不足以完全覆盖内部风险。"),
                        text(node, "recommendedAction", "补充制度衔接条款、流程留痕和责任主体。"),
                        text(node, "priority", "MEDIUM")
                ));
            }
        }
        return items;
    }

    private List<CaseAnalysisResponse.IterationTrace> parseIterationTraces(JsonNode nodes) {
        List<CaseAnalysisResponse.IterationTrace> items = new ArrayList<>();
        if (nodes.isArray()) {
            for (JsonNode node : nodes) {
                items.add(iterationTrace(
                        clamp(node.path("round").asInt(items.size() + 1), 1, 10),
                        text(node, "objective", "迭代分析"),
                        text(node, "query", ""),
                        Math.max(0, node.path("evidenceCount").asInt(0)),
                        text(node, "stopReason", "")
                ));
            }
        }
        return items;
    }

    private List<CaseAnalysisResponse.ManualImportSuggestion> parseManualImportSuggestions(JsonNode nodes) {
        List<CaseAnalysisResponse.ManualImportSuggestion> suggestions = new ArrayList<>();
        if (nodes.isArray()) {
            for (JsonNode node : nodes) {
                suggestions.add(importSuggestion(
                        text(node, "category", "其他"),
                        text(node, "title", "待导入材料"),
                        text(node, "suggestedSource", ""),
                        text(node, "reason", "补强证据链。")
                ));
            }
        }
        return suggestions;
    }

    private void mergeMissingCaseFields(CaseAnalysisResponse target, CaseAnalysisResponse fallback) {
        if (!hasText(target.getSummary())) {
            target.setSummary(fallback.getSummary());
        }
        if (!hasText(target.getRiskLevel())) {
            target.setRiskLevel(fallback.getRiskLevel());
        }
        if (target.getRiskScore() == null) {
            target.setRiskScore(fallback.getRiskScore());
        }
        if (target.getRelatedDocuments().isEmpty()) {
            target.setRelatedDocuments(fallback.getRelatedDocuments());
        }
        if (target.getOrganizationNodes().isEmpty()) {
            target.setOrganizationNodes(fallback.getOrganizationNodes());
        }
        if (target.getRiskItems().isEmpty()) {
            target.setRiskItems(fallback.getRiskItems());
        }
        if (target.getManualImportSuggestions().isEmpty()) {
            target.setManualImportSuggestions(fallback.getManualImportSuggestions());
        }
    }

    private void mergeMissingJointFields(CaseAnalysisResponse target, CaseAnalysisResponse fallback) {
        if (!hasText(target.getJointSummary())) {
            target.setJointSummary(fallback.getJointSummary());
        }
        if (target.getInternalRisks().isEmpty()) {
            target.setInternalRisks(fallback.getInternalRisks());
        }
        if (target.getCaseRegulationMappings().isEmpty()) {
            target.setCaseRegulationMappings(fallback.getCaseRegulationMappings());
        }
        if (target.getExternalConstraints().isEmpty()) {
            target.setExternalConstraints(fallback.getExternalConstraints());
        }
        if (target.getClosureGaps().isEmpty()) {
            target.setClosureGaps(fallback.getClosureGaps());
        }
        if (target.getIterationTraces().isEmpty()) {
            target.setIterationTraces(fallback.getIterationTraces());
        }
    }

    private void mergeMissingIterationFields(CaseAnalysisResponse target, CaseAnalysisResponse fallback) {
        if (target.getControlMeasures().isEmpty()) {
            target.setControlMeasures(fallback.getControlMeasures());
        }
    }

    private CaseAnalysisResponse fallbackCaseAnalysis(String caseTitle, String caseDescription) {
        CaseAnalysisResponse response = new CaseAnalysisResponse();
        response.setCaseTitle(caseTitle);
        boolean fireworks = isFireworksCase(caseTitle + "\n" + caseDescription);
        response.setRiskLevel(fireworks ? "HIGH" : "MEDIUM");
        response.setRiskScore(fireworks ? 82 : 62);
        response.setSummary(fireworks
                ? "该案例涉及烟花爆竹零售许可、禁燃限放政策、存量网点退出、库存回购、异地迁址和事故责任链。核心寻租激励来自旺季前许可窗口、稀缺布点名额、现场核查裁量、回购和迁址安排不透明，以及正常救济周期难以覆盖春节经营损失。"
                : "该案例需要围绕行政许可、监督检查、处罚裁量、组织职责和救济时效建立制度链，再识别权力集中、流程不透明和机会成本错配形成的寻租风险。");
        response.setRelatedDocuments(fireworks ? fireworksDocuments() : genericDocuments());
        response.setOrganizationNodes(fireworks ? fireworksOrganizations() : genericOrganizations());
        response.setRiskItems(fireworks ? fireworksRiskItems() : genericRiskItems());
        response.setManualImportSuggestions(fireworks ? fireworksImportSuggestions() : genericImportSuggestions());
        return response;
    }

    private CaseAnalysisResponse fallbackJointCaseAnalysis(
            String caseTitle,
            String caseDescription,
            RegulationSetBrief primarySet,
            List<RagEvidence> primaryEvidences,
            List<RagEvidence> externalEvidences
    ) {
        CaseAnalysisResponse response = fallbackCaseAnalysis(caseTitle, caseDescription);
        String setName = primarySet == null ? "主规章集 A" : primarySet.name();
        response.setJointAnalysis(true);
        response.setPrimaryRegulationSetId(primarySet == null ? null : primarySet.id());
        response.setPrimaryRegulationSetName(setName);
        response.setJointSummary("联合分析采用两轮有限迭代：先从案例事实和“" + setName + "”中抽取权力节点、利益节点、时间窗口和制度内生风险，再从 A 外部召回上位法、程序约束、资金规则、监督问责和组织职责证据，判断哪些风险已被部分约束、哪些仍缺少制度衔接。");
        response.setInternalRisks(defaultInternalRisks(setName, caseTitle + "\n" + caseDescription));
        response.setCaseRegulationMappings(defaultCaseRegulationMappings(setName, caseTitle + "\n" + caseDescription));
        response.setExternalConstraints(defaultExternalConstraints(externalEvidences));
        response.setClosureGaps(defaultClosureGaps(setName));
        response.setIterationTraces(List.of(
                iterationTrace(1, "A内部风险抽取", "案例事实 + 主规章集 A + A 内 RAG 证据", primaryEvidences == null ? 0 : primaryEvidences.size(), "已形成 A 内部风险初稿，继续召回外部约束 B。"),
                iterationTrace(2, "外部约束B召回", "上位依据、程序约束、资金规则、信息公开、监督问责、组织职责", externalEvidences == null ? 0 : externalEvidences.size(), "达到两轮有限迭代上限；后续需人工确认是否导入外部制度。")
        ));
        return response;
    }

    private CaseAnalysisResponse fallbackRegulationIteration(
            String caseTitle,
            String caseDescription,
            RegulationSetBrief primarySet,
            List<RagEvidence> primaryEvidences,
            List<RagEvidence> externalEvidences,
            boolean externalDiscovery
    ) {
        CaseAnalysisResponse response = fallbackJointCaseAnalysis(caseTitle, caseDescription, primarySet, primaryEvidences, externalEvidences);
        String setName = primarySet == null ? "用户提供的可信法规 URL" : primarySet.name();
        response.setSummary("已按规章迭代分析口径处理“" + setName + "”：第一轮先识别规章内部的寻租风险和制约措施；" +
                (externalDiscovery ? "第二轮再用外部规章来源匹配可约束条文。" : "当前停留在第一轮，等待人工确认后再选择外部规章来源。"));
        response.setJointSummary("规章迭代分析应在第一轮结果处中断，由用户确认内部风险和制约措施；第二轮只围绕这些制约措施检索外部条文，不把外部材料直接并入主规章集。");
        response.setControlMeasures(defaultControlMeasures(response.getInternalRisks()));
        response.setIterationTraces(List.of(
                iterationTrace(1, "内部寻租风险与制约措施", "主规章集或可信法规 URL + A 内 RAG 证据", primaryEvidences == null ? 0 : primaryEvidences.size(),
                        externalDiscovery ? "已完成第一轮并继续匹配外部约束。" : "已完成第一轮；等待人工确认内部风险和制约措施。"),
                iterationTrace(2, "外部条文匹配", "以第一轮制约措施为线索检索外部规章", externalEvidences == null ? 0 : externalEvidences.size(),
                        externalDiscovery ? "已完成第二轮外部约束排查。" : "尚未执行；需要先选择外部规章集或联网检索结果。")
        ));
        return response;
    }

    private List<CaseAnalysisResponse.InternalRisk> defaultInternalRisks(String setName, String text) {
        if (isFireworksCase(text)) {
            return List.of(
                    internalRisk("案例触发", "TIME_WINDOW_RENT", "许可续办与春节收益窗口重叠", "A 内许可期限、核查和禁燃政策切换叠加，经营者正常救济难以覆盖旺季损失。", setName, "HIGH"),
                    internalRisk("规章集A内生", "APPROVAL_CONCENTRATION", "布点规划、现场核查和发证集中", "同一许可链条控制稀缺网点资格，若公开和复核不足，会放大布点名额寻租。", setName, "HIGH"),
                    internalRisk("案例-A交叉", "BENEFIT_RELATED", "库存回购和迁址资格带来退出补偿裁量", "政策退出、库存核验、批发企业回购和迁址申办交织，易把行政协调变成利益分配。", setName, "MEDIUM")
            );
        }
        return List.of(
                internalRisk("案例触发", "PROCESS_TRANSPARENCY", "案例事实暴露流程公开不足", "案例中的异常时间窗口、材料卡点或结果不确定性，需要回到 A 内制度核查公开、告知和救济条款。", setName, "MEDIUM"),
                internalRisk("规章集A内生", "DISCRETIONARY_POWER", "A 内授权或评审条款可能缺少量化边界", "若 A 只规定原则性条件，缺少评审、回避、留痕和复核程序，权力节点可被非正式影响。", setName, "MEDIUM"),
                internalRisk("案例-A交叉", "SUPERVISION_CONSTRAINT", "监督问责慢于利益窗口", "即使 A 有投诉或复核机制，若时效慢于许可、拨款、验收或处罚窗口，仍会形成寻租激励。", setName, "MEDIUM")
        );
    }

    private List<CaseAnalysisResponse.ControlMeasure> defaultControlMeasures(List<CaseAnalysisResponse.InternalRisk> risks) {
        List<CaseAnalysisResponse.ControlMeasure> measures = new ArrayList<>();
        List<CaseAnalysisResponse.InternalRisk> sourceRisks = risks == null ? List.of() : risks;
        for (CaseAnalysisResponse.InternalRisk risk : sourceRisks.stream().limit(6).toList()) {
            measures.add(controlMeasure(
                    risk.getRiskTitle(),
                    "压缩“" + risk.getRiskType() + "”对应权力节点的非正式交易空间",
                    "把办理条件、材料清单、裁量基准、经办留痕、回避复核和结果公开写成可执行条款，并明确责任主体和时限。",
                    "需要匹配上位法、程序法、信息公开、监督问责或组织职责条文。",
                    risk.getRiskType() + " 上位依据 程序约束 公开 留痕 问责",
                    risk.getRiskLevel()
            ));
        }
        if (measures.isEmpty()) {
            measures.add(controlMeasure(
                    "待核验内部风险",
                    "先建立可被外部制度验证的控制目标",
                    "补充主规章正文后，围绕公开、回避、留痕、复核、问责五类约束生成制约措施。",
                    "主规章正文和外部上位制度。",
                    "信息公开 程序约束 监督问责 组织职责",
                    "MEDIUM"
            ));
        }
        return measures;
    }

    private List<CaseAnalysisResponse.CaseRegulationMapping> defaultCaseRegulationMappings(String setName, String text) {
        if (isFireworksCase(text)) {
            return List.of(
                    caseRegulationMapping("春节前备货和续证窗口重叠", "烟花爆竹经营许可实施办法及地方禁燃限放政策", "时间窗口寻租", "衔接缺失", "A 内有许可期限和现场核查规则，但需要补充旺季前提前受理、快速异议和存量证照衔接规则。"),
                    caseRegulationMapping("禁燃区内存量网点退出和迁址", "地方通告、许可清单、库存回购方案", "利益分配风险", "证据不足", "若没有导入正式通告附件和回购迁址规则，无法判断退出补偿与重新申办是否透明。")
            );
        }
        return List.of(
                caseRegulationMapping("案例中的关键权力节点", setName, "审批/评审/拨付/验收裁量风险", "制度缺口", "需要把案例事实映射到 A 内具体条款，确认是规则缺失还是执行缺失。"),
                caseRegulationMapping("案例中的救济或举报成本", setName, "监督约束不足", "衔接缺失", "需要核查 A 内复核、申诉、公开和问责条款能否在利益窗口内生效。")
        );
    }

    private List<CaseAnalysisResponse.ExternalConstraint> defaultExternalConstraints(List<RagEvidence> externalEvidences) {
        if (externalEvidences != null && !externalEvidences.isEmpty()) {
            List<CaseAnalysisResponse.ExternalConstraint> constraints = new ArrayList<>();
            for (RagEvidence evidence : externalEvidences.stream().limit(5).toList()) {
                constraints.add(externalConstraint(
                        "外部约束证据集 B",
                        evidence.getTitle(),
                        "程序约束/监督问责",
                        "约束 A 中流程公开、回避、资金或问责不足的风险",
                        truncate(evidence.getSnippet(), 180),
                        "部分约束",
                        "作为本次分析证据保留；人工确认适用性后再决定是否导入主规章集或建立衔接关系。"
                ));
            }
            return constraints;
        }
        return List.of(
                externalConstraint("外部约束证据集 B", "行政许可、信息公开、财政资金、科研诚信或监督问责相关上位制度", "上位依据/程序约束", "约束 A 中授权过宽、程序不明和监督滞后的风险", "当前 RAG 未召回足够外部证据，需要人工导入或降低检索阈值后复核。", "证据不足", "建议先作为待导入清单，不直接合并进 A。")
        );
    }

    private List<CaseAnalysisResponse.ClosureGap> defaultClosureGaps(String setName) {
        return List.of(
                closureGap("A 与外部约束 B 缺少显式衔接", "主规章集未明确引用上位程序、回避、公开或问责规则", "执行人员可能只按 A 的本地口径操作，外部约束难以前置进入审批、评审或拨款节点。", "在 A 内增加适用上位依据、回避清单、公开字段、快速复核和移送问责条款。", "HIGH"),
                closureGap("外部证据只提供事后问责，不能替代事前控制", "缺少前置公开、自动回避、过程留痕和时限控制", "即使事后可调查处理，利益窗口期内仍可能选择非正式交易。", "把 B 中的问责规则转化为 A 的办前承诺、办中留痕和办后公开机制。", "MEDIUM")
        );
    }

    private List<CaseAnalysisResponse.RelatedDocument> fireworksDocuments() {
        return List.of(
                document("烟花爆竹安全管理条例", "国务院", "https://www.gov.cn/gongbao/content/2006/content_219931.htm", "建立生产、经营、运输、燃放许可制度，并规定零售许可机关、期限、载明事项和监管人员责任。", "已导入"),
                document("烟花爆竹经营许可实施办法", "原国家安全生产监督管理总局", "https://www.mem.gov.cn/gk/gwgg/agwzlfl/zjl_01/201310/t20131023_233708.shtml", "直接规定零售布点规划、现场核查、20个工作日决定、许可证有效期和公告名单。", "已导入"),
                document("烟花爆竹生产经营安全规定", "原国家安全生产监督管理总局", "https://www.mem.gov.cn/gk/gwgg/agwzlfl/zjl_01/201801/t20180124_233685.shtml", "用于识别安全条件、教育培训、隐患排查、年度监督检查计划和责任体系。", "已导入"),
                document("烟花爆竹零售店（点）安全技术规范 AQ 4128-2019", "应急管理部", "https://www.mem.gov.cn/fw/flfgbz/bz/bzwb/201909/P020220607370319545897.pdf", "用于核验选址、外部距离、面积、存放限量、消防电气等现场核查事项。", "已导入"),
                document("中华人民共和国大气污染防治法", "全国人大常委会", "https://www.mee.gov.cn/ywgz/fgbz/fl/201811/t20181113_673567.shtml", "提供禁燃限放依据，也用于区分燃放限制与合格产品销售许可边界。", "已导入"),
                document("中华人民共和国行政许可法", "全国人大常委会", "https://www.npc.gov.cn/zgrdw/npc/xinwen/2019-05/07/content_2086830.htm", "用于审查许可公开、一次性告知、期限、收费、监督检查和工作人员禁止谋利。", "已导入"),
                document("全国人大常委会法工委2023年备案审查工作情况报告", "全国人大常委会法制工作委员会", "https://www.npc.gov.cn/npc/c2/c30834/202312/t20231229_433996.html", "用于审查地方全面禁售禁燃或变相停办许可是否越过上位法边界。", "已导入"),
                document("郴州市进一步规范销售及禁止和限制燃放烟花爆竹通告、征求意见、听证、合法性审查和事故通报材料", "郴州市人民政府及相关部门", "", "决定禁燃区、停止办理许可、库存回购和迁址申办的事实链，需导入官方全文与附件。", "建议导入")
        );
    }

    private List<CaseAnalysisResponse.OrganizationNode> fireworksOrganizations() {
        return List.of(
                organization("应急管理部及省级应急管理部门", "国家/省级", "制定规章、标准并指导烟花爆竹安全监管。", "上位规则、技术标准和监管口径不清时，基层执行空间会放大。", "导入规章制定说明、标准文本、监管职责。"),
                organization("郴州市人民政府", "市级", "统筹禁燃限放政策、规范性文件发布和重大事项协调。", "禁燃区边界、实施日期、过渡期和存量许可衔接若不公开，会影响经营者预期。", "导入通告正式文本、合法性审查、公平竞争审查和听证材料。"),
                organization("郴州市城市管理和综合执法局", "市级", "牵头起草中心城区销售规范与禁燃限放通告。", "政策起草机关与执行机关之间责任边界不清，可能让经营者无法判断申诉对象。", "导入机构职责、征求意见稿、意见采纳情况。"),
                organization("市、区两级应急管理部门", "市级/区县", "负责零售许可证颁发管理、现场核查、退出宣讲和安全监管。", "同一链条掌握布点、核查、发证、停办、复查和迁址判断，审批集中度高。", "导入许可清单、办事指南、现场核查表、取证名单公告。"),
                organization("属地街道和社区", "街道", "参与政策宣讲、群众工作、经营户联系和隐患排查。", "口头宣讲若缺少书面告知和回执，经营者难以证明自身权利和救济期限。", "导入上门宣讲记录、送达回执、整改通知。"),
                organization("公安、市场监管、消防救援等部门", "市级/区县", "分别涉及燃放秩序、产品质量、营业登记、消防安全等协同监管。", "多部门协同缺少主责和移送规则时，可能形成重复检查、选择性执法或推诿。", "导入联合执法方案、检查记录、线索移送规则。"),
                organization("纪检监察机关", "区县/市级", "受理监管人员违纪违法线索并开展责任追究。", "举报渠道若无法快速处理旺季前许可卡点，救济机会成本仍高于非正式支付。", "导入投诉举报办理时限、移送规则和反馈机制。"),
                organization("烟花爆竹批发企业", "企业", "承担供货、配送和库存回购协同。", "原价回购、库存核验和付款时限不透明时，企业与监管链条可能形成利益捆绑。", "导入回购协议、库存清点表、价格和付款凭证。")
        );
    }

    private List<CaseAnalysisResponse.CaseRiskItem> fireworksRiskItems() {
        return List.of(
                caseRisk(
                        "TIME_WINDOW_RENT",
                        "续证窗口与春节备货销售期重叠导致经营者被迫用非正式成本换取确定性",
                        "县级应急管理部门掌握受理、现场核查和发证节奏；经营者面对春节前库存、租金和客流窗口；可交换利益是提前核查、加速办结或获得有利解释。",
                        "烟花爆竹零售高度季节化，普通20个工作日许可决定和公历年末到期安排可能覆盖主要备货期。行政复议、诉讼或纪检举报很难在春节前给出经营确定性，正常救济机会成本高于贿赂成本。",
                        "需核验许可证有效期、续证受理公告、现场核查排期、春节前办结统计和不予许可理由。",
                        "建立旺季前提前受理和倒排时限机制；统一许可证到期衔接规则；公开办件排队、补正、核查和办结节点；设置旺季前快速异议处理窗口。",
                        "HIGH"
                ),
                caseRisk(
                        "APPROVAL_CONCENTRATION",
                        "布点规划、现场核查、发证管理集中在同一许可链条形成稀缺名额分配空间",
                        "许可机关同时影响网点总量、地址是否符合布点、现场核查结论和许可证期限；经营者为进入客流区域或保留存量资格承担高额机会成本；可交换利益是布点名额、地址认可和更长有效期。",
                        "经营许可实施办法要求统一规划、合理布局、总量控制和现场核查，但若规划图、数量依据、申请排序和核查意见不公开，公共安全管理会转化为稀缺经营资格分配。",
                        "需导入布点规划、网点数量调整依据、取证企业名单公告、现场核查意见和申请排序规则。",
                        "公开布点规划图、名额测算依据和调整周期；现场核查双人签名并上传照片、坐标和测距数据；同区域申请按公开规则排序。",
                        "HIGH"
                ),
                caseRisk(
                        "DISCRETIONARY_POWER",
                        "禁燃区边界和安全距离测量口径不透明使相邻店铺可能受到差别待遇",
                        "政策制定机关和现场核查人员掌握禁燃区边界、安全距离起止点、重点场所识别和同一建筑判断；经营者处在能否继续经营的临界位置；可交换利益是边界解释和测量结论。",
                        "禁燃限放与零售许可衔接时，一条街、一个路口或一个重点场所认定就能决定店铺退出或保留。缺少坐标化边界和复核机制时，同类店铺之间很难证明差别待遇。",
                        "需导入禁燃区边界图、测距规则、现场照片、坐标记录和复核意见。",
                        "通告附件必须提供可检索边界图和生效日期；现场测量采用统一起止点、设备、照片和申请人确认；边界争议设复测与第三方复核。",
                        "HIGH"
                ),
                caseRisk(
                        "PROCESS_TRANSPARENCY",
                        "停止办理相关行政许可缺少分类口径会把新办、续办、变更和迁址混成可裁量事项",
                        "政策执行机关掌握停止新办、停止续办、停止变更、存量证照效力和异地迁址重新申办口径；经营者需要快速判断是否备货、退租或迁址；可交换利益是获得例外办理或更有利衔接。",
                        "“停止办理相关行政许可”如果未拆分许可类型和存量权利，就会让经办人员用口头解释影响经营者预期，特别是在既有许可证仍未到期时。",
                        "需导入通告正式文本、政策解读、办事窗口口径、存量许可证处理清单和迁址申办指南。",
                        "把停止办理拆成新办、续办、变更、迁址、存量有效期、库存处置六类规则；每类列明依据、适用对象、办理窗口和救济期限。",
                        "HIGH"
                ),
                caseRisk(
                        "BENEFIT_RELATED",
                        "库存原价回购和批发企业协调缺少清点付款规则会产生利益输送空间",
                        "应急部门、街道和批发企业共同影响库存核验、品类认定、价格确认、回购时间和付款；经营者背负库存损失与安全责任；可交换利益是进入回购名单、获得更高数量或更快付款。",
                        "回购本意是降低退出损失，但如果未公开回购主体、价格、凭证、损耗认定和争议处理，行政协调会把经营风险转嫁成可交换利益。",
                        "需导入回购方案、库存清点表、价格确认书、批发企业协议、付款凭证和争议处理记录。",
                        "建立统一库存清点模板，双人核验并由经营者签字；公开回购价格公式、付款期限、争议复核和未回购库存处置方式。",
                        "MEDIUM"
                ),
                caseRisk(
                        "SUPERVISION_CONSTRAINT",
                        "政策宣讲、隐患检查和退出工作缺少留痕会削弱对基层工作人员的约束",
                        "属地街道、应急监管人员和协同部门直接接触经营者；经营者需要依赖口头信息安排退货、迁址或申诉；可交换利益是更早获知政策、减少检查压力或得到非正式协调。",
                        "案例中政策切换、许可证到期和安全事故处在同一窗口，若上门宣讲、整改通知、检查记录、投诉处理没有可追溯证据，就难以分辨依法履责、拖延卡点或选择性执法。",
                        "需导入宣讲记录、送达回执、检查台账、隐患整改复查和投诉举报办理记录。",
                        "所有上门工作使用统一记录单；关键告知事项书面送达并上传系统；检查和复查双人留痕；投诉举报建立旺季前限时反馈。",
                        "HIGH"
                )
        );
    }

    private List<CaseAnalysisResponse.ManualImportSuggestion> fireworksImportSuggestions() {
        return List.of(
                importSuggestion("地方政策", "郴州市《关于进一步规范市城区销售及禁止和限制燃放烟花爆竹的通告》正式文本、附件边界图和政策解读", "郴州市人民政府网站、郴州市城市管理和综合执法局网站", "决定禁燃区范围、停止办理许可和存量网点退出，是案例分析的地方政策核心。"),
                importSuggestion("组织架构", "郴州市城管局、应急管理局、北湖区应急管理局、属地街道职责和权责清单", "市、区政府部门官网机构职能栏目", "用于判断谁制定政策、谁发证、谁上门宣讲、谁监督检查。"),
                importSuggestion("行政许可清单", "烟花爆竹零售经营许可办事指南、材料目录、承诺时限、补正规则、取证名单公告", "政务服务网、应急管理部门官网", "用于量化许可窗口、材料卡点、现场核查和结果公开是否足以抑制寻租。"),
                importSuggestion("事故调查", "勇鑫烟花零售店燃爆事件官方调查通报、责任处理决定和整改方案", "郴州市人民政府网站、官方新闻发布渠道", "用于核验责任链和制度漏洞，避免只凭报道推断。"),
                importSuggestion("地方政策", "库存回购方案、迁址重新申办指南、禁燃区内存量许可证处理清单", "应急管理部门、街道或联合工作专班公开材料", "用于分析退出补偿、迁址资格和重新申办排序是否透明。")
        );
    }

    private List<CaseAnalysisResponse.RelatedDocument> genericDocuments() {
        return List.of(
                document("中华人民共和国行政许可法", "全国人大常委会", "https://www.npc.gov.cn/zgrdw/npc/xinwen/2019-05/07/content_2086830.htm", "审查许可条件、数量、程序、期限、收费和监督检查公开。", "建议导入"),
                document("中华人民共和国行政处罚法", "全国人大常委会", "", "审查处罚裁量、告知、听证、执法全过程记录和救济。", "建议导入"),
                document("优化营商环境条例", "国务院", "", "审查市场准入、行政检查、政务服务和涉企政策稳定性。", "建议导入"),
                document("中华人民共和国政府信息公开条例", "国务院", "", "审查政策、许可结果、执法信息和组织职责公开。", "建议导入")
        );
    }

    private List<CaseAnalysisResponse.OrganizationNode> genericOrganizations() {
        return List.of(
                organization("行政许可实施机关", "区县/市级", "受理、审查、现场核查和作出许可决定。", "材料补正、现场核查、办理时限和结果公开可能形成卡点。", "导入权责清单、办事指南和办件台账。"),
                organization("行业主管部门", "市级/区县", "制定行业政策、监督检查和整改复查。", "政策口径和检查频次不透明会增加经营者不确定性。", "导入部门职责、年度检查计划和检查记录。"),
                organization("政务服务窗口", "区县/市级", "提供材料收件、补正告知和办理进度反馈。", "一次性告知不足会形成反复补正和中介代办空间。", "导入材料清单、补正记录和承诺时限。"),
                organization("纪检监察或投诉举报部门", "区县/市级", "受理工作人员违纪违法线索和行政效能投诉。", "救济慢于经营窗口时，经营者仍可能选择非正式路径。", "导入投诉渠道、办理时限和反馈规则。")
        );
    }

    private List<CaseAnalysisResponse.CaseRiskItem> genericRiskItems() {
        return List.of(
                caseRisk("TIME_WINDOW_RENT", "许可办理期与经营收益窗口错配产生加速办结寻租", "许可机关掌握时限和排队；经营者担心错过收益窗口；可交换利益是提前受理或加速核查。", "法定期限不等于商业可承受期限，若救济周期长于收益窗口，寻租激励会被放大。", "需导入许可期限、旺季数据、办件排队和救济周期。", "设置提前受理、公开排队和限时异议处理。", "MEDIUM"),
                caseRisk("PROCESS_TRANSPARENCY", "材料目录和补正口径不透明导致反复补正卡点", "窗口和经办人掌握材料解释；经营者依赖非正式辅导；可交换利益是一次通过。", "没有一次性告知和示范文本时，材料瑕疵可被反复使用。", "需导入材料目录、补正记录和办事指南。", "固定材料清单、补正依据、经办人和申请人确认。", "MEDIUM"),
                caseRisk("DISCRETIONARY_POWER", "现场核查标准缺少量化留痕导致同类主体差别待遇", "检查人员掌握合格判断；经营者面临停业或整改损失；可交换利益是通过核查或缩短复查。", "现场标准未表格化、照片化、坐标化时，结论难以复核。", "需导入核查表、照片、复查记录。", "双人核查、统一清单、证据上传和复核机制。", "MEDIUM"),
                caseRisk("SUPERVISION_CONSTRAINT", "投诉举报和复议诉讼无法覆盖紧急经营窗口", "监督机关处理慢；经营者损失即时发生；可交换利益是避免被卡点。", "救济路径存在但时效不足，会降低守法维权吸引力。", "需导入投诉时限、复议诉讼周期和应急处理规则。", "建立涉企许可急难问题限时反馈和纪检移送机制。", "MEDIUM")
        );
    }

    private List<CaseAnalysisResponse.ManualImportSuggestion> genericImportSuggestions() {
        return List.of(
                importSuggestion("法律公文", "行政许可、行政处罚、政府信息公开、优化营商环境相关上位法", "中国人大网、中国政府网、主管部门官网", "建立审查边界，避免只分析单份制度。"),
                importSuggestion("组织架构", "涉案部门机构职能、权责清单和岗位职责", "地方政府部门官网、政务服务网", "判断权力节点和责任边界。"),
                importSuggestion("行政许可清单", "办事指南、材料目录、承诺时限、补正告知规则和办理结果公告", "政务服务网、主管部门官网", "识别材料卡点、时限卡点和结果公开不足。"),
                importSuggestion("事故调查", "事故通报、责任处理、整改方案或审计监察材料", "政府官网、官方发布渠道", "核验事实链和组织责任链。")
        );
    }

    private List<String> evidenceSnippets(List<RagEvidence> evidences) {
        if (evidences == null || evidences.isEmpty()) {
            return List.of();
        }
        return evidences.stream()
                .map(evidence -> "[证据" + evidence.getCitationNo() + "] "
                        + evidence.getTitle()
                        + "："
                        + truncate(evidence.getSnippet(), 900))
                .toList();
    }

    private List<String> evidenceSnippets(List<RagEvidence> primaryEvidences, List<RagEvidence> externalEvidences) {
        List<String> snippets = new ArrayList<>();
        if (primaryEvidences != null) {
            snippets.addAll(primaryEvidences.stream()
                    .map(evidence -> "[A内证据" + evidence.getCitationNo() + "] "
                            + evidence.getTitle()
                            + "："
                            + truncate(evidence.getSnippet(), 900))
                    .toList());
        }
        if (externalEvidences != null) {
            snippets.addAll(externalEvidences.stream()
                    .map(evidence -> "[B外部约束" + evidence.getCitationNo() + "] "
                            + evidence.getTitle()
                            + "："
                            + truncate(evidence.getSnippet(), 900))
                    .toList());
        }
        return snippets;
    }

    private CaseAnalysisResponse.RelatedDocument document(
            String title,
            String issuingBody,
            String sourceUrl,
            String relevance,
            String importStatus
    ) {
        CaseAnalysisResponse.RelatedDocument document = new CaseAnalysisResponse.RelatedDocument();
        document.setTitle(title);
        document.setIssuingBody(issuingBody);
        document.setSourceUrl(sourceUrl);
        document.setRelevance(relevance);
        document.setImportStatus(importStatus);
        return document;
    }

    private CaseAnalysisResponse.OrganizationNode organization(
            String organization,
            String level,
            String role,
            String riskResponsibility,
            String evidenceNeed
    ) {
        CaseAnalysisResponse.OrganizationNode node = new CaseAnalysisResponse.OrganizationNode();
        node.setOrganization(organization);
        node.setLevel(level);
        node.setRole(role);
        node.setRiskResponsibility(riskResponsibility);
        node.setEvidenceNeed(evidenceNeed);
        return node;
    }

    private CaseAnalysisResponse.CaseRiskItem caseRisk(
            String indicatorCode,
            String title,
            String mechanism,
            String reason,
            String evidence,
            String suggestion,
            String riskLevel
    ) {
        CaseAnalysisResponse.CaseRiskItem item = new CaseAnalysisResponse.CaseRiskItem();
        item.setIndicatorCode(indicatorCode);
        item.setTitle(title);
        item.setMechanism(mechanism);
        item.setReason(reason);
        item.setEvidence(evidence);
        item.setSuggestion(suggestion);
        item.setRiskLevel(riskLevel);
        return item;
    }

    private CaseAnalysisResponse.ManualImportSuggestion importSuggestion(
            String category,
            String title,
            String suggestedSource,
            String reason
    ) {
        CaseAnalysisResponse.ManualImportSuggestion suggestion = new CaseAnalysisResponse.ManualImportSuggestion();
        suggestion.setCategory(category);
        suggestion.setTitle(title);
        suggestion.setSuggestedSource(suggestedSource);
        suggestion.setReason(reason);
        return suggestion;
    }

    private CaseAnalysisResponse.InternalRisk internalRisk(
            String source,
            String riskType,
            String riskTitle,
            String mechanism,
            String relatedRegulations,
            String riskLevel
    ) {
        CaseAnalysisResponse.InternalRisk item = new CaseAnalysisResponse.InternalRisk();
        item.setSource(source);
        item.setRiskType(riskType);
        item.setRiskTitle(riskTitle);
        item.setMechanism(mechanism);
        item.setRelatedRegulations(relatedRegulations);
        item.setRiskLevel(riskLevel);
        return item;
    }

    private CaseAnalysisResponse.ControlMeasure controlMeasure(
            String riskTitle,
            String controlObjective,
            String measure,
            String evidenceNeed,
            String externalSearchHint,
            String priority
    ) {
        CaseAnalysisResponse.ControlMeasure item = new CaseAnalysisResponse.ControlMeasure();
        item.setRiskTitle(riskTitle);
        item.setControlObjective(controlObjective);
        item.setMeasure(measure);
        item.setEvidenceNeed(evidenceNeed);
        item.setExternalSearchHint(externalSearchHint);
        item.setPriority(priority);
        return item;
    }

    private CaseAnalysisResponse.CaseRegulationMapping caseRegulationMapping(
            String caseFact,
            String matchedRegulation,
            String matchedRisk,
            String gapType,
            String explanation
    ) {
        CaseAnalysisResponse.CaseRegulationMapping item = new CaseAnalysisResponse.CaseRegulationMapping();
        item.setCaseFact(caseFact);
        item.setMatchedRegulation(matchedRegulation);
        item.setMatchedRisk(matchedRisk);
        item.setGapType(gapType);
        item.setExplanation(explanation);
        return item;
    }

    private CaseAnalysisResponse.ExternalConstraint externalConstraint(
            String sourceSet,
            String documentTitle,
            String constraintType,
            String constrainsRisk,
            String constraintMechanism,
            String sufficiency,
            String importAdvice
    ) {
        CaseAnalysisResponse.ExternalConstraint item = new CaseAnalysisResponse.ExternalConstraint();
        item.setSourceSet(sourceSet);
        item.setDocumentTitle(documentTitle);
        item.setConstraintType(constraintType);
        item.setConstrainsRisk(constrainsRisk);
        item.setConstraintMechanism(constraintMechanism);
        item.setSufficiency(sufficiency);
        item.setImportAdvice(importAdvice);
        return item;
    }

    private CaseAnalysisResponse.ClosureGap closureGap(
            String gapTitle,
            String missingLink,
            String remainingRisk,
            String recommendedAction,
            String priority
    ) {
        CaseAnalysisResponse.ClosureGap item = new CaseAnalysisResponse.ClosureGap();
        item.setGapTitle(gapTitle);
        item.setMissingLink(missingLink);
        item.setRemainingRisk(remainingRisk);
        item.setRecommendedAction(recommendedAction);
        item.setPriority(priority);
        return item;
    }

    private CaseAnalysisResponse.IterationTrace iterationTrace(
            Integer round,
            String objective,
            String query,
            Integer evidenceCount,
            String stopReason
    ) {
        CaseAnalysisResponse.IterationTrace item = new CaseAnalysisResponse.IterationTrace();
        item.setRound(round);
        item.setObjective(objective);
        item.setQuery(query);
        item.setEvidenceCount(evidenceCount);
        item.setStopReason(stopReason);
        return item;
    }

    private boolean isFireworksCase(String text) {
        String value = text == null ? "" : text;
        return value.contains("烟花") || value.contains("爆竹") || value.contains("禁燃") || value.contains("燃放");
    }

    private boolean isOfficialSource(URI uri) {
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        return host.endsWith(".gov.cn")
                || host.endsWith(".npc.gov.cn")
                || host.endsWith(".mem.gov.cn")
                || host.endsWith(".mee.gov.cn")
                || host.endsWith(".court.gov.cn")
                || host.endsWith(".samr.gov.cn")
                || host.equals("www.gov.cn")
                || host.equals("www.npc.gov.cn");
    }

    private String extractHtmlTitle(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        Matcher matcher = Pattern.compile("(?is)<title[^>]*>(.*?)</title>").matcher(html);
        return matcher.find() ? cleanupText(matcher.group(1)) : "";
    }

    private String plainTextOf(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String text = value
                .replaceAll("(?is)<script[^>]*>.*?</script>", " ")
                .replaceAll("(?is)<style[^>]*>.*?</style>", " ")
                .replaceAll("(?is)<noscript[^>]*>.*?</noscript>", " ")
                .replaceAll("(?is)<br\\s*/?>", "\n")
                .replaceAll("(?is)</p>", "\n")
                .replaceAll("(?is)<[^>]+>", " ");
        return cleanupText(text);
    }

    private String cleanupText(String value) {
        return (value == null ? "" : value)
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replaceAll("&#\\d+;", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String fileNameOf(URI uri) {
        String path = uri.getPath();
        if (path == null || path.isBlank()) {
            return uri.toString();
        }
        int index = path.lastIndexOf('/');
        return index >= 0 && index < path.length() - 1 ? path.substring(index + 1) : path;
    }

    private RegulationAiSource loadRegulation(Long regulationId) {
        RegulationAiSource source = jdbcTemplate.query("""
                        SELECT r.title, COALESCE(rc.plain_text, rc.content, '') AS content
                        FROM regulation r
                        LEFT JOIN regulation_content rc ON rc.regulation_id = r.id
                        WHERE r.id = ? AND r.deleted = 0
                        """,
                rs -> {
                    if (!rs.next()) {
                        return null;
                    }
                    return new RegulationAiSource(rs.getString("title"), rs.getString("content"));
                },
                regulationId
        );
        if (source == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Regulation not found");
        }
        return source;
    }

    private String localQaAnswer(RegulationAiSource source, String question) {
        if (source.content == null || source.content.isBlank()) {
            return "该制度尚未维护正文，无法基于制度内容回答。";
        }
        List<String> terms = Arrays.stream(question.replaceAll("[^\\p{IsHan}A-Za-z0-9]", " ").split("\\s+"))
                .filter(term -> term.length() >= 2)
                .toList();
        List<String> clauses = Arrays.stream(source.content.split("[。；;\\n]"))
                .map(String::trim)
                .filter(clause -> !clause.isBlank())
                .toList();
        String matched = clauses.stream()
                .filter(clause -> terms.stream().anyMatch(clause::contains))
                .findFirst()
                .orElse(clauses.get(0));
        return "基于《" + source.title + "》正文，可参考：" + matched + "。";
    }

    private String loadTemplate(String sceneCode) {
        String template = jdbcTemplate.query("""
                        SELECT prompt_content
                        FROM ai_prompt_template
                        WHERE scene_code = ? AND enabled = 1
                        """,
                rs -> rs.next() ? rs.getString("prompt_content") : null,
                sceneCode
        );
        return template == null ? "请基于给定材料输出简明结论。" : template;
    }

    private String configValue(String key, String defaultValue) {
        String value = jdbcTemplate.query("""
                        SELECT config_value
                        FROM sys_config
                        WHERE config_key = ?
                        """,
                rs -> rs.next() ? rs.getString("config_value") : null,
                key
        );
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private boolean isLlmConfigured() {
        return Boolean.parseBoolean(configValue("ai.enabled", String.valueOf(aiProperties.isEnabled())))
                && "openai-compatible".equalsIgnoreCase(configValue("ai.provider", aiProperties.getProvider()))
                && hasText(configValue("ai.base-url", aiProperties.getBaseUrl()))
                && hasText(configValue("ai.api-key", aiProperties.getApiKey()))
                && hasText(configValue("ai.model", aiProperties.getModel()));
    }

    private String complete(String prompt) {
        String baseUrl = configValue("ai.base-url", aiProperties.getBaseUrl());
        String apiKey = configValue("ai.api-key", aiProperties.getApiKey());
        String model = configValue("ai.model", aiProperties.getModel());
        RuntimeException lastError = null;
        for (String url : chatCompletionsUrls(baseUrl)) {
            try {
                String response = restClient().post()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + apiKey)
                        .body("""
                                {
                                  "model": "%s",
                                  "temperature": 0.2,
                                  "messages": [
                                    {
                                      "role": "system",
                                      "content": "你是制度文本和监管政策寻租风险分析专家。必须只返回合法 JSON，不要使用 Markdown。"
                                    },
                                    {
                                      "role": "user",
                                      "content": %s
                                    }
                                  ]
                                }
                                """.formatted(escapeJson(model), quoteJson(prompt)))
                        .retrieve()
                        .body(String.class);
                return parseChatContent(response);
            } catch (RuntimeException ex) {
                lastError = ex;
            }
        }
        if (lastError instanceof BizException bizException) {
            throw bizException;
        }
        throw new BizException(ErrorCode.AI_SERVICE_ERROR, lastError == null ? "LLM service call failed" : lastError.getMessage());
    }

    private String parseChatContent(String response) {
        if (response == null || response.isBlank()) {
            throw new BizException(ErrorCode.AI_SERVICE_ERROR, "LLM returned empty response");
        }
        try {
            JsonNode root = objectMapper.readTree(response);
            String content = root.path("choices").path(0).path("message").path("content").asText();
            if (content == null || content.isBlank()) {
                throw new BizException(ErrorCode.AI_SERVICE_ERROR, "LLM response content is empty");
            }
            return content;
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(ErrorCode.AI_SERVICE_ERROR, "LLM response format is invalid");
        }
    }

    private RentSeekingRiskResult parseRiskResult(String responseText) throws Exception {
        JsonNode root = objectMapper.readTree(extractJsonObject(responseText));
        RentSeekingRiskResult result = new RentSeekingRiskResult();
        result.setSummary(text(root, "summary", "未返回摘要"));
        result.setRiskLevel(normalizeRiskLevel(text(root, "riskLevel", "MEDIUM")));
        result.setRiskScore(Math.max(0, Math.min(100, root.path("riskScore").asInt(50))));
        result.setOverallSuggestion(text(root, "overallSuggestion", "建议结合人工复核完善制度流程、裁量边界和监督约束。"));
        result.setRawResponse(responseText);

        List<RentSeekingRiskItem> items = new ArrayList<>();
        JsonNode riskItems = root.path("riskItems");
        if (riskItems.isArray()) {
            for (JsonNode node : riskItems) {
                RentSeekingRiskItem item = new RentSeekingRiskItem();
                item.setIndicatorCode(text(node, "indicatorCode", "PROCESS_TRANSPARENCY"));
                item.setTitle(text(node, "title", "寻租风险点"));
                item.setDescription(text(node, "description", "LLM 识别到潜在制度风险。"));
                item.setReason(text(node, "reason", "制度表述可能存在执行空间。"));
                item.setSuggestion(text(node, "suggestion", "建议明确标准、流程、监督和留痕要求。"));
                item.setRelatedClause(text(node, "relatedClause", ""));
                item.setRiskLevel(normalizeRiskLevel(text(node, "riskLevel", result.getRiskLevel())));
                items.add(item);
            }
        }
        if (items.isEmpty()) {
            RentSeekingRiskItem item = new RentSeekingRiskItem();
            item.setIndicatorCode("PROCESS_TRANSPARENCY");
            item.setTitle("寻租风险提示");
            item.setDescription("LLM 未返回风险明细。");
            item.setReason("结构化结果中 riskItems 为空。");
            item.setSuggestion("建议人工复核制度文本，并补充公开、公示、监督和问责机制。");
            item.setRelatedClause("");
            item.setRiskLevel(result.getRiskLevel());
            items.add(item);
        }
        result.setRiskItems(items);
        return result;
    }

    private String riskAnalysisPrompt(
            String title,
            String publishDepartment,
            String applicableScope,
            String content,
            String ruleContext,
            String ragContext
    ) {
        String template = loadTemplate("RENT_SEEKING_RISK");
        return """
                %s

                请对以下制度文本或外部监管政策进行寻租风险分析，识别自由裁量、审批集中、流程透明度不足、监督约束不足、利益分配、处罚弹性、时间窗口寻租等风险。
                你必须基于“本地规则线索”“RAG 检索证据”和“正文条款”输出，不要写泛泛的合规套话。
                每个 riskItem 必须满足：
                1. title 写成具体风险，例如“20个工作日许可决定与春节备货窗口重叠”。
                2. description 写清楚“权力节点 + 经营者处境 + 可交换利益”。
                3. reason 写清楚“制度条款如何放大寻租激励”，并引用证据编号，例如“依据[证据1]”。
                4. suggestion 写出可执行制度修订动作，例如提前续证窗口、一次性告知清单、现场核查双人留痕、回购价格公式、异议快速处理时限。
                5. relatedClause 必须摘取或概括最相关的一段制度原文，不得留空。
                6. 至少返回 4 个 riskItems；如果证据不足，也要说明不足的制度字段。
                必须返回如下 JSON 结构：
                {
                  "summary": "制度摘要",
                  "riskLevel": "LOW|MEDIUM|HIGH|CRITICAL",
                  "riskScore": 0-100,
                  "overallSuggestion": "总体整改建议",
                  "riskItems": [
                    {
                      "indicatorCode": "DISCRETIONARY_POWER|APPROVAL_CONCENTRATION|PROCESS_TRANSPARENCY|SUPERVISION_CONSTRAINT|BENEFIT_RELATED|PENALTY_FLEXIBILITY|TIME_WINDOW_RENT",
                      "title": "风险点标题",
                      "description": "风险描述",
                      "reason": "判定原因",
                      "suggestion": "整改建议",
                      "relatedClause": "关联条款原文",
                      "riskLevel": "LOW|MEDIUM|HIGH|CRITICAL"
                    }
                  ]
                }

                文本标题：%s
                发布部门：%s
                适用范围：%s
                本地规则线索：
                %s

                RAG 检索证据：
                %s

                正文：%s
                """.formatted(
                template,
                nullToEmpty(title),
                nullToEmpty(publishDepartment),
                nullToEmpty(applicableScope),
                nullToEmpty(ruleContext),
                nullToEmpty(ragContext),
                nullToEmpty(content)
        );
    }

    private String extractJsonObject(String value) {
        String text = value == null ? "" : value.trim();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```[a-zA-Z]*\\s*", "").replaceFirst("\\s*```$", "").trim();
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new BizException(ErrorCode.AI_SERVICE_ERROR, "LLM response does not contain JSON object");
        }
        return text.substring(start, end + 1);
    }

    private String normalizeRiskLevel(String value) {
        String riskLevel = value == null ? "" : value.trim().toUpperCase();
        return switch (riskLevel) {
            case "LOW", "MEDIUM", "HIGH", "CRITICAL" -> riskLevel;
            default -> "MEDIUM";
        };
    }

    private String text(JsonNode node, String field, String defaultValue) {
        String value = node.path(field).asText(null);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String text = value.trim();
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private RestClient restClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        Duration timeout = timeout();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        return RestClient.builder().requestFactory(requestFactory).build();
    }

    private HttpResponse<String> sendString(HttpClient client, HttpRequest request) throws Exception {
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (SSLException ex) {
            return insecureHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        }
    }

    private HttpClient insecureHttpClient() throws Exception {
        TrustManager[] trustManagers = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }
        };
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagers, new SecureRandom());
        return HttpClient.newBuilder()
                .sslContext(sslContext)
                .connectTimeout(Duration.ofSeconds(8))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    private Duration timeout() {
        String configured = configValue("ai.timeout", null);
        if (configured == null || configured.isBlank()) {
            return aiProperties.getTimeout();
        }
        try {
            return Duration.parse(configured.trim());
        } catch (Exception ignored) {
            String value = configured.trim().toLowerCase();
            if (value.endsWith("ms")) {
                return Duration.ofMillis(Long.parseLong(value.substring(0, value.length() - 2)));
            }
            if (value.endsWith("s")) {
                return Duration.ofSeconds(Long.parseLong(value.substring(0, value.length() - 1)));
            }
            return Duration.ofSeconds(Long.parseLong(value));
        }
    }

    private List<String> chatCompletionsUrls(String baseUrl) {
        String normalized = (baseUrl == null ? "" : baseUrl.trim()).replaceAll("/+$", "");
        List<String> urls = new ArrayList<>();
        if (normalized.endsWith("/chat/completions")) {
            urls.add(normalized);
            return urls;
        }
        String lower = normalized.toLowerCase();
        if (lower.contains("generativelanguage.googleapis.com") && !lower.contains("/openai")) {
            addUrl(urls, normalized + "/openai/chat/completions");
        }
        if (!lower.endsWith("/v1") && !lower.contains("/v1/") && !lower.contains("/openai")) {
            addUrl(urls, normalized + "/v1/chat/completions");
        }
        addUrl(urls, normalized + "/chat/completions");
        return urls;
    }

    private void addUrl(List<String> urls, String url) {
        if (!url.isBlank() && !urls.contains(url)) {
            urls.add(url);
        }
    }

    private String quoteJson(String value) {
        try {
            return objectMapper.writeValueAsString(value == null ? "" : value);
        } catch (Exception ex) {
            return "\"\"";
        }
    }

    private String escapeJson(String value) {
        return (value == null ? "" : value).replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private AiRecordResponse mapRecord(ResultSet rs) throws java.sql.SQLException {
        AiRecordResponse record = new AiRecordResponse();
        record.setId(rs.getLong("id"));
        record.setSceneCode(rs.getString("scene_code"));
        record.setBusinessType(rs.getString("business_type"));
        long businessId = rs.getLong("business_id");
        record.setBusinessId(rs.wasNull() ? null : businessId);
        record.setModelName(rs.getString("model_name"));
        record.setPrompt(rs.getString("prompt"));
        record.setResponseText(rs.getString("response_text"));
        record.setParsedJson(rs.getString("parsed_json"));
        record.setStatus(rs.getString("status"));
        record.setErrorMessage(rs.getString("error_message"));
        record.setCreatedByName(rs.getString("created_by_name"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        record.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
        return record;
    }

    private record RegulationAiSource(String title, String content) {
    }

    private record RegulationCaseSource(Long id, String title, String publishDepartment, String content) {
    }

    private record RegulationSetBrief(Long id, String name) {
    }
}
