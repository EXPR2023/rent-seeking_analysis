SET NAMES utf8mb4;
SET time_zone = '+08:00';
USE rent_seeking_analysis;

INSERT INTO sys_config (config_key, config_value, description, updated_by)
VALUES
  ('conflict.similarity.threshold', '0.75', '文本相似度冲突判定阈值', NULL),
  ('dashboard.cache.seconds', '60', '看板缓存秒数', NULL),
  ('security.token.expire.minutes', '120', 'Token 有效期分钟数', NULL),
  ('system.timezone', 'Asia/Singapore', '系统默认时区', NULL),
  ('demo.password.notice', '演示账号密码仅限本地开发环境使用', '演示密码安全提示', NULL)
ON DUPLICATE KEY UPDATE
  config_value = VALUES(config_value),
  description = VALUES(description),
  updated_by = VALUES(updated_by),
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_config (config_key, config_value, description, updated_by)
VALUES
  ('ai.enabled', 'true', '是否启用 LLM 寻租分析', NULL),
  ('ai.provider', 'openai-compatible', 'LLM 服务提供方', NULL),
  ('ai.base-url', '', 'OpenAI-compatible Base URL，例如 https://api.openai.com/v1', NULL),
  ('ai.api-key', '', 'LLM API Key，页面不明文展示', NULL),
  ('ai.model', '', 'LLM 模型名称', NULL),
  ('ai.timeout', '30s', 'LLM 调用超时时间', NULL)
ON DUPLICATE KEY UPDATE
  description = VALUES(description),
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_config (config_key, config_value, description, updated_by)
VALUES
  ('embedding.provider', 'local', 'Embedding 提供方：local 或 openai-compatible', NULL),
  ('embedding.base-url', '', 'OpenAI-compatible Embedding Base URL，例如 https://api.openai.com/v1', NULL),
  ('embedding.api-key', '', 'Embedding API Key，页面不明文展示', NULL),
  ('embedding.model', 'local-bigram-v1', 'Embedding 模型名称', NULL),
  ('embedding.dimension', '384', '本地向量维度，外部模型可留作记录', NULL),
  ('embedding.timeout', '30s', 'Embedding 调用超时时间', NULL),
  ('embedding.input.max-chars', '12000', '单次 Embedding 输入最大字符数，防止超出模型上下文', NULL),
  ('rag.enabled', 'true', '是否启用 RAG 检索增强', NULL),
  ('rag.top-k', '8', 'RAG 检索返回证据数量', NULL),
  ('rag.similarity.threshold', '0.10', 'RAG 向量余弦相似度阈值', NULL),
  ('rag.chunk-size', '720', '知识切片目标长度', NULL),
  ('rag.auto-index-missing', 'true', '分析时自动补齐未索引制度片段', NULL)
ON DUPLICATE KEY UPDATE
  config_value = CASE
    WHEN VALUES(config_key) LIKE 'embedding.%' THEN sys_config.config_value
    ELSE VALUES(config_value)
  END,
  description = VALUES(description),
  updated_by = VALUES(updated_by),
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO ai_prompt_template (scene_code, template_name, prompt_content, enabled)
VALUES
  ('REGULATION_SUMMARY', '制度摘要提示词', '请基于制度标题、适用范围和正文生成不超过300字的制度摘要，并返回JSON字段summary。', 1),
  ('CONFLICT_EXPLANATION', '冲突说明提示词', '请对两段制度条款的潜在冲突进行简明解释，并返回JSON字段conflictExplanation。', 1),
  ('RENT_SEEKING_RISK', '寻租风险识别提示词', '请基于正文条款、本地规则线索和RAG证据识别制度中的具体寻租风险。风险项必须说明权力节点、经营者处境、可交换利益、机会成本差、证据引用和可执行整改动作，输出riskLevel、riskScore、riskItems和overallSuggestion。', 1),
  ('RECTIFICATION_SUGGESTION', '整改建议提示词', '请针对制度风险点给出可执行的整改建议，并保持结构化JSON输出。', 1),
  ('REPORT_GENERATION', '报告生成提示词', '请聚合制度摘要、冲突结果和风险分析结果，生成报告正文快照。', 1),
  ('REGULATION_QA', '制度问答提示词', '请仅基于给定制度正文回答用户问题，无法判断时说明依据不足。', 1),
  ('CASE_RENT_SEEKING_ANALYSIS', '案例扩展寻租分析提示词', '请围绕真实案例自动扩展相关法律公文、地方政策、标准、事故材料和组织责任链，识别具体寻租风险。风险点必须写清权力节点、经营者处境、可交换利益、制度原因、证据依据和可执行整改动作；证据不足时列出需要手动导入的官方材料。', 1)
ON DUPLICATE KEY UPDATE
  template_name = VALUES(template_name),
  prompt_content = VALUES(prompt_content),
  enabled = VALUES(enabled),
  updated_at = CURRENT_TIMESTAMP;
