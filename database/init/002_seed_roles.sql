SET NAMES utf8mb4;
SET time_zone = '+08:00';
USE rent_seeking_analysis;

INSERT INTO sys_role (role_code, role_name, description, enabled)
VALUES
  ('ADMIN', '系统管理员', '管理账号、角色、系统参数和全量数据', 1),
  ('REGULATION_MANAGER', '制度管理员', '维护制度信息并发起分析任务', 1),
  ('ANALYST', '分析人员', '查看制度并复核冲突与风险分析结果', 1),
  ('VIEWER', '只读用户', '浏览制度、看板和报告', 1)
ON DUPLICATE KEY UPDATE
  role_name = VALUES(role_name),
  description = VALUES(description),
  enabled = VALUES(enabled),
  updated_at = CURRENT_TIMESTAMP;
