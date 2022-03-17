ALTER TABLE `api_definition_exec_result`
    ADD INDEX projectIdIndex (`project_id`);
ALTER TABLE `api_scenario_report`
    ADD INDEX projectIdIndex (`project_id`);
ALTER TABLE `api_scenario_report`
    ADD INDEX projectIdexectypeIndex (`project_id`, `execute_type`);
-- module management
INSERT INTO system_parameter (param_key, param_value, type, sort)
VALUES ('metersphere.module.workstation', 'ENABLE', 'text', 1);
INSERT INTO user_group_permission (id, group_id, permission_id, module_id)
VALUES (uuid(), 'ws_admin', 'WORKSPACE_TEMPLATE:READ+API_TEMPLATE', 'WORKSPACE_TEMPLATE');
