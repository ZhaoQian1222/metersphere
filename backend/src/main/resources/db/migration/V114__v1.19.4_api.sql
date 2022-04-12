ALTER TABLE api_template
    ADD project_id VARCHAR(64) NULL;

ALTER TABLE api_template
    MODIFY id VARCHAR(100);


UPDATE api_template
SET project_id = 'global'
WHERE global = 1;

DROP PROCEDURE IF EXISTS api_template_ws;
DELIMITER //
CREATE PROCEDURE api_template_ws()
BEGIN

    DECLARE templateId VARCHAR(64);
    DECLARE name VARCHAR(64);
    DECLARE type VARCHAR(64);
    DECLARE description VARCHAR(64);
    DECLARE `system` TINYINT(1);
    DECLARE global TINYINT(1);
    DECLARE workspaceId VARCHAR(64);
    DECLARE apiName VARCHAR(64);
    DECLARE apiMethod VARCHAR(64);
    DECLARE apiPath VARCHAR(1000);
    DECLARE createTime BIGINT;
    DECLARE updateTime BIGINT;
    DECLARE createUser VARCHAR(64);

    DECLARE done INT DEFAULT 0;
    # 必须用 table_name.column_name
    DECLARE cursor1 CURSOR FOR SELECT api_template.id,
                                      api_template.name,
                                      api_template.type,
                                      api_template.description,
                                      api_template.`system`,
                                      api_template.global,
                                      api_template.workspace_id,
                                      api_template.api_name,
                                      api_template.api_method,
                                      api_template.api_path,
                                      api_template.create_time,
                                      api_template.update_time,
                                      api_template.create_user
                               FROM api_template
                               WHERE api_template.global = 0;


    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    OPEN cursor1;
    outer_loop:
    LOOP
        FETCH cursor1 INTO templateId, name, type, description, `system`, global, workspaceId,
            apiName,apiMethod,apiPath,createTime,updateTime, createUser;
        IF done
        THEN
            LEAVE outer_loop;
        END IF;

        -- 自定义字段数据下发到项目
        INSERT INTO api_template (id, name, type, description, `system`, global, workspace_id,
                                        api_name,api_method, api_path, create_time,
                                        update_time, create_user, project_id)
        SELECT CONCAT(templateId, '-', SUBSTRING(MD5(RAND()), 1, 10)),
               api_template.name,
               api_template.type,
               api_template.description,
               api_template.`system`,
               api_template.global,
               api_template.workspace_id,
               api_template.api_name,
               api_template.api_method,
               api_template.api_path,
               api_template.create_time,
               api_template.update_time,
               api_template.create_user,
               project.id
        FROM project
                 JOIN api_template ON project.workspace_id = api_template.workspace_id
        WHERE api_template.id = templateId;
        -- 删除处理过的数据
        DELETE FROM api_template WHERE id = templateId;

    END LOOP;
    CLOSE cursor1;
END
//
DELIMITER ;

CALL api_template_ws();
DROP PROCEDURE IF EXISTS api_template_ws;


ALTER TABLE api_template
    DROP COLUMN workspace_id;


UPDATE project
    JOIN api_template ON project.id = api_template.project_id
SET project.api_template_id = api_template.id
WHERE api_template_id IS NOT NULL
  AND api_template.id LIKE CONCAT(api_template_id, '%');


UPDATE project JOIN api_template ON project.id = api_template.project_id
SET api_template_id = api_template.id
WHERE api_template_id IS NULL AND api_template.`system` = 1;

INSERT INTO user_group_permission (id, group_id, permission_id, module_id)
VALUES (uuid(), 'project_admin', 'PROJECT_TEMPLATE:READ+API_TEMPLATE', 'PROJECT_TEMPLATE');
