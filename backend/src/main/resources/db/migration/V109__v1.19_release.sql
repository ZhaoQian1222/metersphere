insert into project_application(project_id, type, type_value)
select id, 'CASE_CUSTOM_NUM', if(custom_num, 'true', 'false')
from project
where not exists(select * from project_application where project_id = id and type = 'CASE_CUSTOM_NUM');

insert into project_application(project_id, type, type_value)
select id, 'SCENARIO_CUSTOM_NUM', if(scenario_custom_num, 'true', 'false')
from project
where not exists(select * from project_application where project_id = id and type = 'SCENARIO_CUSTOM_NUM');

insert into project_application(project_id, type, type_value)
select id, 'API_QUICK_MENU', api_quick
from project
where api_quick is not null
  and not exists(select * from project_application where project_id = id and type = 'API_QUICK_MENU');


insert into project_application(project_id, type, type_value)
select id, 'CASE_PUBLIC', if(case_public, 'true', 'false')
from project
where not exists(select * from project_application where project_id = id and type = 'CASE_PUBLIC');


insert into project_application(project_id, type, type_value)
select id, 'MOCK_TCP_PORT', mock_tcp_port
from project
where not exists(select * from project_application where project_id = id and type = 'MOCK_TCP_PORT');


insert into project_application(project_id, type, type_value)
select id, 'MOCK_TCP_OPEN', if(is_mock_tcp_open, 'true', 'false')
from project
where not exists(select * from project_application where project_id = id and type = 'MOCK_TCP_OPEN');


insert into project_application(project_id, type, type_value)
select id, 'CLEAN_TRACK_REPORT', if(clean_api_report, 'true', 'false')
from project
where not exists(select * from project_application where project_id = id and type = 'CLEAN_TRACK_REPORT');


insert into project_application(project_id, type, type_value)
select id, 'CLEAN_TRACK_REPORT_EXPR', clean_track_report_expr
from project
where clean_track_report_expr is not null
  and not exists(select * from project_application where project_id = id and type = 'CLEAN_TRACK_REPORT_EXPR');

insert into project_application(project_id, type, type_value)
select id, 'CLEAN_API_REPORT', if(clean_api_report, 'true', 'false')
from project
where not exists(select * from project_application where project_id = id and type = 'CLEAN_API_REPORT');


insert into project_application(project_id, type, type_value)
select id, 'CLEAN_API_REPORT_EXPR', clean_api_report_expr
from project
where clean_api_report_expr is not null
  and not exists(select * from project_application where project_id = id and type = 'CLEAN_API_REPORT_EXPR');

insert into project_application(project_id, type, type_value)
select id, 'CLEAN_LOAD_REPORT', if(clean_load_report, 'true', 'false')
from project
where not exists(select * from project_application where project_id = id and type = 'CLEAN_LOAD_REPORT');

insert into project_application(project_id, type, type_value)
select id, 'CLEAN_LOAD_REPORT_EXPR', clean_load_report_expr
from project
where clean_load_report_expr is not null
  and not exists(select * from project_application where project_id = id and type = 'CLEAN_LOAD_REPORT_EXPR');

insert into project_application(project_id, type, type_value)
select id, 'URL_REPEATABLE', if(repeatable, 'true', 'false')
from project
where not exists(select * from project_application where project_id = id and type = 'URL_REPEATABLE');
-- module management
INSERT INTO system_parameter (param_key, param_value, type, sort)
VALUES ('metersphere.module.workstation', 'ENABLE', 'text', 1);

DROP PROCEDURE IF EXISTS project_api_appl;
DELIMITER //
CREATE PROCEDURE project_api_appl()
BEGIN
    #声明结束标识
    DECLARE end_flag int DEFAULT 0;

    DECLARE projectId varchar(64);

    #声明游标 group_curosr
    DECLARE project_curosr CURSOR FOR SELECT DISTINCT id FROM project;

#设置终止标志
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET end_flag = 1;

    #打开游标
    OPEN project_curosr;
    #获取当前游标指针记录，取出值赋给自定义的变量
    FETCH project_curosr INTO projectId;
    #遍历游标
    REPEAT
        #利用取到的值进行数据库的操作
        INSERT INTO project_application (project_id, type, type_value)
        VALUES (projectId, 'API_SHARE_REPORT_TIME', '24H');
        # 将游标中的值再赋值给变量，供下次循环使用
        FETCH project_curosr INTO projectId;
    UNTIL end_flag END REPEAT;

    #关闭游标
    CLOSE project_curosr;

END
//
DELIMITER ;

CALL project_api_appl();
DROP PROCEDURE IF EXISTS project_api_appl;