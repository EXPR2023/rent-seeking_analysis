package com.rentseeking.analysis.ai.service;

import com.rentseeking.analysis.ai.dto.WebSourceDiscoveryRequest;
import com.rentseeking.analysis.ai.dto.WebSourceDiscoveryResponse;
import com.rentseeking.analysis.ai.dto.WebSourceImportRequest;
import com.rentseeking.analysis.ai.dto.WebSourceImportResponse;
import com.rentseeking.analysis.auth.security.JwtUserPrincipal;
import com.rentseeking.analysis.auth.util.SecurityUtils;
import com.rentseeking.analysis.common.constant.ErrorCode;
import com.rentseeking.analysis.common.exception.BizException;
import com.rentseeking.analysis.rag.service.KnowledgeRetrievalService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
public class WebSourceDiscoveryServiceImpl implements WebSourceDiscoveryService {

    private static final String AUTO_SET_CODE = "WEB_DISCOVERED_CONSTRAINTS";
    private static final String AUTO_SET_NAME = "联网发现外部约束集";
    private static final int SEARCH_QUERY_LIMIT = 8;
    private static final int DISCOVERY_RESULT_LIMIT = 30;
    private static final int IMPORT_CONTENT_LIMIT = 30000;
    private static final Pattern HREF_PATTERN = Pattern.compile("(?is)href=[\"']([^\"'#]+)[\"']");
    private static final Pattern RSS_LINK_PATTERN = Pattern.compile("(?is)<link>(https?://[^<]+)</link>");

    private final JdbcTemplate jdbcTemplate;
    private final KnowledgeRetrievalService knowledgeRetrievalService;

    public WebSourceDiscoveryServiceImpl(JdbcTemplate jdbcTemplate, KnowledgeRetrievalService knowledgeRetrievalService) {
        this.jdbcTemplate = jdbcTemplate;
        this.knowledgeRetrievalService = knowledgeRetrievalService;
    }

    @Override
    public WebSourceDiscoveryResponse discover(WebSourceDiscoveryRequest request) {
        String baseQuery = baseQuery(request);
        List<String> queries = buildQueries(request, baseQuery);
        int maxResults = clamp(request.getMaxResults() == null ? 18 : request.getMaxResults(), 1, DISCOVERY_RESULT_LIMIT);
        HttpClient client = httpClient();
        Map<String, WebDocument> documents = new LinkedHashMap<>();
        for (String manualUrl : request.getSourceUrls() == null ? List.<String>of() : request.getSourceUrls()) {
            String normalized = normalizeUrl(manualUrl);
            if (!normalized.isBlank() && !documents.containsKey(normalized)) {
                documents.put(normalized, fetchDocument(client, normalized, false));
            }
        }
        for (String seedUrl : seedUrlsFor(baseQuery + " " + nullToEmpty(request.getCaseDescription()))) {
            if (documents.size() >= maxResults) {
                break;
            }
            documents.put(seedUrl, fetchDocument(client, seedUrl, false));
        }
        for (String query : queries) {
            if (documents.size() >= maxResults) {
                break;
            }
            for (String url : searchOfficialUrls(client, query)) {
                String normalized = normalizeUrl(url);
                if (normalized.isBlank() || documents.containsKey(normalized) || !isAllowedCandidate(normalized)) {
                    continue;
                }
                documents.put(normalized, fetchDocument(client, normalized, false));
                if (documents.size() >= maxResults) {
                    break;
                }
            }
        }

        WebSourceDiscoveryResponse response = new WebSourceDiscoveryResponse();
        response.setQuery(baseQuery);
        response.setSearchedQueries(queries);
        response.setDiscoveredAt(LocalDateTime.now());
        response.setCandidates(documents.values().stream().map(this::candidateOf).toList());
        return response;
    }

    @Override
    @Transactional
    public WebSourceImportResponse importSources(WebSourceImportRequest request) {
        List<WebSourceImportRequest.ImportSource> sources = request.getSources() == null ? List.of() : request.getSources();
        if (sources.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "请选择需要导入的联网来源");
        }
        Long regulationSetId = request.getRegulationSetId() == null ? ensureAutoImportSet() : ensureSet(request.getRegulationSetId());
        String setName = regulationSetName(regulationSetId);
        JwtUserPrincipal principal = SecurityUtils.currentPrincipal();
        HttpClient client = httpClient();
        List<WebSourceImportResponse.ImportResult> results = new ArrayList<>();
        int imported = 0;
        int skipped = 0;
        int failed = 0;

        for (WebSourceImportRequest.ImportSource source : sources.stream().limit(DISCOVERY_RESULT_LIMIT).toList()) {
            WebSourceImportResponse.ImportResult result = new WebSourceImportResponse.ImportResult();
            String url = normalizeUrl(source.getUrl());
            result.setUrl(url);
            try {
                if (url.isBlank()) {
                    throw new BizException(ErrorCode.BAD_REQUEST, "URL 为空");
                }
                WebDocument document = fetchDocument(client, url, true);
                result.setTitle(firstText(knownTitle(url), source.getTitle(), document.title(), url));
                if (!"FETCHED".equals(document.status()) || document.plainText().length() < 120) {
                    result.setStatus("FAILED");
                    result.setMessage(document.message());
                    failed++;
                    results.add(result);
                    continue;
                }
                String code = "WEB-" + sha256(url).substring(0, 16).toUpperCase(Locale.ROOT);
                Long existingId = existingRegulationId(regulationSetId, code);
                if (existingId != null) {
                    result.setRegulationId(existingId);
                    result.setStatus("SKIPPED");
                    result.setMessage("该来源已在目标规章集中导入");
                    skipped++;
                    results.add(result);
                    continue;
                }
                Long regulationId = insertRegulation(
                        regulationSetId,
                        firstText(knownTitle(url), source.getTitle(), document.title(), fileNameOf(document.uri())),
                        code,
                        validTypeCode(source.getTypeCode(), inferTypeCode(document.title(), document.plainText())),
                        firstText(source.getPublishDepartment(), inferDepartment(document.uri(), document.plainText())),
                        "联网导入：" + url,
                        documentContent(document),
                        principal.getUserId()
                );
                knowledgeRetrievalService.indexRegulation(regulationId);
                result.setRegulationId(regulationId);
                result.setStatus("IMPORTED");
                result.setMessage("已导入并完成 RAG 索引");
                imported++;
            } catch (BizException ex) {
                result.setStatus("FAILED");
                result.setMessage(ex.getMessage());
                failed++;
            } catch (Exception ex) {
                result.setStatus("FAILED");
                result.setMessage(truncate(ex.getMessage(), 160));
                failed++;
            }
            results.add(result);
        }

        WebSourceImportResponse response = new WebSourceImportResponse();
        response.setRegulationSetId(regulationSetId);
        response.setRegulationSetName(setName);
        response.setImportedCount(imported);
        response.setSkippedCount(skipped);
        response.setFailedCount(failed);
        response.setResults(results);
        return response;
    }

    private Long insertRegulation(
            Long regulationSetId,
            String title,
            String code,
            String typeCode,
            String publishDepartment,
            String applicableScope,
            String content,
            Long userId
    ) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                            INSERT INTO regulation (
                              regulation_set_id, title, code, type_code, publish_department,
                              applicable_scope, status, analysis_status, created_by, updated_by
                            )
                            VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE', 'NOT_ANALYZED', ?, ?)
                            """, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, regulationSetId);
            ps.setString(2, truncate(firstText(title, "联网导入制度"), 255));
            ps.setString(3, code);
            ps.setString(4, typeCode);
            ps.setString(5, truncate(blankToNull(publishDepartment), 128));
            ps.setString(6, truncate(applicableScope, 500));
            ps.setLong(7, userId);
            ps.setLong(8, userId);
            return ps;
        }, keyHolder);
        Long regulationId = keyHolder.getKey().longValue();
        String plainText = plainTextOf(content);
        jdbcTemplate.update("""
                        INSERT INTO regulation_content (regulation_id, content, plain_text, content_hash)
                        VALUES (?, ?, ?, ?)
                        """,
                regulationId,
                content,
                plainText,
                sha256(content)
        );
        return regulationId;
    }

    private Long ensureAutoImportSet() {
        Long existing = jdbcTemplate.query("""
                        SELECT id
                        FROM regulation_set
                        WHERE set_code = ? AND deleted = 0
                        """,
                rs -> rs.next() ? rs.getLong("id") : null,
                AUTO_SET_CODE
        );
        if (existing != null) {
            jdbcTemplate.update("UPDATE regulation_set SET enabled = 1 WHERE id = ?", existing);
            return existing;
        }
        JwtUserPrincipal principal = SecurityUtils.currentPrincipal();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                            INSERT INTO regulation_set (
                              set_name, set_code, description, enabled, is_default, created_by, updated_by
                            )
                            VALUES (?, ?, ?, 1, 0, ?, ?)
                            """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, AUTO_SET_NAME);
            ps.setString(2, AUTO_SET_CODE);
            ps.setString(3, "由联网发现功能导入的外部约束、上位制度、组织职责和可核验公开公文。");
            ps.setLong(4, principal.getUserId());
            ps.setLong(5, principal.getUserId());
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    private Long ensureSet(Long regulationSetId) {
        Long existing = jdbcTemplate.query("""
                        SELECT id
                        FROM regulation_set
                        WHERE id = ? AND enabled = 1 AND deleted = 0
                        """,
                rs -> rs.next() ? rs.getLong("id") : null,
                regulationSetId
        );
        if (existing == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "目标规章集不存在或未启用");
        }
        return existing;
    }

    private String regulationSetName(Long id) {
        return jdbcTemplate.query("""
                        SELECT set_name
                        FROM regulation_set
                        WHERE id = ?
                        """,
                rs -> rs.next() ? rs.getString("set_name") : "",
                id
        );
    }

    private Long existingRegulationId(Long regulationSetId, String code) {
        return jdbcTemplate.query("""
                        SELECT id
                        FROM regulation
                        WHERE regulation_set_id = ? AND code = ? AND deleted = 0
                        """,
                rs -> rs.next() ? rs.getLong("id") : null,
                regulationSetId,
                code
        );
    }

    private WebSourceDiscoveryResponse.WebSourceCandidate candidateOf(WebDocument document) {
        WebSourceDiscoveryResponse.WebSourceCandidate candidate = new WebSourceDiscoveryResponse.WebSourceCandidate();
        candidate.setUrl(document.url());
        candidate.setTitle(firstText(knownTitle(document.url()), document.title()));
        candidate.setDomain(document.uri().getHost());
        candidate.setOfficialSource(isOfficialSource(document.uri()));
        candidate.setSourceType(sourceType(document.uri()));
        candidate.setFetchStatus(document.status());
        candidate.setMessage(document.message());
        candidate.setSnippet(truncate(document.plainText(), 900));
        candidate.setContentLength(document.plainText() == null ? 0 : document.plainText().length());
        candidate.setSuggestedTypeCode(inferTypeCode(document.title(), document.plainText()));
        candidate.setSuggestedPublishDepartment(inferDepartment(document.uri(), document.plainText()));
        return candidate;
    }

    private List<String> buildQueries(WebSourceDiscoveryRequest request, String baseQuery) {
        List<String> queries = new ArrayList<>();
        addQuery(queries, baseQuery + " 法律 法规 规章 规定 官方");
        addQuery(queries, baseQuery + " 规范性文件 通告 办法 政策解读");
        addQuery(queries, baseQuery + " 实施办法 管理办法 暂行办法 实施细则");
        addQuery(queries, baseQuery + " 上位法 程序约束 信息公开 监督问责");
        addQuery(queries, baseQuery + " 机构职责 权责清单 组织架构");
        addQuery(queries, baseQuery + " 行政许可 公示 投诉举报 责任追究");
        String setName = regulationSetNameOrEmpty(request.getRegulationSetId());
        if (!setName.isBlank()) {
            addQuery(queries, setName + " 上位法 程序 监督 问责 信息公开");
            addQuery(queries, setName + " 行政许可 回避 留痕 复核 公示");
        }
        return queries.stream().limit(SEARCH_QUERY_LIMIT).toList();
    }

    private List<String> seedUrlsFor(String text) {
        String value = nullToEmpty(text);
        List<String> urls = new ArrayList<>();
        if (value.contains("烟花爆竹") || value.toLowerCase(Locale.ROOT).contains("firework")) {
            urls.add("https://www.gov.cn/gongbao/content/2006/content_219931.htm");
            urls.add("https://www.mem.gov.cn/gk/gwgg/agwzlfl/zjl_01/201310/t20131023_233708.shtml");
            urls.add("https://www.mem.gov.cn/gk/gwgg/agwzlfl/zjl_01/201801/t20180124_233685.shtml");
        }
        if (value.contains("科研") || value.contains("学术诚信") || value.contains("基金")) {
            urls.add("https://www.gov.cn/zhengce/2018-05/30/content_5294886.htm");
            urls.add("https://www.most.gov.cn/xxgk/xinxifenlei/fdzdgknr/fgzc/gfxwj/gfxwj2022/202209/t20220907_182313.html");
            urls.add("https://www.most.gov.cn/xxgk/xinxifenlei/fdzdgknr/fgzc/bmgz/202602/t20260211_195895.html");
            urls.add("https://www.moe.gov.cn/srcsite/A02/s5911/moe_621/201607/t20160718_272156.html");
            urls.add("https://www.gov.cn/zhengce/2012-11/13/content_5713384.htm");
        }
        return urls;
    }

    private String knownTitle(String url) {
        return switch (normalizeUrl(url)) {
            case "https://www.gov.cn/gongbao/content/2006/content_219931.htm" -> "烟花爆竹安全管理条例";
            case "https://www.mem.gov.cn/gk/gwgg/agwzlfl/zjl_01/201310/t20131023_233708.shtml" -> "烟花爆竹经营许可实施办法";
            case "https://www.mem.gov.cn/gk/gwgg/agwzlfl/zjl_01/201801/t20180124_233685.shtml" -> "烟花爆竹生产经营安全规定";
            case "https://www.gov.cn/zhengce/2018-05/30/content_5294886.htm" -> "国务院关于优化科研管理提升科研绩效若干措施的通知";
            case "https://www.most.gov.cn/xxgk/xinxifenlei/fdzdgknr/fgzc/gfxwj/gfxwj2022/202209/t20220907_182313.html" -> "科学技术活动违规行为处理暂行规定";
            case "https://www.most.gov.cn/xxgk/xinxifenlei/fdzdgknr/fgzc/bmgz/202602/t20260211_195895.html" -> "国家科技计划项目管理暂行办法";
            case "https://www.moe.gov.cn/srcsite/A02/s5911/moe_621/201607/t20160718_272156.html" -> "高等学校预防与处理学术不端行为办法";
            case "https://www.gov.cn/zhengce/2012-11/13/content_5713384.htm" -> "学位论文作假行为处理办法";
            default -> "";
        };
    }

    private String baseQuery(WebSourceDiscoveryRequest request) {
        String direct = normalizeWhitespace(request.getQuery());
        if (!direct.isBlank()) {
            return direct;
        }
        String title = normalizeWhitespace(request.getCaseTitle());
        if (!title.isBlank()) {
            return title;
        }
        String description = normalizeWhitespace(request.getCaseDescription());
        if (description.isBlank()) {
            boolean hasManualUrl = request.getSourceUrls() != null && request.getSourceUrls().stream().anyMatch(url -> !normalizeUrl(url).isBlank());
            if (hasManualUrl) {
                return "用户提供法规 URL 核验";
            }
            throw new BizException(ErrorCode.BAD_REQUEST, "请输入案例标题、案例事实或检索词");
        }
        return truncate(description, 80);
    }

    private String regulationSetNameOrEmpty(Long regulationSetId) {
        if (regulationSetId == null) {
            return "";
        }
        return jdbcTemplate.query("""
                        SELECT set_name
                        FROM regulation_set
                        WHERE id = ? AND deleted = 0
                        """,
                rs -> rs.next() ? rs.getString("set_name") : "",
                regulationSetId
        );
    }

    private void addQuery(List<String> queries, String query) {
        String normalized = normalizeWhitespace(query);
        if (!normalized.isBlank() && !queries.contains(normalized)) {
            queries.add(normalized);
        }
    }

    private List<String> searchOfficialUrls(HttpClient client, String query) {
        String encoded = URLEncoder.encode(query + " site:gov.cn", StandardCharsets.UTF_8);
        List<String> urls = new ArrayList<>();
        fetchSearchPage(client, "https://www.bing.com/search?format=rss&q=" + encoded + "&count=20&mkt=zh-CN", urls);
        fetchSearchPage(client, "https://www.bing.com/search?q=" + encoded + "&count=20&mkt=zh-CN", urls);
        fetchSearchPage(client, "https://duckduckgo.com/html/?q=" + encoded, urls);
        return urls;
    }

    private void fetchSearchPage(HttpClient client, String searchUrl, List<String> urls) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(searchUrl))
                    .timeout(Duration.ofSeconds(8))
                    .header("User-Agent", "Mozilla/5.0 rent-seeking-analysis/0.1")
                    .GET()
                    .build();
            HttpResponse<String> response = sendString(client, request);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return;
            }
            String body = response.body() == null ? "" : response.body();
            Matcher rssMatcher = RSS_LINK_PATTERN.matcher(body);
            while (rssMatcher.find() && urls.size() < 60) {
                String resolved = resolveSearchHref(rssMatcher.group(1));
                if (!resolved.isBlank() && !urls.contains(resolved)) {
                    urls.add(resolved);
                }
            }
            Matcher matcher = HREF_PATTERN.matcher(body);
            while (matcher.find() && urls.size() < 60) {
                String resolved = resolveSearchHref(matcher.group(1));
                if (!resolved.isBlank() && !urls.contains(resolved)) {
                    urls.add(resolved);
                }
            }
        } catch (Exception ignored) {
            // Search is best-effort; the UI will show empty candidates if providers are unavailable.
        }
    }

    private String resolveSearchHref(String href) {
        String value = decodeHtml(href);
        try {
            if (value.startsWith("//")) {
                value = "https:" + value;
            }
            if (value.startsWith("/")) {
                if (value.startsWith("/l/") && value.contains("uddg=")) {
                    return queryParam(value, "uddg");
                }
                return "";
            }
            URI uri = URI.create(value);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            if (host.contains("bing.com") && uri.getQuery() != null && uri.getQuery().contains("u=")) {
                String encoded = queryParam(uri.getRawQuery(), "u");
                String decoded = decodeBingUrl(encoded);
                return decoded.isBlank() ? "" : decoded;
            }
            return value;
        } catch (Exception ignored) {
            return "";
        }
    }

    private String queryParam(String queryOrPath, String name) {
        String query = queryOrPath == null ? "" : queryOrPath;
        int question = query.indexOf('?');
        if (question >= 0) {
            query = query.substring(question + 1);
        }
        for (String part : query.split("&")) {
            int index = part.indexOf('=');
            if (index <= 0) {
                continue;
            }
            if (part.substring(0, index).equals(name)) {
                return URLDecoder.decode(part.substring(index + 1), StandardCharsets.UTF_8);
            }
        }
        return "";
    }

    private String decodeBingUrl(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.startsWith("a1") ? value.substring(2) : value;
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(normalized);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            try {
                return new String(Base64.getDecoder().decode(normalized), StandardCharsets.UTF_8);
            } catch (IllegalArgumentException ignored) {
                return "";
            }
        }
    }

    private WebDocument fetchDocument(HttpClient client, String url, boolean forImport) {
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException ex) {
            return new WebDocument(url, URI.create("https://invalid.local"), "", "FAILED", "URL 格式无效", "");
        }
        WebDocument invalid = validateUri(uri);
        if (invalid != null) {
            return invalid;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(forImport ? 14 : 10))
                    .header("User-Agent", "Mozilla/5.0 rent-seeking-analysis/0.1")
                    .GET()
                    .build();
            HttpResponse<String> response = sendString(client, request);
            int statusCode = response.statusCode();
            if (statusCode < 200 || statusCode >= 300) {
                return new WebDocument(url, uri, fileNameOf(uri), "FAILED", "HTTP " + statusCode, "");
            }
            String contentType = response.headers().firstValue("content-type").orElse("").toLowerCase(Locale.ROOT);
            if (contentType.contains("pdf") || uri.getPath().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
                return new WebDocument(url, uri, fileNameOf(uri), "NEED_MANUAL_IMPORT", "识别为 PDF，当前不解析 PDF 正文，请下载后手动导入或粘贴正文。", "");
            }
            String html = response.body() == null ? "" : response.body();
            String title = firstText(extractMetaTitle(html), extractHtmlTitle(html), fileNameOf(uri));
            String text = plainTextOf(html);
            if (text.length() > IMPORT_CONTENT_LIMIT) {
                text = text.substring(0, IMPORT_CONTENT_LIMIT);
            }
            String status = text.isBlank() ? "NEED_MANUAL_IMPORT" : "FETCHED";
            String message = isOfficialSource(uri)
                    ? "已抓取官方公开来源正文"
                    : "已抓取公开来源，域名需人工核验权威性";
            return new WebDocument(url, uri, title, status, message, text);
        } catch (Exception ex) {
            return new WebDocument(url, uri, fileNameOf(uri), "FAILED", truncate(ex.getMessage(), 160), "");
        }
    }

    private WebDocument validateUri(URI uri) {
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!Set.of("http", "https").contains(scheme)) {
            return new WebDocument(uri.toString(), uri, "", "FAILED", "仅支持 http/https 链接", "");
        }
        return null;
    }

    private boolean isAllowedCandidate(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            if (host.isBlank() || host.contains("bing.com") || host.contains("duckduckgo.com") || host.contains("baidu.com")) {
                return false;
            }
            String path = uri.getPath() == null ? "" : uri.getPath().toLowerCase(Locale.ROOT);
            if (path.endsWith(".jpg") || path.endsWith(".png") || path.endsWith(".gif") || path.endsWith(".zip") || path.endsWith(".rar")) {
                return false;
            }
            return isOfficialSource(uri) || host.endsWith(".edu.cn");
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean isOfficialSource(URI uri) {
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        return host.endsWith(".gov.cn")
                || host.equals("gov.cn")
                || host.endsWith(".npc.gov.cn")
                || host.endsWith(".court.gov.cn");
    }

    private String sourceType(URI uri) {
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        if (isOfficialSource(uri)) {
            return "官方政府/法律来源";
        }
        if (host.endsWith(".edu.cn")) {
            return "高校/科研机构来源";
        }
        return "公开来源需核验";
    }

    private String inferTypeCode(String title, String text) {
        String value = nullToEmpty(title) + "\n" + nullToEmpty(text);
        if (value.contains("权责清单") || value.contains("机构职能") || value.contains("监督") || value.contains("问责")) {
            return "SUPERVISION";
        }
        if (value.contains("许可") || value.contains("审批") || value.contains("办事指南")) {
            return "APPROVAL";
        }
        if (value.contains("流程") || value.contains("程序") || value.contains("实施细则")) {
            return "PROCEDURE";
        }
        return "POLICY";
    }

    private String validTypeCode(String value, String fallback) {
        String typeCode = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        return Set.of("POLICY", "PROCEDURE", "APPROVAL", "SUPERVISION").contains(typeCode) ? typeCode : fallback;
    }

    private String inferDepartment(URI uri, String text) {
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        if (host.contains("npc.gov.cn")) {
            return "全国人大及其常委会";
        }
        if (host.contains("gov.cn")) {
            Matcher sourceMatcher = Pattern.compile("来源[:：]\\s*([^\\s　]{2,40})").matcher(text == null ? "" : text);
            if (sourceMatcher.find()) {
                return sourceMatcher.group(1);
            }
            if (host.contains("most.gov.cn")) {
                return "科学技术部";
            }
            if (host.contains("moe.gov.cn")) {
                return "教育部";
            }
            if (host.contains("mem.gov.cn")) {
                return "应急管理部";
            }
            if (host.contains("samr.gov.cn")) {
                return "市场监管总局";
            }
            if (host.contains("www.gov.cn")) {
                return "中国政府网";
            }
            return host;
        }
        if (host.endsWith(".edu.cn")) {
            return "高校或科研机构";
        }
        return host;
    }

    private String documentContent(WebDocument document) {
        StringBuilder builder = new StringBuilder();
        builder.append("来源URL：").append(document.url()).append("\n");
        builder.append("来源域名：").append(document.uri().getHost()).append("\n");
        builder.append("抓取状态：").append(document.status()).append("\n");
        builder.append("抓取说明：").append(document.message()).append("\n\n");
        builder.append(document.plainText());
        return builder.toString().trim();
    }

    private String extractMetaTitle(String html) {
        Matcher matcher = Pattern.compile("(?is)<meta[^>]+(?:name|property)=[\"'](?:title|og:title)[\"'][^>]+content=[\"']([^\"']+)[\"']").matcher(nullToEmpty(html));
        return matcher.find() ? cleanupText(matcher.group(1)) : "";
    }

    private String extractHtmlTitle(String html) {
        Matcher matcher = Pattern.compile("(?is)<title[^>]*>(.*?)</title>").matcher(nullToEmpty(html));
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
                .replaceAll("(?is)</div>", "\n")
                .replaceAll("(?is)</li>", "\n")
                .replaceAll("(?is)<[^>]+>", " ");
        return cleanupText(text);
    }

    private String cleanupText(String value) {
        return decodeHtml(value)
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String decodeHtml(String value) {
        return nullToEmpty(value)
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replaceAll("&#\\d+;", " ");
    }

    private String normalizeUrl(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        String value = decodeHtml(url.trim());
        int hash = value.indexOf('#');
        if (hash >= 0) {
            value = value.substring(0, hash);
        }
        return value;
    }

    private String normalizeWhitespace(String value) {
        return nullToEmpty(value).replaceAll("\\s+", " ").trim();
    }

    private String fileNameOf(URI uri) {
        String path = uri.getPath();
        if (path == null || path.isBlank()) {
            return uri.toString();
        }
        int index = path.lastIndexOf('/');
        return index >= 0 && index < path.length() - 1 ? path.substring(index + 1) : path;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String text = value.trim();
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(nullToEmpty(value).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private HttpClient httpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(6))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
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
                .connectTimeout(Duration.ofSeconds(6))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    private record WebDocument(
            String url,
            URI uri,
            String title,
            String status,
            String message,
            String plainText
    ) {
    }
}
