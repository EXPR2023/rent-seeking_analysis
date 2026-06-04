SET NAMES utf8mb4;
SET time_zone = '+08:00';
USE rent_seeking_analysis;

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
  KEY idx_regulation_set_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='规章集表';

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

SET @default_regulation_set_id = (
  SELECT id
  FROM regulation_set
  WHERE set_code = 'FIREWORKS_SUPERVISION'
  LIMIT 1
);

DROP PROCEDURE IF EXISTS add_regulation_set_id_column;
DELIMITER //
CREATE PROCEDURE add_regulation_set_id_column()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'regulation'
      AND column_name = 'regulation_set_id'
  ) THEN
    ALTER TABLE regulation ADD COLUMN regulation_set_id BIGINT NULL COMMENT '规章集ID' AFTER id;
  END IF;
END//
DELIMITER ;
CALL add_regulation_set_id_column();
DROP PROCEDURE add_regulation_set_id_column;

UPDATE regulation
SET regulation_set_id = @default_regulation_set_id
WHERE regulation_set_id IS NULL;

ALTER TABLE regulation
  MODIFY regulation_set_id BIGINT NOT NULL;

DROP PROCEDURE IF EXISTS migrate_regulation_code_index;
DELIMITER //
CREATE PROCEDURE migrate_regulation_code_index()
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'regulation'
      AND index_name = 'uk_regulation_code'
  ) THEN
    ALTER TABLE regulation DROP INDEX uk_regulation_code;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'regulation'
      AND index_name = 'uk_regulation_set_code'
  ) THEN
    ALTER TABLE regulation ADD UNIQUE KEY uk_regulation_set_code (regulation_set_id, code);
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'regulation'
      AND index_name = 'idx_regulation_set_id'
  ) THEN
    ALTER TABLE regulation ADD KEY idx_regulation_set_id (regulation_set_id);
  END IF;
END//
DELIMITER ;
CALL migrate_regulation_code_index();
DROP PROCEDURE migrate_regulation_code_index;

UPDATE regulation r
SET r.regulation_set_id = @default_regulation_set_id
WHERE r.code LIKE 'CZ-FW-REG-%'
  AND r.deleted = 0;
