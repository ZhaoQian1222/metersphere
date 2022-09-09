-- 初始化 sql
-- V122_2-1-0_swagger_url_project

ALTER TABLE swagger_url_project ADD COLUMN cover_module TINYINT(1) DEFAULT 0 COMMENT '是否覆盖模块';
update swagger_url_project set mode_id='incrementalMerge' where mode_id='不覆盖';


--issue_template表添加天玑模板
insert into issue_template (id,name,platform,description,title,`system`,`global`,workspace_id,content,create_time,update_time)
values ('f2cd9e48-f136-4528-8249-a649c15aa399','天玑-默认模版','Yunxiao','Yunxiao默认模版','',1,1,'global','',unix_timestamp() * 1000,unix_timestamp() * 1000);

--project表添加项目key字段
ALTER TABLE project ADD yunxiao_key varchar(50);