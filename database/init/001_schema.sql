SET NAMES utf8mb4;
SET time_zone = '+08:00';

CREATE DATABASE IF NOT EXISTS rent_seeking_analysis
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE rent_seeking_analysis;

CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
  username VARCHAR(64) NOT NULL COMMENT '登录账号',
  password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希',
  real_name VARCHAR(64) NOT NULL COMMENT '姓名',
  email VARCHAR(128) NULL COMMENT '邮箱',
  phone VARCHAR(32) NULL COMMENT '手机号',
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '用户状态',
  last_login_at DATETIME NULL COMMENT '最后登录时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  UNIQUE KEY uk_sys_user_username (username),
  KEY idx_sys_user_status (status),
  KEY idx_sys_user_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';

CREATE TABLE IF NOT EXISTS sys_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID',
  role_code VARCHAR(64) NOT NULL COMMENT '角色编码',
  role_name VARCHAR(64) NOT NULL COMMENT '角色名称',
  description VARCHAR(255) NULL COMMENT '说明',
  enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_sys_role_code (role_code),
  KEY idx_sys_role_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色表';

CREATE TABLE IF NOT EXISTS sys_user_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  role_id BIGINT NOT NULL COMMENT '角色ID',
  UNIQUE KEY uk_sys_user_role (user_id, role_id),
  KEY idx_sys_user_role_user_id (user_id),
  KEY idx_sys_user_role_role_id (role_id),
  CONSTRAINT fk_sys_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
  CONSTRAINT fk_sys_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户角色关联表';

CREATE TABLE IF NOT EXISTS sys_menu (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '菜单ID',
  parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父菜单ID',
  menu_name VARCHAR(64) NOT NULL COMMENT '菜单名称',
  menu_code VARCHAR(64) NOT NULL COMMENT '菜单编码',
  path VARCHAR(128) NULL COMMENT '前端路由',
  component VARCHAR(128) NULL COMMENT '组件路径',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
  visible TINYINT NOT NULL DEFAULT 1 COMMENT '是否显示',
  UNIQUE KEY uk_sys_menu_code (menu_code),
  KEY idx_sys_menu_parent (parent_id),
  KEY idx_sys_menu_sort (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜单表';

CREATE TABLE IF NOT EXISTS sys_role_menu (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  role_id BIGINT NOT NULL COMMENT '角色ID',
  menu_id BIGINT NOT NULL COMMENT '菜单ID',
  UNIQUE KEY uk_sys_role_menu (role_id, menu_id),
  KEY idx_sys_role_menu_role_id (role_id),
  KEY idx_sys_role_menu_menu_id (menu_id),
  CONSTRAINT fk_sys_role_menu_role FOREIGN KEY (role_id) REFERENCES sys_role (id),
  CONSTRAINT fk_sys_role_menu_menu FOREIGN KEY (menu_id) REFERENCES sys_menu (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色菜单关联表';

CREATE TABLE IF NOT EXISTS regulation_set (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '规章集ID',
  set_name VARCHAR(128) NOT NULL COMMENT '规章集名称',
  set_code VARCHAR(64) NOT NULL COMMENT '规章集编码',
  description VARCHAR(500) NULL COMMENT '说明',
  enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
  is_default TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认集合',
  created_by BIGINT NULL COMMENT '创建人',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_by BIGINT NULL COMMENT '更新人',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  UNIQUE KEY uk_regulation_set_code (set_code),
  KEY idx_regulation_set_enabled (enabled),
  KEY idx_regulation_set_default (is_default),
  KEY idx_regulation_set_deleted (deleted),
  CONSTRAINT fk_regulation_set_created_by FOREIGN KEY (created_by) REFERENCES sys_user (id),
  CONSTRAINT fk_regulation_set_updated_by FOREIGN KEY (updated_by) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='规章集表';

CREATE TABLE IF NOT EXISTS regulation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '制度ID',
  regulation_set_id BIGINT NOT NULL DEFAULT 1 COMMENT '规章集ID',
  title VARCHAR(255) NOT NULL COMMENT '制度标题',
  code VARCHAR(128) NULL COMMENT '制度编号',
  type_code VARCHAR(64) NOT NULL COMMENT '制度类型',
  publish_department VARCHAR(128) NULL COMMENT '发布部门',
  effective_date DATE NULL COMMENT '生效日期',
  expiry_date DATE NULL COMMENT '失效日期',
  applicable_scope VARCHAR(500) NULL COMMENT '适用范围',
  status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '制度状态',
  analysis_status VARCHAR(32) NOT NULL DEFAULT 'NOT_ANALYZED' COMMENT '风险分析状态',
  created_by BIGINT NOT NULL COMMENT '创建人',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_by BIGINT NULL COMMENT '更新人',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  UNIQUE KEY uk_regulation_set_code (regulation_set_id, code),
  KEY idx_regulation_set_id (regulation_set_id),
  KEY idx_regulation_title (title),
  KEY idx_regulation_type_status (type_code, status),
  KEY idx_regulation_analysis_status (analysis_status),
  KEY idx_regulation_deleted (deleted),
  CONSTRAINT fk_regulation_set FOREIGN KEY (regulation_set_id) REFERENCES regulation_set (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='制度主表';

CREATE TABLE IF NOT EXISTS regulation_content (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  regulation_id BIGINT NOT NULL COMMENT '制度ID',
  content LONGTEXT NOT NULL COMMENT '制度正文',
  plain_text LONGTEXT NULL COMMENT '纯文本内容',
  content_hash VARCHAR(128) NULL COMMENT '内容哈希',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_regulation_content_regulation_id (regulation_id),
  KEY idx_regulation_content_hash (content_hash),
  CONSTRAINT fk_regulation_content_regulation FOREIGN KEY (regulation_id) REFERENCES regulation (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='制度正文表';

CREATE TABLE IF NOT EXISTS conflict_detection_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务ID',
  main_regulation_id BIGINT NOT NULL COMMENT '主制度ID',
  compare_regulation_ids VARCHAR(500) NOT NULL COMMENT '对比制度ID列表',
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '任务状态',
  total_items INT NOT NULL DEFAULT 0 COMMENT '冲突项数量',
  created_by BIGINT NOT NULL COMMENT '创建人',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  finished_at DATETIME NULL COMMENT '完成时间',
  KEY idx_conflict_task_main_regulation (main_regulation_id),
  KEY idx_conflict_task_status (status),
  KEY idx_conflict_task_created_at (created_at),
  CONSTRAINT fk_conflict_task_main_regulation FOREIGN KEY (main_regulation_id) REFERENCES regulation (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='冲突检测任务表';

CREATE TABLE IF NOT EXISTS conflict_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '冲突项ID',
  task_id BIGINT NOT NULL COMMENT '检测任务ID',
  main_regulation_id BIGINT NOT NULL COMMENT '主制度ID',
  compare_regulation_id BIGINT NOT NULL COMMENT '对比制度ID',
  conflict_type VARCHAR(64) NOT NULL COMMENT '冲突类型',
  conflict_level VARCHAR(32) NOT NULL COMMENT '冲突等级',
  main_clause TEXT NULL COMMENT '主制度条款',
  compare_clause TEXT NULL COMMENT '对比制度条款',
  similarity DECIMAL(5,4) NULL COMMENT '相似度',
  explanation TEXT NULL COMMENT '冲突说明',
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '冲突状态',
  review_comment VARCHAR(500) NULL COMMENT '复核备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY idx_conflict_item_task_id (task_id),
  KEY idx_conflict_item_main_compare (main_regulation_id, compare_regulation_id),
  KEY idx_conflict_item_type_level (conflict_type, conflict_level),
  KEY idx_conflict_item_status (status),
  CONSTRAINT fk_conflict_item_task FOREIGN KEY (task_id) REFERENCES conflict_detection_task (id),
  CONSTRAINT fk_conflict_item_main_regulation FOREIGN KEY (main_regulation_id) REFERENCES regulation (id),
  CONSTRAINT fk_conflict_item_compare_regulation FOREIGN KEY (compare_regulation_id) REFERENCES regulation (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='冲突明细表';

CREATE TABLE IF NOT EXISTS risk_analysis (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '分析ID',
  regulation_id BIGINT NOT NULL COMMENT '制度ID',
  summary TEXT NULL COMMENT '制度摘要',
  risk_level VARCHAR(32) NOT NULL COMMENT '风险等级',
  risk_score INT NOT NULL COMMENT '风险分数',
  analysis_mode VARCHAR(32) NOT NULL DEFAULT 'RULE_ONLY' COMMENT '分析模式',
  model_name VARCHAR(128) NULL COMMENT '模型名称',
  prompt_version VARCHAR(64) NULL COMMENT '提示词版本',
  confidence DECIMAL(5,4) NULL COMMENT '综合置信度',
  evidence_count INT NOT NULL DEFAULT 0 COMMENT '证据数量',
  overall_suggestion TEXT NULL COMMENT '总体建议',
  review_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '复核状态',
  review_comment VARCHAR(500) NULL COMMENT '复核备注',
  created_by BIGINT NOT NULL COMMENT '创建人',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY idx_risk_analysis_regulation_id (regulation_id),
  KEY idx_risk_analysis_level (risk_level),
  KEY idx_risk_analysis_mode (analysis_mode),
  KEY idx_risk_analysis_review_status (review_status),
  KEY idx_risk_analysis_created_at (created_at),
  CONSTRAINT fk_risk_analysis_regulation FOREIGN KEY (regulation_id) REFERENCES regulation (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='风险分析结果表';

CREATE TABLE IF NOT EXISTS risk_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '风险项ID',
  analysis_id BIGINT NOT NULL COMMENT '分析ID',
  indicator_code VARCHAR(64) NULL COMMENT '风险指标编码',
  title VARCHAR(255) NOT NULL COMMENT '风险标题',
  description TEXT NULL COMMENT '风险描述',
  reason TEXT NULL COMMENT '风险原因',
  suggestion TEXT NULL COMMENT '整改建议',
  related_clause TEXT NULL COMMENT '相关条款',
  risk_level VARCHAR(32) NOT NULL COMMENT '风险等级',
  source_trace VARCHAR(64) NOT NULL DEFAULT 'RULE' COMMENT '来源轨迹',
  evidence_count INT NOT NULL DEFAULT 0 COMMENT '证据数量',
  rectification_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '整改状态',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
  KEY idx_risk_item_analysis_id (analysis_id),
  KEY idx_risk_item_indicator (indicator_code),
  KEY idx_risk_item_level (risk_level),
  KEY idx_risk_item_source (source_trace),
  KEY idx_risk_item_rectification (rectification_status),
  CONSTRAINT fk_risk_item_analysis FOREIGN KEY (analysis_id) REFERENCES risk_analysis (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='风险点明细表';

CREATE TABLE IF NOT EXISTS risk_evidence (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '证据ID',
  analysis_id BIGINT NOT NULL COMMENT '分析ID',
  risk_item_id BIGINT NULL COMMENT '风险项ID',
  evidence_type VARCHAR(64) NOT NULL COMMENT '证据类型',
  source_type VARCHAR(64) NOT NULL COMMENT '来源类型',
  source_id BIGINT NULL COMMENT '来源ID',
  title VARCHAR(255) NOT NULL COMMENT '证据标题',
  snippet TEXT NOT NULL COMMENT '证据片段',
  similarity DECIMAL(5,4) NULL COMMENT '相似度',
  citation_no INT NOT NULL DEFAULT 0 COMMENT '引用编号',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_risk_evidence_analysis (analysis_id),
  KEY idx_risk_evidence_item (risk_item_id),
  KEY idx_risk_evidence_source (source_type, source_id),
  CONSTRAINT fk_risk_evidence_analysis FOREIGN KEY (analysis_id) REFERENCES risk_analysis (id),
  CONSTRAINT fk_risk_evidence_item FOREIGN KEY (risk_item_id) REFERENCES risk_item (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='风险分析证据表';

CREATE TABLE IF NOT EXISTS knowledge_chunk (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '知识片段ID',
  source_type VARCHAR(64) NOT NULL COMMENT '来源类型',
  source_id BIGINT NOT NULL COMMENT '来源ID',
  title VARCHAR(255) NOT NULL COMMENT '标题',
  chunk_text TEXT NOT NULL COMMENT '片段文本',
  chunk_order INT NOT NULL DEFAULT 0 COMMENT '片段序号',
  content_hash VARCHAR(128) NULL COMMENT '内容哈希',
  embedding_model VARCHAR(128) NULL COMMENT 'Embedding模型',
  embedding_dimension INT NULL COMMENT '向量维度',
  embedding_vector LONGTEXT NULL COMMENT 'Embedding向量JSON',
  indexed_at DATETIME NULL COMMENT '索引时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY uk_knowledge_chunk_source_order (source_type, source_id, chunk_order),
  KEY idx_knowledge_chunk_source (source_type, source_id),
  KEY idx_knowledge_chunk_hash (content_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='RAG知识片段表';

CREATE TABLE IF NOT EXISTS ai_prompt_template (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '模板ID',
  scene_code VARCHAR(64) NOT NULL COMMENT '场景编码',
  template_name VARCHAR(128) NOT NULL COMMENT '模板名称',
  prompt_content TEXT NOT NULL COMMENT '提示词内容',
  enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_ai_prompt_scene_code (scene_code),
  KEY idx_ai_prompt_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI提示词模板表';

CREATE TABLE IF NOT EXISTS ai_analysis_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
  scene_code VARCHAR(64) NOT NULL COMMENT '分析场景',
  business_type VARCHAR(64) NOT NULL COMMENT '业务类型',
  business_id BIGINT NULL COMMENT '业务ID',
  model_name VARCHAR(128) NULL COMMENT '模型名称',
  prompt LONGTEXT NULL COMMENT '请求提示词',
  response_text LONGTEXT NULL COMMENT '原始响应',
  parsed_json LONGTEXT NULL COMMENT '解析后JSON',
  rag_context LONGTEXT NULL COMMENT 'RAG引用上下文',
  token_usage VARCHAR(255) NULL COMMENT 'Token用量摘要',
  status VARCHAR(32) NOT NULL COMMENT '调用状态',
  error_message TEXT NULL COMMENT '错误信息',
  created_by BIGINT NULL COMMENT '调用人',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '调用时间',
  KEY idx_ai_record_scene (scene_code),
  KEY idx_ai_record_business (business_type, business_id),
  KEY idx_ai_record_status (status),
  KEY idx_ai_record_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI分析记录表';

CREATE TABLE IF NOT EXISTS report_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '报告ID',
  report_name VARCHAR(255) NOT NULL COMMENT '报告名称',
  report_type VARCHAR(64) NOT NULL COMMENT '报告类型',
  regulation_id BIGINT NULL COMMENT '制度ID',
  content_snapshot LONGTEXT NULL COMMENT '报告内容快照',
  created_by BIGINT NOT NULL COMMENT '创建人',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_report_record_type (report_type),
  KEY idx_report_record_regulation (regulation_id),
  KEY idx_report_record_created_at (created_at),
  CONSTRAINT fk_report_record_regulation FOREIGN KEY (regulation_id) REFERENCES regulation (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='报告记录表';

CREATE TABLE IF NOT EXISTS sys_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '参数ID',
  config_key VARCHAR(128) NOT NULL COMMENT '参数键',
  config_value VARCHAR(1000) NULL COMMENT '参数值',
  description VARCHAR(255) NULL COMMENT '说明',
  updated_by BIGINT NULL COMMENT '更新人',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_sys_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统参数表';

CREATE TABLE IF NOT EXISTS sys_dict (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '字典ID',
  dict_type VARCHAR(64) NOT NULL COMMENT '字典类型',
  item_code VARCHAR(64) NOT NULL COMMENT '字典编码',
  item_name VARCHAR(128) NOT NULL COMMENT '字典名称',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
  enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
  UNIQUE KEY uk_sys_dict_type_code (dict_type, item_code),
  KEY idx_sys_dict_type (dict_type),
  KEY idx_sys_dict_enabled (enabled),
  KEY idx_sys_dict_sort (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统字典表';

CREATE TABLE IF NOT EXISTS operation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
  user_id BIGINT NULL COMMENT '操作用户ID',
  username VARCHAR(64) NULL COMMENT '操作账号',
  module_name VARCHAR(64) NOT NULL COMMENT '模块名称',
  operation_type VARCHAR(64) NOT NULL COMMENT '操作类型',
  business_id BIGINT NULL COMMENT '业务ID',
  request_method VARCHAR(16) NULL COMMENT 'HTTP方法',
  request_uri VARCHAR(255) NULL COMMENT '请求URI',
  request_params TEXT NULL COMMENT '请求参数摘要',
  result_code VARCHAR(64) NULL COMMENT '结果编码',
  ip_address VARCHAR(64) NULL COMMENT 'IP地址',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  KEY idx_operation_log_user_id (user_id),
  KEY idx_operation_log_module_type (module_name, operation_type),
  KEY idx_operation_log_business (business_id),
  KEY idx_operation_log_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作日志表';
