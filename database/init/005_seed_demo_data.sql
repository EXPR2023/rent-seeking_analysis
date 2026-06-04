SET NAMES utf8mb4;
SET time_zone = '+08:00';
USE rent_seeking_analysis;

INSERT INTO sys_user (username, password_hash, real_name, email, phone, status)
VALUES
  ('admin', '$2b$10$sADP5dnP9/TtbTeT4dEbZu7yh0WkMaRb3AMBtiarjolcnyNibPsku', '系统管理员', 'admin@example.local', '13800000001', 'ENABLED'),
  ('regulation', '$2b$10$WgYlYb1SkEbtJ5eA7fY14.Av7JNiyEhHrKZNBTN/2lVI21U/4n0fy', '制度管理员', 'regulation@example.local', '13800000002', 'ENABLED'),
  ('analyst', '$2b$10$0iXpDiBA08aL34tUAWfsLeOrcvs984h58NVpafmHAw312ezeL0yN2', '分析人员', 'analyst@example.local', '13800000003', 'ENABLED'),
  ('viewer', '$2b$10$n3k3kn5I9ZknKQVlmAFHP.9n1CWU.9iLfOYvL7.an6PtQzY68LOu6', '只读用户', 'viewer@example.local', '13800000004', 'ENABLED')
ON DUPLICATE KEY UPDATE
  password_hash = VALUES(password_hash),
  real_name = VALUES(real_name),
  email = VALUES(email),
  phone = VALUES(phone),
  status = VALUES(status),
  deleted = 0,
  updated_at = CURRENT_TIMESTAMP;

INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u
JOIN sys_role r ON r.role_code = 'ADMIN'
WHERE u.username = 'admin';

INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u
JOIN sys_role r ON r.role_code = 'REGULATION_MANAGER'
WHERE u.username = 'regulation';

INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u
JOIN sys_role r ON r.role_code = 'ANALYST'
WHERE u.username = 'analyst';

INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u
JOIN sys_role r ON r.role_code = 'VIEWER'
WHERE u.username = 'viewer';

INSERT INTO sys_menu (parent_id, menu_name, menu_code, path, component, sort_order, visible)
VALUES
  (0, '工作台', 'DASHBOARD', '/dashboard', 'views/dashboard/index', 10, 1),
  (0, '用户管理', 'USERS', '/users', 'views/users/index', 20, 1),
  (0, '角色权限', 'ROLES', '/roles', 'views/roles/index', 30, 1),
  (0, '制度管理', 'REGULATIONS', '/regulations', 'views/regulations/index', 40, 1),
  (0, '案例分析', 'CASE_ANALYSIS', '/case-analysis', 'views/ai/case-analysis', 50, 1),
  (0, '规章迭代分析', 'REGULATION_ITERATION_ANALYSIS', '/regulation-iteration-analysis', 'views/ai/regulation-iteration-analysis', 55, 1),
  (0, '制度风险库', 'RISKS', '/risks', 'views/risks/index', 60, 1),
  (0, '冲突检测', 'CONFLICT_TASKS', '/conflicts/tasks', 'views/conflicts/tasks', 65, 1),
  (0, 'AI 分析记录', 'AI_RECORDS', '/ai/records', 'views/ai/records', 70, 1),
  (0, '报告预览', 'REPORTS', '/reports', 'views/reports/index', 90, 1),
  (0, '系统参数', 'SYSTEM_CONFIGS', '/system/configs', 'views/system/configs', 100, 1),
  (0, '字典管理', 'SYSTEM_DICTS', '/system/dicts', 'views/system/dicts', 110, 1),
  (0, '操作日志', 'SYSTEM_LOGS', '/system/logs', 'views/system/logs', 120, 1),
  (0, '健康检查', 'SYSTEM_HEALTH', '/system/health', 'views/system/health', 130, 1)
ON DUPLICATE KEY UPDATE
  menu_name = VALUES(menu_name),
  path = VALUES(path),
  component = VALUES(component),
  sort_order = VALUES(sort_order),
  visible = VALUES(visible);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
JOIN sys_menu m
WHERE r.role_code = 'ADMIN';

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
JOIN sys_menu m ON m.menu_code IN (
  'DASHBOARD', 'REGULATIONS', 'CONFLICT_TASKS', 'RISKS', 'AI_RECORDS', 'CASE_ANALYSIS', 'REGULATION_ITERATION_ANALYSIS', 'AI_QA', 'REPORTS', 'SYSTEM_HEALTH'
)
WHERE r.role_code = 'REGULATION_MANAGER';

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
JOIN sys_menu m ON m.menu_code IN (
  'DASHBOARD', 'REGULATIONS', 'CONFLICT_TASKS', 'RISKS', 'AI_RECORDS', 'CASE_ANALYSIS', 'REGULATION_ITERATION_ANALYSIS', 'AI_QA', 'REPORTS', 'SYSTEM_HEALTH'
)
WHERE r.role_code = 'ANALYST';

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
JOIN sys_menu m ON m.menu_code IN (
  'DASHBOARD', 'REGULATIONS', 'REPORTS', 'SYSTEM_HEALTH'
)
WHERE r.role_code = 'VIEWER';

INSERT INTO regulation_set (set_name, set_code, description, enabled, is_default, created_by, updated_by)
SELECT
  '烟花爆竹监管制度集',
  'FIREWORKS_SUPERVISION',
  '围绕烟花爆竹经营许可、禁燃限放、库存回购、迁址申办和事故责任链的真实监管制度样本。',
  1,
  1,
  u.id,
  u.id
FROM sys_user u
WHERE u.username = 'admin'
ON DUPLICATE KEY UPDATE
  set_name = VALUES(set_name),
  description = VALUES(description),
  enabled = VALUES(enabled),
  is_default = VALUES(is_default),
  updated_by = VALUES(updated_by),
  updated_at = CURRENT_TIMESTAMP,
  deleted = 0;

SET @fireworks_set_id = (
  SELECT id
  FROM regulation_set
  WHERE set_code = 'FIREWORKS_SUPERVISION'
  LIMIT 1
);

UPDATE regulation
SET deleted = 1,
    status = 'ARCHIVED',
    updated_at = CURRENT_TIMESTAMP
WHERE code IN ('DEMO-REG-001', 'DEMO-REG-002');

INSERT INTO regulation (
  regulation_set_id, title, code, type_code, publish_department, effective_date, expiry_date,
  applicable_scope, status, analysis_status, created_by, updated_by
)
SELECT
  @fireworks_set_id,
  '烟花爆竹安全管理条例',
  'CZ-FW-REG-001',
  'SUPERVISION',
  '国务院',
  '2006-01-21',
  NULL,
  '适用于烟花爆竹生产、经营、运输、燃放及大型焰火燃放活动的许可、监督和法律责任。',
  'ACTIVE',
  'NOT_ANALYZED',
  u.id,
  u.id
FROM sys_user u
WHERE u.username = 'admin'
ON DUPLICATE KEY UPDATE
  regulation_set_id = VALUES(regulation_set_id),
  title = VALUES(title),
  type_code = VALUES(type_code),
  publish_department = VALUES(publish_department),
  effective_date = VALUES(effective_date),
  expiry_date = VALUES(expiry_date),
  applicable_scope = VALUES(applicable_scope),
  status = VALUES(status),
  analysis_status = VALUES(analysis_status),
  updated_by = VALUES(updated_by),
  updated_at = CURRENT_TIMESTAMP,
  deleted = 0;

INSERT INTO regulation (
  regulation_set_id, title, code, type_code, publish_department, effective_date, expiry_date,
  applicable_scope, status, analysis_status, created_by, updated_by
)
SELECT
  @fireworks_set_id,
  '烟花爆竹经营许可实施办法',
  'CZ-FW-REG-002',
  'APPROVAL',
  '原国家安全生产监督管理总局',
  '2013-12-01',
  NULL,
  '适用于烟花爆竹经营许可证申请、审查、颁发、管理及经营单位安全监管。',
  'ACTIVE',
  'NOT_ANALYZED',
  u.id,
  u.id
FROM sys_user u
WHERE u.username = 'admin'
ON DUPLICATE KEY UPDATE
  regulation_set_id = VALUES(regulation_set_id),
  title = VALUES(title),
  type_code = VALUES(type_code),
  publish_department = VALUES(publish_department),
  effective_date = VALUES(effective_date),
  expiry_date = VALUES(expiry_date),
  applicable_scope = VALUES(applicable_scope),
  status = VALUES(status),
  analysis_status = VALUES(analysis_status),
  updated_by = VALUES(updated_by),
  updated_at = CURRENT_TIMESTAMP,
  deleted = 0;

INSERT INTO regulation (
  regulation_set_id, title, code, type_code, publish_department, effective_date, expiry_date,
  applicable_scope, status, analysis_status, created_by, updated_by
)
SELECT
  @fireworks_set_id,
  '烟花爆竹生产经营安全规定',
  'CZ-FW-REG-003',
  'SUPERVISION',
  '原国家安全生产监督管理总局',
  '2018-03-01',
  NULL,
  '适用于烟花爆竹生产企业、批发企业、零售经营者的安全生产及监督管理。',
  'ACTIVE',
  'NOT_ANALYZED',
  u.id,
  u.id
FROM sys_user u
WHERE u.username = 'admin'
ON DUPLICATE KEY UPDATE
  regulation_set_id = VALUES(regulation_set_id),
  title = VALUES(title),
  type_code = VALUES(type_code),
  publish_department = VALUES(publish_department),
  effective_date = VALUES(effective_date),
  expiry_date = VALUES(expiry_date),
  applicable_scope = VALUES(applicable_scope),
  status = VALUES(status),
  analysis_status = VALUES(analysis_status),
  updated_by = VALUES(updated_by),
  updated_at = CURRENT_TIMESTAMP,
  deleted = 0;

INSERT INTO regulation (
  regulation_set_id, title, code, type_code, publish_department, effective_date, expiry_date,
  applicable_scope, status, analysis_status, created_by, updated_by
)
SELECT
  @fireworks_set_id,
  '烟花爆竹零售店（点）安全技术规范 AQ 4128-2019',
  'CZ-FW-REG-004',
  'SUPERVISION',
  '应急管理部',
  '2020-02-01',
  NULL,
  '适用于烟花爆竹零售店、零售点的设置和安全管理。',
  'ACTIVE',
  'NOT_ANALYZED',
  u.id,
  u.id
FROM sys_user u
WHERE u.username = 'admin'
ON DUPLICATE KEY UPDATE
  regulation_set_id = VALUES(regulation_set_id),
  title = VALUES(title),
  type_code = VALUES(type_code),
  publish_department = VALUES(publish_department),
  effective_date = VALUES(effective_date),
  expiry_date = VALUES(expiry_date),
  applicable_scope = VALUES(applicable_scope),
  status = VALUES(status),
  analysis_status = VALUES(analysis_status),
  updated_by = VALUES(updated_by),
  updated_at = CURRENT_TIMESTAMP,
  deleted = 0;

INSERT INTO regulation (
  regulation_set_id, title, code, type_code, publish_department, effective_date, expiry_date,
  applicable_scope, status, analysis_status, created_by, updated_by
)
SELECT
  @fireworks_set_id,
  '中华人民共和国大气污染防治法（烟花爆竹燃放管理条款）',
  'CZ-FW-REG-005',
  'POLICY',
  '全国人民代表大会常务委员会',
  '2018-10-26',
  NULL,
  '适用于大气污染防治及城市人民政府禁止时段、区域内烟花爆竹燃放管理。',
  'ACTIVE',
  'NOT_ANALYZED',
  u.id,
  u.id
FROM sys_user u
WHERE u.username = 'admin'
ON DUPLICATE KEY UPDATE
  regulation_set_id = VALUES(regulation_set_id),
  title = VALUES(title),
  type_code = VALUES(type_code),
  publish_department = VALUES(publish_department),
  effective_date = VALUES(effective_date),
  expiry_date = VALUES(expiry_date),
  applicable_scope = VALUES(applicable_scope),
  status = VALUES(status),
  analysis_status = VALUES(analysis_status),
  updated_by = VALUES(updated_by),
  updated_at = CURRENT_TIMESTAMP,
  deleted = 0;

INSERT INTO regulation (
  regulation_set_id, title, code, type_code, publish_department, effective_date, expiry_date,
  applicable_scope, status, analysis_status, created_by, updated_by
)
SELECT
  @fireworks_set_id,
  '中华人民共和国行政许可法',
  'CZ-FW-REG-006',
  'APPROVAL',
  '全国人民代表大会常务委员会',
  '2019-04-23',
  NULL,
  '适用于行政许可设定、实施、公开、收费、监督检查和法律责任。',
  'ACTIVE',
  'NOT_ANALYZED',
  u.id,
  u.id
FROM sys_user u
WHERE u.username = 'admin'
ON DUPLICATE KEY UPDATE
  regulation_set_id = VALUES(regulation_set_id),
  title = VALUES(title),
  type_code = VALUES(type_code),
  publish_department = VALUES(publish_department),
  effective_date = VALUES(effective_date),
  expiry_date = VALUES(expiry_date),
  applicable_scope = VALUES(applicable_scope),
  status = VALUES(status),
  analysis_status = VALUES(analysis_status),
  updated_by = VALUES(updated_by),
  updated_at = CURRENT_TIMESTAMP,
  deleted = 0;

INSERT INTO regulation (
  regulation_set_id, title, code, type_code, publish_department, effective_date, expiry_date,
  applicable_scope, status, analysis_status, created_by, updated_by
)
SELECT
  @fireworks_set_id,
  '郴州市中心城区进一步规范销售及禁止和限制燃放烟花爆竹通告相关材料',
  'CZ-FW-REG-007',
  'POLICY',
  '郴州市人民政府及郴州市城市管理和综合执法局',
  '2025-11-26',
  NULL,
  '适用于郴州市中心城区烟花爆竹销售规范、禁燃限放区域调整、相关行政许可办理及存量网点退出安排。',
  'ACTIVE',
  'NOT_ANALYZED',
  u.id,
  u.id
FROM sys_user u
WHERE u.username = 'admin'
ON DUPLICATE KEY UPDATE
  regulation_set_id = VALUES(regulation_set_id),
  title = VALUES(title),
  type_code = VALUES(type_code),
  publish_department = VALUES(publish_department),
  effective_date = VALUES(effective_date),
  expiry_date = VALUES(expiry_date),
  applicable_scope = VALUES(applicable_scope),
  status = VALUES(status),
  analysis_status = VALUES(analysis_status),
  updated_by = VALUES(updated_by),
  updated_at = CURRENT_TIMESTAMP,
  deleted = 0;

INSERT INTO regulation (
  regulation_set_id, title, code, type_code, publish_department, effective_date, expiry_date,
  applicable_scope, status, analysis_status, created_by, updated_by
)
SELECT
  @fireworks_set_id,
  '全国人大常委会法工委备案审查工作报告（烟花爆竹全面禁售禁燃审查意见）',
  'CZ-FW-REG-008',
  'POLICY',
  '全国人大常委会法制工作委员会',
  '2023-12-29',
  NULL,
  '用于判断地方烟花爆竹全面禁售禁燃、限售限放规范性文件是否符合上位法精神和比例原则。',
  'ACTIVE',
  'NOT_ANALYZED',
  u.id,
  u.id
FROM sys_user u
WHERE u.username = 'admin'
ON DUPLICATE KEY UPDATE
  regulation_set_id = VALUES(regulation_set_id),
  title = VALUES(title),
  type_code = VALUES(type_code),
  publish_department = VALUES(publish_department),
  effective_date = VALUES(effective_date),
  expiry_date = VALUES(expiry_date),
  applicable_scope = VALUES(applicable_scope),
  status = VALUES(status),
  analysis_status = VALUES(analysis_status),
  updated_by = VALUES(updated_by),
  updated_at = CURRENT_TIMESTAMP,
  deleted = 0;

INSERT INTO regulation_content (regulation_id, content, plain_text, content_hash)
SELECT
  r.id,
  '信息来源：https://www.gov.cn/gongbao/content/2006/content_219931.htm。来源说明：中国政府网国务院公报发布《烟花爆竹安全管理条例》。关键制度摘录：国家对烟花爆竹生产、经营、运输和举办焰火晚会以及其他大型焰火燃放活动实行许可证制度；未经许可，任何单位或者个人不得生产、经营、运输烟花爆竹。烟花爆竹经营布点应当经安全生产监督管理部门审批，城市市区的零售网点应当按照严格控制的原则合理布设。申请零售经营者应向所在地县级人民政府安全生产监督管理部门提出申请，受理机关应当自受理申请之日起20日内审查材料和经营场所，对符合条件的核发《烟花爆竹经营（零售）许可证》，对不符合条件的应说明理由。零售许可证应载明经营负责人、经营场所地址、经营期限、烟花爆竹种类和限制存放量。县级以上地方人民政府可以根据本行政区域实际情况，确定限制或者禁止燃放烟花爆竹的时间、地点和种类。安全生产监督管理部门、公安部门、质量监督检验部门、工商行政管理部门工作人员在烟花爆竹安全监管工作中滥用职权、玩忽职守、徇私舞弊的，依法追责。',
  '信息来源：https://www.gov.cn/gongbao/content/2006/content_219931.htm。来源说明：中国政府网国务院公报发布《烟花爆竹安全管理条例》。关键制度摘录：国家对烟花爆竹生产、经营、运输和举办焰火晚会以及其他大型焰火燃放活动实行许可证制度；未经许可，任何单位或者个人不得生产、经营、运输烟花爆竹。烟花爆竹经营布点应当经安全生产监督管理部门审批，城市市区的零售网点应当按照严格控制的原则合理布设。申请零售经营者应向所在地县级人民政府安全生产监督管理部门提出申请，受理机关应当自受理申请之日起20日内审查材料和经营场所，对符合条件的核发《烟花爆竹经营（零售）许可证》，对不符合条件的应说明理由。零售许可证应载明经营负责人、经营场所地址、经营期限、烟花爆竹种类和限制存放量。县级以上地方人民政府可以根据本行政区域实际情况，确定限制或者禁止燃放烟花爆竹的时间、地点和种类。安全生产监督管理部门、公安部门、质量监督检验部门、工商行政管理部门工作人员在烟花爆竹安全监管工作中滥用职权、玩忽职守、徇私舞弊的，依法追责。',
  SHA2(CONCAT('CZ-FW-REG-001', r.updated_at), 256)
FROM regulation r
WHERE r.code = 'CZ-FW-REG-001'
ON DUPLICATE KEY UPDATE
  content = VALUES(content),
  plain_text = VALUES(plain_text),
  content_hash = VALUES(content_hash),
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO regulation_content (regulation_id, content, plain_text, content_hash)
SELECT
  r.id,
  '信息来源：https://www.mem.gov.cn/gk/gwgg/agwzlfl/zjl_01/201310/t20131023_233708.shtml。来源说明：应急管理部网站发布国家安全生产监督管理总局令第65号《烟花爆竹经营许可实施办法》。关键制度摘录：烟花爆竹经营单位布点按照保障安全、统一规划、合理布局、总量控制、适度竞争的原则审批；严格控制城市建成区内烟花爆竹零售点数量，且零售点不得与居民居住场所设置在同一建筑物内。县级安全监管局负责本行政区域内零售经营布点规划与零售许可证颁发和管理。零售经营者应符合布点规划，主要负责人经过安全培训合格，春节期间零售点、城市长期零售点实行专店销售，零售场所面积不小于10平方米，周边50米范围内没有其他零售点，并与学校、幼儿园、医院、集贸市场、加油站等重点建筑物保持100米以上安全距离。申请领取零售许可证时，应提交申请书、零售点及其周围安全条件说明和发证机关要求提供的其他材料。发证机关受理后应对申请材料和零售场所安全条件进行现场核查，负责现场核查人员应提出书面核查意见。发证机关应自受理申请之日起20个工作日内作出颁发或不予颁发零售许可证的决定，并书面告知申请人；不予颁发的应书面说明理由。零售许可证有效期限由发证机关确定，最长不超过2年；期满后继续经营，或者变更零售点名称、主要负责人、零售场所和许可范围的，应重新申请取得零售许可证。发证机关应坚持公开、公平、公正原则，严格审查核发许可证，建立档案管理制度和信息化管理系统，并定期向社会公告取证企业名单。任何单位或者个人对违反《烟花爆竹安全管理条例》和本办法规定的行为，有权向安全生产监督管理部门或者监察机关等有关部门举报。以欺骗、贿赂等不正当手段取得许可证的，应予撤销，经营单位3年内不得再次申请。监管工作人员滥用职权、玩忽职守、徇私舞弊、未依法履责的，依法处分；构成犯罪的，依法追究刑事责任。',
  '信息来源：https://www.mem.gov.cn/gk/gwgg/agwzlfl/zjl_01/201310/t20131023_233708.shtml。来源说明：应急管理部网站发布国家安全生产监督管理总局令第65号《烟花爆竹经营许可实施办法》。关键制度摘录：烟花爆竹经营单位布点按照保障安全、统一规划、合理布局、总量控制、适度竞争的原则审批；严格控制城市建成区内烟花爆竹零售点数量，且零售点不得与居民居住场所设置在同一建筑物内。县级安全监管局负责本行政区域内零售经营布点规划与零售许可证颁发和管理。零售经营者应符合布点规划，主要负责人经过安全培训合格，春节期间零售点、城市长期零售点实行专店销售，零售场所面积不小于10平方米，周边50米范围内没有其他零售点，并与学校、幼儿园、医院、集贸市场、加油站等重点建筑物保持100米以上安全距离。申请领取零售许可证时，应提交申请书、零售点及其周围安全条件说明和发证机关要求提供的其他材料。发证机关受理后应对申请材料和零售场所安全条件进行现场核查，负责现场核查人员应提出书面核查意见。发证机关应自受理申请之日起20个工作日内作出颁发或不予颁发零售许可证的决定，并书面告知申请人；不予颁发的应书面说明理由。零售许可证有效期限由发证机关确定，最长不超过2年；期满后继续经营，或者变更零售点名称、主要负责人、零售场所和许可范围的，应重新申请取得零售许可证。发证机关应坚持公开、公平、公正原则，严格审查核发许可证，建立档案管理制度和信息化管理系统，并定期向社会公告取证企业名单。任何单位或者个人对违反《烟花爆竹安全管理条例》和本办法规定的行为，有权向安全生产监督管理部门或者监察机关等有关部门举报。以欺骗、贿赂等不正当手段取得许可证的，应予撤销，经营单位3年内不得再次申请。监管工作人员滥用职权、玩忽职守、徇私舞弊、未依法履责的，依法处分；构成犯罪的，依法追究刑事责任。',
  SHA2(CONCAT('CZ-FW-REG-002', r.updated_at), 256)
FROM regulation r
WHERE r.code = 'CZ-FW-REG-002'
ON DUPLICATE KEY UPDATE
  content = VALUES(content),
  plain_text = VALUES(plain_text),
  content_hash = VALUES(content_hash),
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO regulation_content (regulation_id, content, plain_text, content_hash)
SELECT
  r.id,
  '信息来源：https://www.mem.gov.cn/gk/gwgg/agwzlfl/zjl_01/201801/t20180124_233685.shtml。来源说明：应急管理部网站发布国家安全生产监督管理总局令第93号《烟花爆竹生产经营安全规定》。关键制度摘录：本规定为加强烟花爆竹生产经营安全、预防和减少生产安全事故而制定，适用于烟花爆竹生产企业、批发企业和零售经营者的安全生产及监督管理。生产经营单位应具备法律、行政法规和国家标准或者行业标准规定的安全生产条件，并依法取得相应行政许可。生产企业、批发企业应建立健全全员安全生产责任制、安全生产工作责任体系，制定并落实符合法律法规、国家标准或者行业标准的安全生产规章制度和操作规程。生产企业、批发企业应保证安全设备设施维护、隐患排查治理、风险评估与安全评价、安全生产教育培训、应急救援器材和演练等安全生产资金投入。从业人员应接受烟花爆竹安全知识和岗位操作技能培训，未经安全生产教育培训不得上岗作业。地方安全生产监督管理部门应加强监督检查，明确监管主体，制定并落实年度监督检查计划。',
  '信息来源：https://www.mem.gov.cn/gk/gwgg/agwzlfl/zjl_01/201801/t20180124_233685.shtml。来源说明：应急管理部网站发布国家安全生产监督管理总局令第93号《烟花爆竹生产经营安全规定》。关键制度摘录：本规定为加强烟花爆竹生产经营安全、预防和减少生产安全事故而制定，适用于烟花爆竹生产企业、批发企业和零售经营者的安全生产及监督管理。生产经营单位应具备法律、行政法规和国家标准或者行业标准规定的安全生产条件，并依法取得相应行政许可。生产企业、批发企业应建立健全全员安全生产责任制、安全生产工作责任体系，制定并落实符合法律法规、国家标准或者行业标准的安全生产规章制度和操作规程。生产企业、批发企业应保证安全设备设施维护、隐患排查治理、风险评估与安全评价、安全生产教育培训、应急救援器材和演练等安全生产资金投入。从业人员应接受烟花爆竹安全知识和岗位操作技能培训，未经安全生产教育培训不得上岗作业。地方安全生产监督管理部门应加强监督检查，明确监管主体，制定并落实年度监督检查计划。',
  SHA2(CONCAT('CZ-FW-REG-003', r.updated_at), 256)
FROM regulation r
WHERE r.code = 'CZ-FW-REG-003'
ON DUPLICATE KEY UPDATE
  content = VALUES(content),
  plain_text = VALUES(plain_text),
  content_hash = VALUES(content_hash),
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO regulation_content (regulation_id, content, plain_text, content_hash)
SELECT
  r.id,
  '信息来源：https://www.mem.gov.cn/fw/flfgbz/bz/bzwb/201909/P020220607370319545897.pdf。来源说明：应急管理部发布安全生产行业标准AQ 4128-2019《烟花爆竹零售店（点）安全技术规范》，2019年8月12日发布，2020年2月1日实施。关键制度摘录：本标准规定烟花爆竹零售店、零售点的选址及外部距离、面积和存放限量、平面布置、建筑结构、消防与电气、经营行为及安全管理要求，适用于烟花爆竹零售店、零售点的设置和安全管理。平台分析时应重点关注零售点选址、外部安全距离、是否与居住场所混设、存放限量、消防电气配置、经营行为和安全管理制度是否形成可核验标准。',
  '信息来源：https://www.mem.gov.cn/fw/flfgbz/bz/bzwb/201909/P020220607370319545897.pdf。来源说明：应急管理部发布安全生产行业标准AQ 4128-2019《烟花爆竹零售店（点）安全技术规范》，2019年8月12日发布，2020年2月1日实施。关键制度摘录：本标准规定烟花爆竹零售店、零售点的选址及外部距离、面积和存放限量、平面布置、建筑结构、消防与电气、经营行为及安全管理要求，适用于烟花爆竹零售店、零售点的设置和安全管理。平台分析时应重点关注零售点选址、外部安全距离、是否与居住场所混设、存放限量、消防电气配置、经营行为和安全管理制度是否形成可核验标准。',
  SHA2(CONCAT('CZ-FW-REG-004', r.updated_at), 256)
FROM regulation r
WHERE r.code = 'CZ-FW-REG-004'
ON DUPLICATE KEY UPDATE
  content = VALUES(content),
  plain_text = VALUES(plain_text),
  content_hash = VALUES(content_hash),
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO regulation_content (regulation_id, content, plain_text, content_hash)
SELECT
  r.id,
  '信息来源：https://www.mee.gov.cn/ywgz/fgbz/fl/201811/t20181113_673567.shtml。来源说明：生态环境部发布《中华人民共和国大气污染防治法》现行文本。关键制度摘录：本法以保护和改善环境、防治大气污染、保障公众健康、推进生态文明建设为目的。关于烟花爆竹管理，禁止生产、销售和燃放不符合质量标准的烟花爆竹；任何单位和个人不得在城市人民政府禁止的时段和区域内燃放烟花爆竹。平台分析时应区分对燃放行为的时段、区域禁止，与对合格烟花爆竹销售、经营许可和存量零售网点退出的管理边界，避免将空气质量治理目标泛化为不透明的经营限制。',
  '信息来源：https://www.mee.gov.cn/ywgz/fgbz/fl/201811/t20181113_673567.shtml。来源说明：生态环境部发布《中华人民共和国大气污染防治法》现行文本。关键制度摘录：本法以保护和改善环境、防治大气污染、保障公众健康、推进生态文明建设为目的。关于烟花爆竹管理，禁止生产、销售和燃放不符合质量标准的烟花爆竹；任何单位和个人不得在城市人民政府禁止的时段和区域内燃放烟花爆竹。平台分析时应区分对燃放行为的时段、区域禁止，与对合格烟花爆竹销售、经营许可和存量零售网点退出的管理边界，避免将空气质量治理目标泛化为不透明的经营限制。',
  SHA2(CONCAT('CZ-FW-REG-005', r.updated_at), 256)
FROM regulation r
WHERE r.code = 'CZ-FW-REG-005'
ON DUPLICATE KEY UPDATE
  content = VALUES(content),
  plain_text = VALUES(plain_text),
  content_hash = VALUES(content_hash),
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO regulation_content (regulation_id, content, plain_text, content_hash)
SELECT
  r.id,
  '信息来源：https://www.npc.gov.cn/zgrdw/npc/xinwen/2019-05/07/content_2086830.htm。来源说明：中国人大网发布《中华人民共和国行政许可法》。关键制度摘录：有关行政许可的规定应当公布，未经公布不得作为实施行政许可的依据。行政许可实施机关应当公开行政许可事项、依据、条件、数量、程序、期限以及需要提交的全部材料目录和申请书示范文本。行政机关实施行政许可和对行政许可事项进行监督检查，不得收取任何费用，法律、行政法规另有规定的除外；依法收费的，应按照公布的法定项目和标准收费，所收取费用必须全部上缴国库，不得截留、挪用、私分或者变相私分。平台分析时应关注办证材料目录、收费项目、审查期限、补正告知、现场核查、结果公开和投诉举报机制是否足以压缩寻租空间。',
  '信息来源：https://www.npc.gov.cn/zgrdw/npc/xinwen/2019-05/07/content_2086830.htm。来源说明：中国人大网发布《中华人民共和国行政许可法》。关键制度摘录：有关行政许可的规定应当公布，未经公布不得作为实施行政许可的依据。行政许可实施机关应当公开行政许可事项、依据、条件、数量、程序、期限以及需要提交的全部材料目录和申请书示范文本。行政机关实施行政许可和对行政许可事项进行监督检查，不得收取任何费用，法律、行政法规另有规定的除外；依法收费的，应按照公布的法定项目和标准收费，所收取费用必须全部上缴国库，不得截留、挪用、私分或者变相私分。平台分析时应关注办证材料目录、收费项目、审查期限、补正告知、现场核查、结果公开和投诉举报机制是否足以压缩寻租空间。',
  SHA2(CONCAT('CZ-FW-REG-006', r.updated_at), 256)
FROM regulation r
WHERE r.code = 'CZ-FW-REG-006'
ON DUPLICATE KEY UPDATE
  content = VALUES(content),
  plain_text = VALUES(plain_text),
  content_hash = VALUES(content_hash),
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO regulation_content (regulation_id, content, plain_text, content_hash)
SELECT
  r.id,
  '信息来源一：https://www.chinanews.com.cn/sh/2026/03-27/10593646.shtml。来源说明：中国新闻网援引郴州市人民政府网站消息，发布郴州市联合调查组关于北湖区勇鑫烟花零售店燃爆事件通报。信息来源二：https://m.voc.com.cn/xhn/news/202511/30936049.html。来源说明：新湖南客户端发布郴州市城市管理和综合执法局公开征求意见信息，称征求意见稿全文刊登于郴州市人民政府网站。关键制度事实：郴州市城管局2025年1月起草《关于进一步规范市城区销售及禁止和限制燃放烟花爆竹的通告》，相继开展征求意见、公平竞争审查、合法性审查、听证会、专家评审、社会稳定风险评估等程序，2025年11月26日在市人民政府网站公示期满。通告重新划定中心城区禁止燃放烟花爆竹区域，勇鑫烟花零售店在该区域，并明确对禁燃区范围内不符合法定条件的烟花爆竹零售网点停止办理相关行政许可。为推进禁燃限放，市、区两级应急管理部门以及属地街道对新划定禁燃区内须退出的零售店开展上门宣讲政策、协调批发公司原价回购、禁燃区外异地选址重新依规申办等工作。公开征求意见稿依据《中华人民共和国大气污染防治法》《烟花爆竹安全管理条例》等法律法规，目标包括改善城市环境空气质量、降低噪声污染、消除安全隐患、保障公共安全和生命财产安全。平台分析时应关注禁燃区划定、停止办理行政许可、存量许可证衔接、库存回购价格、迁址条件、重新申办流程和救济渠道是否明确。',
  '信息来源一：https://www.chinanews.com.cn/sh/2026/03-27/10593646.shtml。来源说明：中国新闻网援引郴州市人民政府网站消息，发布郴州市联合调查组关于北湖区勇鑫烟花零售店燃爆事件通报。信息来源二：https://m.voc.com.cn/xhn/news/202511/30936049.html。来源说明：新湖南客户端发布郴州市城市管理和综合执法局公开征求意见信息，称征求意见稿全文刊登于郴州市人民政府网站。关键制度事实：郴州市城管局2025年1月起草《关于进一步规范市城区销售及禁止和限制燃放烟花爆竹的通告》，相继开展征求意见、公平竞争审查、合法性审查、听证会、专家评审、社会稳定风险评估等程序，2025年11月26日在市人民政府网站公示期满。通告重新划定中心城区禁止燃放烟花爆竹区域，勇鑫烟花零售店在该区域，并明确对禁燃区范围内不符合法定条件的烟花爆竹零售网点停止办理相关行政许可。为推进禁燃限放，市、区两级应急管理部门以及属地街道对新划定禁燃区内须退出的零售店开展上门宣讲政策、协调批发公司原价回购、禁燃区外异地选址重新依规申办等工作。公开征求意见稿依据《中华人民共和国大气污染防治法》《烟花爆竹安全管理条例》等法律法规，目标包括改善城市环境空气质量、降低噪声污染、消除安全隐患、保障公共安全和生命财产安全。平台分析时应关注禁燃区划定、停止办理行政许可、存量许可证衔接、库存回购价格、迁址条件、重新申办流程和救济渠道是否明确。',
  SHA2(CONCAT('CZ-FW-REG-007', r.updated_at), 256)
FROM regulation r
WHERE r.code = 'CZ-FW-REG-007'
ON DUPLICATE KEY UPDATE
  content = VALUES(content),
  plain_text = VALUES(plain_text),
  content_hash = VALUES(content_hash),
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO regulation_content (regulation_id, content, plain_text, content_hash)
SELECT
  r.id,
  '信息来源：https://www.npc.gov.cn/npc/c2/c30834/202312/t20231229_433996.html；备份公开报道：https://m.gmw.cn/2023-12/29/content_1303615927.htm。来源说明：全国人大常委会法工委关于2023年备案审查工作情况的报告及官方媒体报道。关键制度事实：全国人大常委会法工委审查认为，大气污染防治法和国务院制定的烟花爆竹安全管理条例等法律、行政法规，对于销售、燃放符合质量标准的烟花爆竹未作全面禁止性规定；地方性法规关于全面禁止销售、燃放烟花爆竹的规定，与上位法规定不一致，应当按照上位法规定的精神修改。平台分析时应将该意见作为审查地方禁售禁燃、限售限放、总量压减和行政许可停止办理政策比例原则、合法性依据和竞争影响的重要证据。',
  '信息来源：https://www.npc.gov.cn/npc/c2/c30834/202312/t20231229_433996.html；备份公开报道：https://m.gmw.cn/2023-12/29/content_1303615927.htm。来源说明：全国人大常委会法工委关于2023年备案审查工作情况的报告及官方媒体报道。关键制度事实：全国人大常委会法工委审查认为，大气污染防治法和国务院制定的烟花爆竹安全管理条例等法律、行政法规，对于销售、燃放符合质量标准的烟花爆竹未作全面禁止性规定；地方性法规关于全面禁止销售、燃放烟花爆竹的规定，与上位法规定不一致，应当按照上位法规定的精神修改。平台分析时应将该意见作为审查地方禁售禁燃、限售限放、总量压减和行政许可停止办理政策比例原则、合法性依据和竞争影响的重要证据。',
  SHA2(CONCAT('CZ-FW-REG-008', r.updated_at), 256)
FROM regulation r
WHERE r.code = 'CZ-FW-REG-008'
ON DUPLICATE KEY UPDATE
  content = VALUES(content),
  plain_text = VALUES(plain_text),
  content_hash = VALUES(content_hash),
  updated_at = CURRENT_TIMESTAMP;

SET @cz_fw_reg_001 = CONCAT_WS('\n',
  '信息来源：https://www.gov.cn/gongbao/content/2006/content_219931.htm。',
  '发布主体：国务院。制度名称：烟花爆竹安全管理条例。',
  '制度定位：该条例是烟花爆竹生产、经营、运输、燃放及大型焰火燃放活动的基础行政法规。它确立许可证制度、经营布点审批、零售许可审查、禁燃限放区域确定和监管人员责任追究规则。',
  '条文要点一：国家对烟花爆竹生产、经营、运输和举办焰火晚会以及其他大型焰火燃放活动实行许可证制度。没有取得许可的单位或者个人，不得生产、经营、运输烟花爆竹，也不得举办大型焰火燃放活动。',
  '条文要点二：烟花爆竹经营分为批发和零售。批发企业和零售经营者的布点，应当经安全生产监督管理部门审批。城市市区零售网点应当严格控制并合理布设。',
  '条文要点三：申请零售经营的经营者，应当向所在地县级人民政府安全生产监督管理部门提出申请。受理机关应当在法定期间内审查材料和经营场所，对符合条件的核发烟花爆竹经营零售许可证，对不符合条件的应当说明理由。',
  '条文要点四：零售许可证应载明经营负责人、经营场所地址、经营期限、烟花爆竹种类和限制存放量。这些字段直接决定经营者是否能够备货、何时销售、能销售哪些品类以及库存上限。',
  '条文要点五：县级以上地方人民政府可以根据本行政区域实际情况，确定限制或者禁止燃放烟花爆竹的时间、地点和种类。该授权主要针对燃放行为，但地方政策如果进一步影响销售许可、库存退出和存量经营网点，应当明确法律依据、过渡期和救济路径。',
  '条文要点六：安全生产监督管理部门、公安部门、质量监督检验部门、工商行政管理部门等负有烟花爆竹安全监督管理职责。监管人员滥用职权、玩忽职守、徇私舞弊的，应依法给予处分；构成犯罪的，依法追究刑事责任。',
  '可分析字段：许可证制度、经营布点审批、城市市区严格控制、零售许可证载明经营期限、限制存放量、禁燃限放区域、监管人员滥用职权、徇私舞弊。',
  '寻租分析关注点：当零售许可和禁燃区域调整共同作用时，许可证经营期限、网点布设数量、限制存放量和禁止燃放区域边界会同时影响经营者核心收益；如果审批、边界、过渡、退出补偿和复核机制不透明，容易形成审批卡点、差别待遇、库存处置和重新选址中的寻租空间。'
);

UPDATE regulation_content rc
JOIN regulation r ON r.id = rc.regulation_id
SET rc.content = @cz_fw_reg_001,
    rc.plain_text = @cz_fw_reg_001,
    rc.content_hash = SHA2(@cz_fw_reg_001, 256),
    rc.updated_at = CURRENT_TIMESTAMP
WHERE r.code = 'CZ-FW-REG-001';

SET @cz_fw_reg_002 = CONCAT_WS('\n',
  '信息来源：https://www.mem.gov.cn/gk/gwgg/agwzlfl/zjl_01/201310/t20131023_233708.shtml。',
  '发布主体：原国家安全生产监督管理总局。制度名称：烟花爆竹经营许可实施办法。',
  '制度定位：该办法细化烟花爆竹批发、零售经营许可证的申请、审查、颁发、延续、变更、撤销和监管。对零售网点而言，最关键的是布点规划、申请材料、现场核查、办理期限、许可证有效期和取证名单公开。',
  '条文要点一：烟花爆竹经营单位布点按照保障安全、统一规划、合理布局、总量控制、适度竞争的原则审批。城市建成区内烟花爆竹零售点数量应严格控制。',
  '条文要点二：县级安全监管局负责本行政区域零售经营布点规划与零售许可证颁发管理。也就是说，同一县区监管机关同时掌握网点规划、数量控制、申请受理、现场核查和最终许可决定多个环节。',
  '条文要点三：零售经营者应符合所在地县级安全监管局制定的零售经营布点规划；主要负责人应经过安全培训合格；春节期间零售点和城市长期零售点实行专店销售；零售场所面积不得小于规定面积；周边一定范围内不得有其他零售点；并应与学校、幼儿园、医院、集贸市场、加油站等重点场所保持规定安全距离。',
  '条文要点四：申请领取零售许可证，应提交申请书、零售点及其周围安全条件说明，以及发证机关要求提供的其他材料。材料清单中的其他材料具有兜底性质，如果目录、格式、补正次数和补正时限不公开，容易形成临时加项和反复补正空间。',
  '条文要点五：发证机关受理申请后，应对申请材料和零售场所安全条件进行现场核查。负责现场核查的人员应提出书面核查意见。现场核查涉及面积、安全距离、周边风险、存放条件、消防条件等事实判断，必须留存测距、照片、人员签名和复核记录。',
  '条文要点六：发证机关应当自受理申请之日起20个工作日内作出颁发或者不予颁发零售许可证的决定，并告知申请人；不予颁发的，应书面说明理由。20个工作日如果与春节备货、销售旺季、许可证到期或地方政策切换期重叠，经营者承担的延误成本会显著升高。',
  '条文要点七：零售许可证有效期限由发证机关确定，最长不超过2年。期满后继续经营，或者变更零售点名称、主要负责人、零售场所和许可范围的，应重新申请取得零售许可证。有效期限不是固定值，而是由发证机关确定，这会影响经营者跨年度备货和春节销售预期。',
  '条文要点八：发证机关应坚持公开、公平、公正原则，严格审查核发许可证，建立许可证档案管理制度和信息化管理系统，并定期向社会公告取证企业名单。公开取证名单不能替代对申请排序、现场核查意见、补正记录、不予许可理由和投诉处理结果的公开。',
  '条文要点九：任何单位或者个人有权举报违反条例和本办法规定的行为。以欺骗、贿赂等不正当手段取得许可证的，应撤销许可证，经营单位在一定期限内不得再次申请。监管工作人员滥用职权、玩忽职守、徇私舞弊的，应依法处分；构成犯罪的，依法追究刑事责任。',
  '可分析字段：统一规划、总量控制、适度竞争、严格控制城市建成区零售点、发证机关要求提供的其他材料、现场核查书面意见、20个工作日、许可证有效期限由发证机关确定、最长不超过2年、期满重新申请、定期公告取证企业名单、举报、贿赂撤销。',
  '寻租分析关注点：该办法同时包含稀缺名额、审批时限、现场核查、兜底材料、许可期限和经营旺季衔接。若年度办证窗口接近春节，经营者为了避免错过主要盈利期，会倾向于用低于预期损失的非正式成本换取及时受理、现场核查通过或优先发证。'
);

UPDATE regulation_content rc
JOIN regulation r ON r.id = rc.regulation_id
SET rc.content = @cz_fw_reg_002,
    rc.plain_text = @cz_fw_reg_002,
    rc.content_hash = SHA2(@cz_fw_reg_002, 256),
    rc.updated_at = CURRENT_TIMESTAMP
WHERE r.code = 'CZ-FW-REG-002';

SET @cz_fw_reg_003 = CONCAT_WS('\n',
  '信息来源：https://www.mem.gov.cn/gk/gwgg/agwzlfl/zjl_01/201801/t20180124_233685.shtml。',
  '发布主体：原国家安全生产监督管理总局。制度名称：烟花爆竹生产经营安全规定。',
  '制度定位：该规定围绕烟花爆竹生产企业、批发企业、零售经营者的安全生产责任、教育培训、隐患排查、风险评估、应急救援和监督检查形成监管框架。',
  '条文要点一：生产经营单位应具备法律、行政法规和国家标准或者行业标准规定的安全生产条件，并依法取得相应行政许可。安全条件与许可资格直接绑定，任何安全条件认定不透明都会影响经营主体准入。',
  '条文要点二：生产企业、批发企业应建立健全全员安全生产责任制、安全生产工作责任体系，制定并落实符合法律法规、国家标准或者行业标准的安全生产规章制度和操作规程。',
  '条文要点三：生产企业、批发企业应保障安全设备设施维护、隐患排查治理、风险评估与安全评价、安全生产教育培训、应急救援器材和演练等安全生产资金投入。',
  '条文要点四：从业人员应接受烟花爆竹安全知识和岗位操作技能培训，未经安全生产教育培训不得上岗作业。零售许可证办理和日常监管中，培训证明、上岗资格、经营负责人安全能力是可核查事项。',
  '条文要点五：地方安全生产监督管理部门应加强监督检查，明确监管主体，制定并落实年度监督检查计划。年度计划、抽查频次、检查清单、隐患整改闭环和复查责任应当可追溯。',
  '条文要点六：生产经营过程中的隐患排查、风险评估、安全评价和应急演练具有专业性。如果第三方服务、批发企业、零售户和监管人员之间缺少隔离机制，可能形成指定服务、重复整改、选择性检查等利益链条。',
  '可分析字段：安全生产条件、依法许可、全员安全责任、教育培训、隐患排查治理、风险评估、安全评价、应急救援、年度监督检查计划、监管主体。',
  '寻租分析关注点：该规定的风险不只在许可发证，也在日常检查和隐患整改。检查计划不公开、整改标准不统一、复查期限不明确时，监管人员可以把检查频次、整改认定和复查通过作为交换资源。'
);

UPDATE regulation_content rc
JOIN regulation r ON r.id = rc.regulation_id
SET rc.content = @cz_fw_reg_003,
    rc.plain_text = @cz_fw_reg_003,
    rc.content_hash = SHA2(@cz_fw_reg_003, 256),
    rc.updated_at = CURRENT_TIMESTAMP
WHERE r.code = 'CZ-FW-REG-003';

SET @cz_fw_reg_004 = CONCAT_WS('\n',
  '信息来源：https://www.mem.gov.cn/fw/flfgbz/bz/bzwb/201909/P020220607370319545897.pdf。',
  '发布主体：应急管理部。标准名称：烟花爆竹零售店（点）安全技术规范 AQ 4128-2019。',
  '制度定位：该标准将零售店和零售点的选址、外部距离、面积、存放限量、平面布置、建筑结构、消防、电气、经营行为和安全管理要求转化为技术规范，是现场核查的重要依据。',
  '条文要点一：标准适用于烟花爆竹零售店、零售点的设置和安全管理。零售店和零售点的概念、经营方式和存放条件不同，核查时应先识别经营形态。',
  '条文要点二：选址和外部距离是核心安全条件。核查时通常需要确认与居民居住场所、学校、幼儿园、医院、集贸市场、加油站、易燃易爆场所等对象的距离关系。',
  '条文要点三：面积和存放限量是许可载明事项的基础。面积、最大允许存放量、货架布置、通道宽度和安全出口关系到是否具备经营条件。',
  '条文要点四：建筑结构、消防与电气要求决定经营场所是否适合存放烟花爆竹。电气线路、照明、灭火器材、警示标志、禁火管理、通风防潮等事项应以清单形式核查。',
  '条文要点五：经营行为和安全管理要求包括不得超范围经营、不得超量存放、不得在许可地点之外储存或者销售、不得与生活居住区域混用、不得违规试放或者违规展示。',
  '条文要点六：技术标准越具体，越能压缩裁量空间；但如果监管只笼统引用标准而不公开核查表、测距方式、照片证据和整改复查程序，仍可能形成选择性解释。',
  '可分析字段：选址、外部距离、面积、存放限量、平面布置、建筑结构、消防、电气、经营行为、安全管理、不得与生活居住区域混用、超范围经营、超量存放。',
  '寻租分析关注点：现场核查人员如果可以自行解释距离、面积、限量和消防电气标准，且无双人核查、影像留痕和复核渠道，经营者可能为了避免反复整改、错过销售窗口或承担库存压力而接受非正式支付。'
);

UPDATE regulation_content rc
JOIN regulation r ON r.id = rc.regulation_id
SET rc.content = @cz_fw_reg_004,
    rc.plain_text = @cz_fw_reg_004,
    rc.content_hash = SHA2(@cz_fw_reg_004, 256),
    rc.updated_at = CURRENT_TIMESTAMP
WHERE r.code = 'CZ-FW-REG-004';

SET @cz_fw_reg_005 = CONCAT_WS('\n',
  '信息来源：https://www.mee.gov.cn/ywgz/fgbz/fl/201811/t20181113_673567.shtml。',
  '发布主体：全国人民代表大会常务委员会。制度名称：中华人民共和国大气污染防治法。',
  '制度定位：该法以保护和改善环境、防治大气污染、保障公众健康、推进生态文明建设为目标。对烟花爆竹的直接约束集中在不合格产品和城市人民政府确定的禁止燃放时段、区域。',
  '条文要点一：禁止生产、销售和燃放不符合质量标准的烟花爆竹。该条针对产品质量和安全环保标准，不等同于对所有合格烟花爆竹销售行为的全面禁止。',
  '条文要点二：任何单位和个人不得在城市人民政府禁止的时段和区域内燃放烟花爆竹。该条授权城市人民政府确定禁止燃放的时段和区域，核心对象是燃放行为。',
  '条文要点三：地方政策如果基于空气质量和公共安全需求扩大禁燃限放，应当清楚区分禁放区域、限放时段、销售许可、存量网点退出、库存处置和迁址重新申办之间的法律关系。',
  '条文要点四：环境治理目标具有公共利益正当性，但经营主体退出、停办许可或迁址安排会影响既有经营收益，应同步明确过渡期、补偿或回购规则、申请救济路径和公开公示程序。',
  '可分析字段：不符合质量标准的烟花爆竹、禁止时段、禁止区域、城市人民政府、燃放行为、质量标准、空气质量治理、公共安全。',
  '寻租分析关注点：如果地方执行中把禁燃区管理泛化为不透明停办销售许可，或者只对部分主体保留经营空间，容易形成边界划定、政策解释、许可停办和存量退出中的差别待遇。'
);

UPDATE regulation_content rc
JOIN regulation r ON r.id = rc.regulation_id
SET rc.content = @cz_fw_reg_005,
    rc.plain_text = @cz_fw_reg_005,
    rc.content_hash = SHA2(@cz_fw_reg_005, 256),
    rc.updated_at = CURRENT_TIMESTAMP
WHERE r.code = 'CZ-FW-REG-005';

SET @cz_fw_reg_006 = CONCAT_WS('\n',
  '信息来源：https://www.npc.gov.cn/zgrdw/npc/xinwen/2019-05/07/content_2086830.htm。',
  '发布主体：全国人民代表大会常务委员会。制度名称：中华人民共和国行政许可法。',
  '制度定位：该法规范行政许可设定、实施、公开、期限、费用、监督检查和法律责任，是烟花爆竹经营零售许可证办理的通用上位规则。',
  '条文要点一：有关行政许可的规定应当公布，未经公布不得作为实施行政许可的依据。许可事项、依据、条件、数量、程序、期限以及需要提交的全部材料目录和申请书示范文本应向社会公开。',
  '条文要点二：申请材料存在可以当场更正的错误，应允许申请人当场更正；材料不齐全或者不符合法定形式的，应一次性告知需要补正的全部内容。未一次性告知的，可能导致经营者反复补材料。',
  '条文要点三：行政机关应在法定期限内作出行政许可决定。不能按期作出决定的，应依法履行延期批准和告知程序。对于季节性强的烟花爆竹零售行业，期限延误直接影响主要销售窗口。',
  '条文要点四：行政机关实施行政许可和对行政许可事项进行监督检查，不得收取费用；依法收费的，应按照公布的法定项目和标准收费，所收取费用必须全部上缴国库。',
  '条文要点五：行政机关应对被许可人从事行政许可事项活动进行监督检查，并将监督检查情况和处理结果记录归档。公众有权查阅监督检查记录，法律、行政法规另有规定的除外。',
  '条文要点六：行政机关工作人员办理行政许可、实施监督检查时，索取或者收受他人财物或者谋取其他利益的，应依法处分；构成犯罪的，依法追究刑事责任。',
  '可分析字段：许可事项公开、依据条件数量程序期限公开、材料目录公开、一次性告知、法定期限、不收费、监督检查记录、公众查阅、工作人员索取收受财物。',
  '寻租分析关注点：烟花爆竹经营许可的寻租风险常在材料目录、补正告知、办理期限、现场核查、监督检查记录和收费边界。行政许可法提供了判断这些环节是否公开透明、可救济、可追责的基准。'
);

UPDATE regulation_content rc
JOIN regulation r ON r.id = rc.regulation_id
SET rc.content = @cz_fw_reg_006,
    rc.plain_text = @cz_fw_reg_006,
    rc.content_hash = SHA2(@cz_fw_reg_006, 256),
    rc.updated_at = CURRENT_TIMESTAMP
WHERE r.code = 'CZ-FW-REG-006';

SET @cz_fw_reg_007 = CONCAT_WS('\n',
  '信息来源一：https://www.chinanews.com.cn/sh/2026/03-27/10593646.shtml。',
  '信息来源二：https://m.voc.com.cn/xhn/news/202511/30936049.html。',
  '材料定位：该样本汇总郴州市北湖区勇鑫烟花零售店燃爆事件通报和郴州市中心城区烟花爆竹销售、禁燃限放通告相关公开材料，用于分析地方政策切换、存量零售网点退出和许可证办理衔接中的寻租激励。',
  '事实要点一：公开通报显示，2025年11月30日7时44分，郴州市北湖区勇鑫烟花零售店发生燃爆，公安机关后续认定燃爆系经营者所为。事件造成3人受伤、车辆和房屋受损。',
  '事实要点二：通报显示，勇鑫烟花零售店许可证有效期为2024年12月27日至2025年12月31日。该期限接近公历年末到期，春节销售旺季通常在年末年初衔接，许可证续期、库存采购和主要盈利期高度相关。',
  '事实要点三：郴州市城管局2025年1月起草《关于进一步规范市城区销售及禁止和限制燃放烟花爆竹的通告》，相继开展征求意见、公平竞争审查、合法性审查、听证会、专家评审、社会稳定风险评估等程序，2025年11月26日在市人民政府网站公示期满。',
  '事实要点四：通告重新划定中心城区禁止燃放烟花爆竹区域，勇鑫烟花零售店处于新划定区域内，并明确对禁燃区范围内不符合法定条件的烟花爆竹零售网点停止办理相关行政许可。',
  '事实要点五：通报显示，为推进禁燃限放工作，市、区两级应急管理部门以及属地街道对新划定禁燃区内须退出的零售店开展上门政策宣讲、协调批发公司原价回购、禁燃区外异地选址重新依规申办等工作。',
  '事实要点六：公开征求意见材料称，通告依据大气污染防治法、烟花爆竹安全管理条例等法律法规，目的包括改善城市环境空气质量、降低噪声污染、消除安全隐患、保障公共安全和生命财产安全。',
  '事实要点七：通报还显示，针对有关公职人员涉嫌违纪违法问题线索，经核查，对存在违反中央八项规定精神、工作纪律、群众纪律等违纪违法行为以及履行责任不到位的13名公职人员作出处理，并对2个党组织问责。',
  '可分析字段：2024年12月27日至2025年12月31日、2025年11月26日公示期满、2025年11月30日燃爆、禁燃区重新划定、停止办理相关行政许可、原价回购、异地选址、重新依规申办、13名公职人员被处理。',
  '寻租分析关注点一：许可证有效期在2025年12月31日结束，而政策公示期满为2025年11月26日，燃爆发生于2025年11月30日。时间节点集中在年末，经营者既面临许可证到期，也面临春节备货和政策退出压力，非正式支付的机会成本比较容易被放大。',
  '寻租分析关注点二：停止办理相关行政许可、原价回购、禁燃区外异地选址和重新申办都需要具体规则支撑。如果缺少退出名单、库存核验、价格认定、付款时限、迁址排序和异议渠道，就可能在回购资格、回购金额、选址协助和重新申请中形成寻租空间。',
  '寻租分析关注点三：通告程序虽然包含征求意见、公平竞争审查、合法性审查、听证、专家评审和稳定风险评估，但平台仍应检查这些程序的结果是否公开、是否回应个体经营者具体利益影响、是否设置快速救济。'
);

UPDATE regulation_content rc
JOIN regulation r ON r.id = rc.regulation_id
SET rc.content = @cz_fw_reg_007,
    rc.plain_text = @cz_fw_reg_007,
    rc.content_hash = SHA2(@cz_fw_reg_007, 256),
    rc.updated_at = CURRENT_TIMESTAMP
WHERE r.code = 'CZ-FW-REG-007';

SET @cz_fw_reg_008 = CONCAT_WS('\n',
  '信息来源：https://www.npc.gov.cn/npc/c2/c30834/202312/t20231229_433996.html。',
  '备份公开报道：https://m.gmw.cn/2023-12/29/content_1303615927.htm。',
  '材料定位：全国人大常委会法工委2023年备案审查工作情况报告中，涉及地方性法规全面禁止销售、燃放烟花爆竹与上位法规定精神的关系。该材料可作为审查地方禁售禁燃、限售限放和停止办理许可政策合法性边界的重要依据。',
  '审查要点一：有公民、企业对若干地方性法规全面禁止销售、燃放烟花爆竹提出审查建议，认为相关规定与大气污染防治法、烟花爆竹安全管理条例等上位法不一致。',
  '审查要点二：法工委审查认为，大气污染防治法和国务院制定的烟花爆竹安全管理条例等法律、行政法规，对于销售、燃放符合质量标准的烟花爆竹未作全面禁止性规定。',
  '审查要点三：地方性法规关于全面禁止销售、燃放烟花爆竹的规定，与上位法规定不一致，应当按照上位法规定的精神修改。',
  '审查要点四：该意见并不否定地方政府基于公共安全、环境治理和城市管理需要依法划定禁止或者限制燃放的时段和区域，但提醒地方政策不得脱离上位法授权边界。',
  '可分析字段：全面禁止销售、全面禁止燃放、符合质量标准、未作全面禁止性规定、与上位法规定不一致、按照上位法精神修改。',
  '寻租分析关注点：当地方政策从禁止燃放延伸到销售许可停办、存量网点退出或者总量压减时，需要检查是否存在上位法依据、比例原则、公平竞争影响和过渡安排。合法性边界越模糊，执行机关越可能在保留谁、退出谁、如何迁址和何时重新申办中掌握过大裁量。'
);

UPDATE regulation_content rc
JOIN regulation r ON r.id = rc.regulation_id
SET rc.content = @cz_fw_reg_008,
    rc.plain_text = @cz_fw_reg_008,
    rc.content_hash = SHA2(@cz_fw_reg_008, 256),
    rc.updated_at = CURRENT_TIMESTAMP
WHERE r.code = 'CZ-FW-REG-008';

SET @cz_fw_reg_001 = CONCAT_WS('\n',
  @cz_fw_reg_001,
  '扩展条款清单一：许可制度的核心不是单一证照，而是把生产、经营、运输、燃放活动分别纳入许可边界。平台分析时应抽取许可事项、许可主体、许可机关、许可期限、许可范围、许可载明事项、未许可后果。',
  '扩展条款清单二：经营布点审批与零售许可审查共同决定市场准入。布点阶段决定能不能进入某一区域，零售许可阶段决定某一经营者能不能在特定地点经营。两者叠加时，名额、地址和期限都可能成为可交换利益。',
  '扩展条款清单三：零售许可证载明经营负责人、经营场所地址、经营期限、烟花爆竹种类和限制存放量。每一项都能影响经营收益：负责人变更会触发管理审查，地址决定客流和禁燃区边界，经营期限影响跨年春节销售，品类和限量影响备货规模。',
  '扩展条款清单四：地方政府确定禁燃限放时间、地点、种类时，应与经营许可和销售行为区分。禁放区域可以减少燃放风险，但如果同步导致经营点退出，需要明确依据、对象、过渡、库存、补偿、迁址、救济。',
  '扩展条款清单五：监管人员责任条款说明制度已经预见滥用职权、玩忽职守和徇私舞弊风险。平台不应只识别经营者违法，也应识别行政机关在许可、检查、处罚、退出和信息公开中的权力约束是否充分。',
  '分析字段扩展：经营布点审批是否公开规划图和数量；许可审查是否公开材料目录和现场核查清单；经营期限是否避开主要盈利期；禁燃限放政策是否有存量许可衔接；监管责任是否能定位到具体岗位和流程记录。',
  '具体寻租场景：若某区域零售网点严格控制，监管机关掌握布点名额和地址判断，经营者可能为了进入有客流的边界区域而支付好处；若经营期限临近春节，经营者可能为了避免错过旺季而寻求加速审查；若限制存放量核定不透明，经营者可能通过关系争取更高限量。'
);

UPDATE regulation_content rc
JOIN regulation r ON r.id = rc.regulation_id
SET rc.content = @cz_fw_reg_001,
    rc.plain_text = @cz_fw_reg_001,
    rc.content_hash = SHA2(@cz_fw_reg_001, 256),
    rc.updated_at = CURRENT_TIMESTAMP
WHERE r.code = 'CZ-FW-REG-001';

SET @cz_fw_reg_002 = CONCAT_WS('\n',
  @cz_fw_reg_002,
  '扩展条款清单一：统一规划、合理布局、总量控制和适度竞争是互相制约的四个原则。总量控制如果缺少公开的数量依据、空间布局图、评估周期和调整规则，会把公共安全管理转化为稀缺指标分配。',
  '扩展条款清单二：县级安全监管局负责零售经营布点规划与零售许可证颁发管理，意味着同一机关可能同时影响是否有网点、网点在哪里、谁能申请、何时核查、是否发证。平台应提高审批集中度权重。',
  '扩展条款清单三：零售点需符合面积、安全距离、周边无其他零售点、远离重点场所等条件。这些条件看似技术化，但现场测距、对象识别、边界认定和照片留痕如果不规范，实际裁量仍然很大。',
  '扩展条款清单四：申请材料中的零售点及其周围安全条件说明，应当与现场核查清单对应。如果申请人事先不知道如何描述周边条件，容易产生中介代办、熟人辅导和补正卡点。',
  '扩展条款清单五：发证机关要求提供的其他材料应当被严格限定。其他材料不能成为临时增加证明、指定第三方报告、要求额外盖章或者要求经营者提供难以取得材料的入口。',
  '扩展条款清单六：现场核查人员提出书面核查意见，是责任链条的关键证据。分析时应检查核查意见是否包含核查人员、时间、地点、测量方法、照片、整改项、复查结论和申请人确认。',
  '扩展条款清单七：20个工作日许可决定在普通行业中可能可接受，但烟花爆竹零售具有季节性。若受理期落在春节前一个月，延迟一天都可能损失大额销售机会，寻租激励会显著高于日常经营许可。',
  '扩展条款清单八：零售许可证有效期限由发证机关确定且最长不超过2年。若同一地区对不同经营者给予不同期限，且没有公开标准，就会形成差别待遇。若期限统一到公历年末，也会把续证压力集中到春节前。',
  '扩展条款清单九：期满继续经营和变更零售场所均需重新申请。禁燃区调整、城市更新、租赁到期等事项可能迫使经营者重新选址，如果重新申办排序不透明，迁址机会本身会变成稀缺资源。',
  '扩展条款清单十：定期公告取证企业名单有助于结果公开，但不足以解释过程公正。平台应继续要求公开申请总数、通过数、不予许可原因分类、现场核查问题、撤销许可证原因和投诉处理数量。',
  '具体寻租场景：审批人员以安全距离需重新测量为由拖延核查；以其他材料为由要求申请人找指定机构出具说明；在网点总量有限时优先通知特定申请人；在春节前控制发证节奏，使经营者以支付成本换取提前办结。'
);

UPDATE regulation_content rc
JOIN regulation r ON r.id = rc.regulation_id
SET rc.content = @cz_fw_reg_002,
    rc.plain_text = @cz_fw_reg_002,
    rc.content_hash = SHA2(@cz_fw_reg_002, 256),
    rc.updated_at = CURRENT_TIMESTAMP
WHERE r.code = 'CZ-FW-REG-002';

SET @cz_fw_reg_003 = CONCAT_WS('\n',
  @cz_fw_reg_003,
  '扩展条款清单一：安全生产条件是取得许可的前提，也是日常监管和整改复查的判断标准。平台应把安全条件拆解为设备设施、人员培训、管理制度、隐患排查、风险评估、应急准备。',
  '扩展条款清单二：全员安全生产责任制要求责任落实到岗位。如果制度只写企业负责而没有明确企业主要负责人、零售店负责人、批发配送人员和监管检查人员的责任边界，追责链条会模糊。',
  '扩展条款清单三：隐患排查治理和风险评估具有持续性。监管机关如果可以频繁要求整改但不说明隐患等级、整改依据和复查期限，经营者会面对持续不确定性。',
  '扩展条款清单四：安全生产教育培训是上岗条件。培训机构选择、培训合格证明、证书有效期和补训安排如果不透明，可能形成指定培训、强制服务和费用转嫁。',
  '扩展条款清单五：年度监督检查计划应明确检查对象、检查频次、重点事项、随机抽查方式和结果公开范围。缺少计划会使检查具有随意性。',
  '扩展条款清单六：安全评价、风险评估和应急演练可能需要第三方服务。如果监管人员事实上指定服务机构，或者用是否购买服务影响检查评价，就形成利益输送风险。',
  '具体寻租场景：经营者为尽快通过隐患复查向检查人员输送利益；批发企业利用监管关系要求零售户选择指定配送或培训；监管人员用年度检查计划外抽查影响经营者旺季销售。'
);

UPDATE regulation_content rc
JOIN regulation r ON r.id = rc.regulation_id
SET rc.content = @cz_fw_reg_003,
    rc.plain_text = @cz_fw_reg_003,
    rc.content_hash = SHA2(@cz_fw_reg_003, 256),
    rc.updated_at = CURRENT_TIMESTAMP
WHERE r.code = 'CZ-FW-REG-003';

SET @cz_fw_reg_004 = CONCAT_WS('\n',
  @cz_fw_reg_004,
  '扩展技术要点一：选址核查应明确测距起止点。不同测距方式可能导致同一店铺通过或不通过，必须通过图示、坐标、照片和复核签名固定证据。',
  '扩展技术要点二：面积和存放限量应形成对应关系。若面积核定不清，限量就会变成可裁量事项；若限量调整没有公开标准，经营者可能为增加库存上限寻求非正式帮助。',
  '扩展技术要点三：消防和电气项目应使用检查表。灭火器配置、警示标识、电气线路、照明设备、防潮通风、禁火管理等项目都应有合格、不合格、整改后复查三类记录。',
  '扩展技术要点四：不得与生活居住区域混用是重要安全边界。监管应明确同一建筑、相邻建筑、楼上楼下、后场仓储等不同场景的判断标准。',
  '扩展技术要点五：经营行为监管包括超范围经营、超量存放、异地储存、违规销售和违规展示。旺季检查如果只凭现场人员主观认定，会增加选择性执法空间。',
  '扩展技术要点六：技术标准可以降低自由裁量，但前提是核查表、照片、测距数据和整改复查均可追溯。若只在结论中写符合或不符合，标准仍无法真正约束核查人员。',
  '具体寻租场景：核查人员对相邻重点场所距离认定前后不一；同类场所面积或限量核定差异明显；整改复查没有期限，导致经营者在春节前被迫寻求加速复查。'
);

UPDATE regulation_content rc
JOIN regulation r ON r.id = rc.regulation_id
SET rc.content = @cz_fw_reg_004,
    rc.plain_text = @cz_fw_reg_004,
    rc.content_hash = SHA2(@cz_fw_reg_004, 256),
    rc.updated_at = CURRENT_TIMESTAMP
WHERE r.code = 'CZ-FW-REG-004';

SET @cz_fw_reg_005 = CONCAT_WS('\n',
  @cz_fw_reg_005,
  '扩展条款清单一：大气污染防治法提供的是环境治理依据，重点是防治大气污染和规范燃放行为。平台应把该依据与安全监管许可依据分开建模。',
  '扩展条款清单二：禁止在城市人民政府禁止的时段和区域内燃放烟花爆竹，说明地方政府可以划定时段和区域，但制度文本应说明划定依据、边界图、实施日期、例外情形和公众告知。',
  '扩展条款清单三：销售不符合质量标准的烟花爆竹被禁止，这一规则指向质量标准。对于符合质量标准的产品，是否限制销售需要另行审查上位法依据和地方政策目标。',
  '扩展条款清单四：禁燃区调整会改变消费者需求和经营者收益。如果禁燃区扩大导致部分零售户退出，应明确是否属于行政许可条件变化、是否影响既有许可证效力以及如何处置库存。',
  '扩展条款清单五：环境治理政策容易以公共利益为由快速推进，但平台应识别程序公开是否充分，包括征求意见、听证、专家评审、稳定风险评估、公平竞争审查和合法性审查结果是否可查。',
  '具体寻租场景：禁燃区边界附近的店铺通过非公开协调被排除或纳入；合格产品销售被变相全面禁止但仍保留个别渠道；停办许可没有解释与环境治理目标之间的比例关系。'
);

UPDATE regulation_content rc
JOIN regulation r ON r.id = rc.regulation_id
SET rc.content = @cz_fw_reg_005,
    rc.plain_text = @cz_fw_reg_005,
    rc.content_hash = SHA2(@cz_fw_reg_005, 256),
    rc.updated_at = CURRENT_TIMESTAMP
WHERE r.code = 'CZ-FW-REG-005';

SET @cz_fw_reg_006 = CONCAT_WS('\n',
  @cz_fw_reg_006,
  '扩展条款清单一：许可公开不只公开办事指南，还应公开条件、数量、程序、期限、材料目录和示范文本。烟花爆竹零售许可存在数量控制时，数量和排序规则尤其关键。',
  '扩展条款清单二：一次性告知是抑制材料寻租的核心机制。系统应识别是否存在补正清单、补正依据、告知时间、经办人、申请人确认和逾期默认后果。',
  '扩展条款清单三：许可期限是抑制拖延寻租的核心机制。若临近春节，普通法定期限可能仍不足以保护经营者，应通过行业制度设置提前受理和旺季前办结机制。',
  '扩展条款清单四：行政许可不收费原则能识别显性收费问题，但现实中可能转化为指定培训、指定评估、指定中介、重复材料打印、非正式协调费等隐性负担。',
  '扩展条款清单五：监督检查记录归档和公众查阅可以把日常检查转化为可审计证据。若检查记录不公开，经营者很难证明同类经营者受到不同对待。',
  '扩展条款清单六：工作人员索取收受财物、谋取其他利益被明确禁止。平台应把是否存在办件留痕、岗位分离、双人核查、随机抽查、投诉处理和纪检移送作为制度反寻租强度指标。',
  '具体寻租场景：经办人不一次性告知材料，要求申请人反复补正；现场核查迟迟不安排，暗示找熟人协调；以免费许可为名，实际要求参加指定培训或购买指定服务。'
);

UPDATE regulation_content rc
JOIN regulation r ON r.id = rc.regulation_id
SET rc.content = @cz_fw_reg_006,
    rc.plain_text = @cz_fw_reg_006,
    rc.content_hash = SHA2(@cz_fw_reg_006, 256),
    rc.updated_at = CURRENT_TIMESTAMP
WHERE r.code = 'CZ-FW-REG-006';

SET @cz_fw_reg_007 = CONCAT_WS('\n',
  @cz_fw_reg_007,
  '扩展事实链一：该事件的时间顺序对寻租分析非常重要。2025年1月起草通告，2025年11月26日公示期满，2025年11月30日发生燃爆，许可证有效期至2025年12月31日。政策切换、许可证到期和春节备货压力集中在一个多月内。',
  '扩展事实链二：通告重新划定禁燃区，勇鑫烟花零售店处于新划定禁燃区内。禁燃区边界如何划定、店铺是否刚好位于边界内外、同类店铺如何处理，是判断差别待遇的重要事实字段。',
  '扩展事实链三：停止办理相关行政许可不是单一动作。它至少涉及停止新办、停止续办、停止变更、存量许可证是否继续有效、到期前是否能销售库存、是否可迁址重新申请。',
  '扩展事实链四：原价回购看似保护经营者，但仍需明确库存品类、数量核验、进货凭证、损耗认定、付款主体、付款期限、争议处理和未回购库存处置方式。',
  '扩展事实链五：禁燃区外异地选址重新依规申办也需要规则。候选区域、布点名额、安全距离、申请排序、原网点优先权、重新培训和现场核查都可能影响经营者实际损失。',
  '扩展事实链六：事件后对13名公职人员作出处理，说明监管链条中存在责任问题。平台不应推定具体违法事实，但应把公职人员处理作为制度复盘证据，检查权责、检查、许可、政策宣讲和群众工作是否可追溯。',
  '扩展事实链七：对经营者而言，正常举报、行政复议或诉讼可能无法在春节前解决许可、库存和迁址问题。救济机制越慢，经营者越可能选择成本更低、见效更快的非正式路径。',
  '具体寻租场景：退出网点回购名单不透明；迁址重新申办排序不透明；停办许可口径只对部分店铺严格执行；现场政策宣讲没有书面告知，导致经营者不知道救济期限和材料要求。'
);

UPDATE regulation_content rc
JOIN regulation r ON r.id = rc.regulation_id
SET rc.content = @cz_fw_reg_007,
    rc.plain_text = @cz_fw_reg_007,
    rc.content_hash = SHA2(@cz_fw_reg_007, 256),
    rc.updated_at = CURRENT_TIMESTAMP
WHERE r.code = 'CZ-FW-REG-007';

SET @cz_fw_reg_008 = CONCAT_WS('\n',
  @cz_fw_reg_008,
  '扩展审查要点一：备案审查意见提供了地方政策合法性边界。地方可以依法限制特定时段和区域燃放，但不能简单把全部合格烟花爆竹销售、燃放一概禁止而不回应上位法规定。',
  '扩展审查要点二：全面禁售禁燃与限时限区禁燃不同。平台应在冲突检测中区分全面禁止、区域禁止、时段禁止、品类禁止、销售许可停办、存量退出等不同政策强度。',
  '扩展审查要点三：如果地方政策从禁燃扩展到停办销售许可，需要说明为何销售行为本身会破坏禁燃目标，以及是否存在更温和手段，例如限制销售时段、限制品类、限制存放量、加强流向登记。',
  '扩展审查要点四：公平竞争影响同样重要。全面或变相禁止销售可能改变市场主体数量，若保留少数批发企业或特定零售点，就需要公开选择标准，避免形成行政性垄断或利益输送。',
  '扩展审查要点五：上位法冲突风险本身也是寻租风险来源。当基层执行人员面对模糊或争议性政策时，可以用政策不确定性影响经营者预期，进而形成寻租空间。',
  '具体寻租场景：以禁燃名义压减全部零售户，但保留少数渠道；以政策争议为由拖延个案许可；在边界政策不清时，让经营者通过关系获得更有利解释。'
);

UPDATE regulation_content rc
JOIN regulation r ON r.id = rc.regulation_id
SET rc.content = @cz_fw_reg_008,
    rc.plain_text = @cz_fw_reg_008,
    rc.content_hash = SHA2(@cz_fw_reg_008, 256),
    rc.updated_at = CURRENT_TIMESTAMP
WHERE r.code = 'CZ-FW-REG-008';

DELETE kc
FROM knowledge_chunk kc
JOIN regulation r ON r.id = kc.source_id
WHERE kc.source_type = 'REGULATION'
  AND (r.code LIKE 'CZ-FW-REG-%' OR r.deleted = 1);

INSERT INTO operation_log (
  user_id, username, module_name, operation_type, business_id,
  request_method, request_uri, request_params, result_code, ip_address
)
SELECT u.id, u.username, 'SYSTEM', 'INIT_DEMO_DATA', NULL, 'SQL', 'database/init/005_seed_demo_data.sql', '初始化真实烟花爆竹监管制度样本数据', 'SUCCESS', '127.0.0.1'
FROM sys_user u
WHERE u.username = 'admin';
