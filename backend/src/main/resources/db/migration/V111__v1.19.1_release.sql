
DROP PROCEDURE IF EXISTS schema_change_api;
DELIMITER //
CREATE PROCEDURE schema_change_api() BEGIN
    DECLARE  CurrentDatabase VARCHAR(100);
    SELECT DATABASE() INTO CurrentDatabase;
    IF NOT EXISTS (SELECT * FROM information_schema.statistics WHERE table_schema=CurrentDatabase AND table_name = 'test_plan_api_case' AND index_name = 'plan_id_index') THEN
        ALTER TABLE `test_plan_api_case` ADD INDEX plan_id_index ( `test_plan_id` );
    END IF;
END//
DELIMITER ;
CALL schema_change_api();

DROP PROCEDURE IF EXISTS schema_change_api_one;
DELIMITER //
CREATE PROCEDURE schema_change_api_one() BEGIN
    DECLARE  CurrentDatabase VARCHAR(100);
    SELECT DATABASE() INTO CurrentDatabase;
    IF  EXISTS (SELECT * FROM information_schema.statistics WHERE table_schema=CurrentDatabase AND table_name = 'test_plan_api_case' AND index_name = 'planIdIndex') THEN
        ALTER TABLE `test_plan_api_case` DROP INDEX planIdIndex;
    END IF;
END//
DELIMITER ;
CALL schema_change_api_one();

DROP PROCEDURE IF EXISTS schema_change_scenario;
DELIMITER //
CREATE PROCEDURE schema_change_scenario() BEGIN
    DECLARE  CurrentDatabase VARCHAR(100);
    SELECT DATABASE() INTO CurrentDatabase;
    IF NOT EXISTS (SELECT * FROM information_schema.statistics WHERE table_schema=CurrentDatabase AND table_name = 'test_plan_api_scenario' AND index_name = 'plan_id_index') THEN
        ALTER TABLE `test_plan_api_scenario` ADD INDEX plan_id_index ( `test_plan_id` );
    END IF;
END//
DELIMITER ;
CALL schema_change_scenario();

DROP PROCEDURE IF EXISTS schema_change_scenario_one;
DELIMITER //
CREATE PROCEDURE schema_change_scenario_one() BEGIN
    DECLARE  CurrentDatabase VARCHAR(100);
    SELECT DATABASE() INTO CurrentDatabase;
    IF  EXISTS (SELECT * FROM information_schema.statistics WHERE table_schema=CurrentDatabase AND table_name = 'test_plan_api_scenario' AND index_name = 'planIdIndex') THEN
        ALTER TABLE `test_plan_api_scenario` DROP INDEX planIdIndex;
    END IF;
END//
DELIMITER ;
CALL schema_change_scenario_one();


DROP PROCEDURE IF EXISTS schema_change_load;
DELIMITER //
CREATE PROCEDURE schema_change_load() BEGIN
    DECLARE  CurrentDatabase VARCHAR(100);
    SELECT DATABASE() INTO CurrentDatabase;
    IF NOT EXISTS (SELECT * FROM information_schema.statistics WHERE table_schema=CurrentDatabase AND table_name = 'test_plan_load_case' AND index_name = 'plan_id_index') THEN
        ALTER TABLE `test_plan_load_case` ADD INDEX plan_id_index ( `test_plan_id` );
    END IF;
END//
DELIMITER ;
CALL schema_change_load();

DROP PROCEDURE IF EXISTS schema_change_load_one;
DELIMITER //
CREATE PROCEDURE schema_change_load_one() BEGIN
    DECLARE  CurrentDatabase VARCHAR(100);
    SELECT DATABASE() INTO CurrentDatabase;
    IF  EXISTS (SELECT * FROM information_schema.statistics WHERE table_schema=CurrentDatabase AND table_name = 'test_plan_load_case' AND index_name = 'planIdIndex') THEN
        ALTER TABLE `test_plan_load_case` DROP INDEX planIdIndex;
    END IF;
END//
DELIMITER ;
CALL schema_change_load_one();


DROP PROCEDURE IF EXISTS schema_change_report;
DELIMITER //
CREATE PROCEDURE schema_change_report() BEGIN
    DECLARE  CurrentDatabase VARCHAR(100);
    SELECT DATABASE() INTO CurrentDatabase;
    IF NOT EXISTS (SELECT * FROM information_schema.statistics WHERE table_schema=CurrentDatabase AND table_name = 'test_plan_report' AND index_name = 'plan_id_index') THEN
        ALTER TABLE `test_plan_report` ADD INDEX plan_id_index ( `test_plan_id` );
    END IF;
END//
DELIMITER ;
CALL schema_change_report();

DROP PROCEDURE IF EXISTS schema_change_report_one;
DELIMITER //
CREATE PROCEDURE schema_change_report_one() BEGIN
    DECLARE  CurrentDatabase VARCHAR(100);
    SELECT DATABASE() INTO CurrentDatabase;
    IF  EXISTS (SELECT * FROM information_schema.statistics WHERE table_schema=CurrentDatabase AND table_name = 'test_plan_report' AND index_name = 'planIdIndex') THEN
        ALTER TABLE `test_plan_report` DROP INDEX planIdIndex;
    END IF;
END//
DELIMITER ;
CALL schema_change_report_one();


DROP PROCEDURE IF EXISTS schema_change_issue;
DELIMITER //
CREATE PROCEDURE schema_change_issue() BEGIN
    DECLARE  CurrentDatabase VARCHAR(100);
    SELECT DATABASE() INTO CurrentDatabase;
    IF NOT EXISTS (SELECT * FROM information_schema.statistics WHERE table_schema=CurrentDatabase AND table_name = 'test_case_issues' AND index_name = 'issues_id_index') THEN
        ALTER TABLE `test_case_issues` ADD INDEX issues_id_index ( `issues_id` );
    END IF;
END//
DELIMITER ;
CALL schema_change_issue();

DROP PROCEDURE IF EXISTS schema_change;
DELIMITER //
CREATE PROCEDURE schema_change() BEGIN
    DECLARE  CurrentDatabase VARCHAR(100);
    SELECT DATABASE() INTO CurrentDatabase;
    IF NOT EXISTS (SELECT * FROM information_schema.statistics WHERE table_schema=CurrentDatabase AND table_name = 'api_scenario_report' AND index_name = 'update_time_index') THEN
        ALTER TABLE `api_scenario_report` ADD INDEX update_time_index ( `update_time` );
    END IF;
END//
DELIMITER ;
CALL schema_change();

-- csv版本git管理
INSERT INTO user_group_permission (id, group_id, permission_id, module_id)
VALUES (uuid(), 'ws_admin', 'WORKSPACE_REPOSITORY:READ', 'WORKSPACE_USER');

INSERT INTO user_group_permission (id, group_id, permission_id, module_id)
VALUES (uuid(), 'ws_member', 'WORKSPACE_REPOSITORY:READ', 'WORKSPACE_USER');

INSERT INTO user_group_permission (id, group_id, permission_id, module_id)
VALUES (uuid(), 'ws_admin', 'WORKSPACE_REPOSITORY:READ+CREATE', 'WORKSPACE_USER');

INSERT INTO user_group_permission (id, group_id, permission_id, module_id)
VALUES (uuid(), 'ws_admin', 'WORKSPACE_REPOSITORY:READ+EDIT', 'WORKSPACE_USER');

INSERT INTO user_group_permission (id, group_id, permission_id, module_id)
VALUES (uuid(), 'ws_admin', 'WORKSPACE_REPOSITORY:READ+DELETE', 'WORKSPACE_USER');

DROP TABLE IF EXISTS `workspace_repository`;
CREATE TABLE IF NOT EXISTS `workspace_repository`
(
    `id` varchar(50) NOT NULL COMMENT 'Repository ID',
    `repository_name` varchar(100) NOT NULL COMMENT '存储库名称',
    `repository_url` varchar(300)  NOT NULL COMMENT '存储库地址',
    `username` varchar(256)  NOT NULL COMMENT 'UserName',
    `password` varchar(256)  NOT NULL COMMENT 'Password',
    `create_time` bigint(13) NOT NULL COMMENT 'Create timestamp',
    `update_time` bigint(13) NOT NULL COMMENT 'Update timestamp',
    `workspace_id` varchar(50) DEFAULT NULL COMMENT '工作空间ID',
    `create_user` varchar(100) DEFAULT NULL COMMENT '创建人',
    `description` longtext COMMENT '仓库描述信息',
    PRIMARY KEY (`id`)
    ) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;


DROP TABLE IF EXISTS `workspace_repository_file_version`;
CREATE TABLE `workspace_repository_file_version` (
                                                     `id` varchar(50) NOT NULL COMMENT 'ID',
                                                     `repository_id` varchar(50) NOT NULL COMMENT '存储库ID',
                                                     `branch` varchar(100) NOT NULL COMMENT '存储库分支',
                                                     `path` varchar(500) NOT NULL COMMENT '文件路径',
                                                     `scenario_id` varchar(100) NOT NULL COMMENT '场景ID',
                                                     `create_time` bigint(13) NOT NULL COMMENT 'Create timestamp',
                                                     `update_time` bigint(13) NOT NULL COMMENT 'Update timestamp',
                                                     `create_user` varchar(100) DEFAULT NULL COMMENT '创建人',
                                                     `commit_id` varchar(100) NOT NULL COMMENT '文件commentId',
                                                     PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT=DYNAMIC;