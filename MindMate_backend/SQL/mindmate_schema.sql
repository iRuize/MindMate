-- ===========================================================
-- MindMate 建表脚本（达梦 DM8）
--
-- 本文件是 MINDMATE 模式的权威 DDL，执行后即得到项目所需的全部表结构。
-- 正文只含 CREATE TABLE / CREATE INDEX / COMMENT ON，
-- 可在 DIsql、SQLark、DM 管理工具、Navicat 达梦连接等任意客户端直接执行。
--
-- 前置条件：模式 MINDMATE 必须已存在。首次部署时先单独执行一次：
--
--     CREATE SCHEMA MINDMATE;
--
--   用 DIsql 执行上面这句时，必须在其后另起一行写一个斜杠，
--   否则 DIsql 不认为语句结束，会把后面的内容全部吞掉：
--
--     CREATE SCHEMA MINDMATE;
--     /
--
--   正因这个斜杠在图形化客户端里是语法错误，模式创建没有并入正文，
--   以保证本文件跨客户端可执行。模式创建只需一次，不必重复。
--
-- 设计说明
--   1. 任务采用「快照表 TASK + 事件流 TASK_EVENT」双写：
--      快照负责列表查询，事件流负责保留行为历史，两者在同一事务内写入。
--   2. 行为埋点字段（耗时、推迟次数、首次计划日）必须一次性建齐，
--      上线后再补则历史数据无法回补，而 AI 决策质量完全依赖历史积累。
--   3. 派生值不入库：紧迫度、四象限由 DUE_AT 在决策时现算，避免与源字段不同步。
--   4. 不建物理外键，关联完整性由应用层保证，避免后续分库受阻。
--
-- 达梦适配要点（这是达梦语法，不能拿到 MySQL 连接上执行）
--   表名用 SYS_USER 而非 user：USER 是达梦保留字，未加引号不能作为表名，
--   而 MyBatis-Plus 生成的 SQL 不会给表名加引号。
--   AUTO_INCREMENT -> IDENTITY(1,1)
--   JSON -> CLOB（达梦无原生 JSON 类型，载荷由应用层序列化）
--   ON UPDATE CURRENT_TIMESTAMP 不支持，更新时间由应用层维护
--   DATETIME(3) -> TIMESTAMP(3)，毫秒精度保留
--
-- 编码：本文件为 UTF-8。若用 DIsql 执行须先转码，否则中文注释会乱码：
--   iconv -f UTF-8 -t GBK mindmate_schema.sql > /tmp/run.sql
--   DIsql SYSDBA/密码@localhost:5236 < /tmp/run.sql
-- ===========================================================

-- -----------------------------------------------------------
-- 用户表
-- -----------------------------------------------------------
CREATE TABLE "MINDMATE"."SYS_USER"
(
    "USER_ID"                BIGINT IDENTITY(1, 1) NOT NULL,
    "PHONE_NUMBER"           VARCHAR(20)  NOT NULL,
    "USERNAME"               VARCHAR(50),
    "AVATAR_URL"             VARCHAR(255),
    "PERSONALIZED_SIGNATURE" VARCHAR(255),
    "EMAIL"                  VARCHAR(100),
    "CREATED_AT"             TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "UPDATED_AT"             TIMESTAMP,
    "LAST_LOGIN_AT"          TIMESTAMP,
    NOT CLUSTER PRIMARY KEY ("USER_ID"),
    CONSTRAINT "UK_SYS_USER_PHONE" UNIQUE ("PHONE_NUMBER")
) STORAGE(ON "MAIN", CLUSTERBTR);

COMMENT ON TABLE "MINDMATE"."SYS_USER" IS '用户表';
COMMENT ON COLUMN "MINDMATE"."SYS_USER"."USER_ID" IS '用户ID';
COMMENT ON COLUMN "MINDMATE"."SYS_USER"."PHONE_NUMBER" IS '手机号，全局唯一，作为登录账号';
COMMENT ON COLUMN "MINDMATE"."SYS_USER"."USERNAME" IS '用户名';
COMMENT ON COLUMN "MINDMATE"."SYS_USER"."AVATAR_URL" IS '头像公开访问路径';
COMMENT ON COLUMN "MINDMATE"."SYS_USER"."PERSONALIZED_SIGNATURE" IS '个性签名';
COMMENT ON COLUMN "MINDMATE"."SYS_USER"."EMAIL" IS '电子邮箱';
COMMENT ON COLUMN "MINDMATE"."SYS_USER"."CREATED_AT" IS '创建时间';
COMMENT ON COLUMN "MINDMATE"."SYS_USER"."UPDATED_AT" IS '更新时间';
COMMENT ON COLUMN "MINDMATE"."SYS_USER"."LAST_LOGIN_AT" IS '最近登录时间';

-- -----------------------------------------------------------
-- 任务表（当前状态快照）
-- -----------------------------------------------------------
CREATE TABLE "MINDMATE"."TASK"
(
    "ID"                 BIGINT IDENTITY(1, 1) NOT NULL,
    "USER_ID"            BIGINT       NOT NULL,
    "TITLE"              VARCHAR(200) NOT NULL,
    "DESCRIPTION"        CLOB,
    "STATUS"             TINYINT DEFAULT 0 NOT NULL,
    "PRIORITY"           TINYINT DEFAULT 1 NOT NULL,
    "PLANNED_DATE"       DATE,
    "DUE_AT"             TIMESTAMP,
    "ESTIMATED_MINUTES"  INT,
    "ACTUAL_MINUTES"     INT,
    "STARTED_AT"         TIMESTAMP,
    "COMPLETED_AT"       TIMESTAMP,
    "DEFER_COUNT"        INT DEFAULT 0 NOT NULL,
    "FIRST_PLANNED_DATE" DATE,
    "LAST_DEFERRED_AT"   TIMESTAMP,
    "SOURCE"             TINYINT DEFAULT 1 NOT NULL,
    "CREATED_AT"         TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "UPDATED_AT"         TIMESTAMP,
    "DELETED"            TINYINT DEFAULT 0 NOT NULL,
    NOT CLUSTER PRIMARY KEY ("ID")
) STORAGE(ON "MAIN", CLUSTERBTR);

COMMENT ON TABLE "MINDMATE"."TASK" IS '任务表（当前状态快照）';
COMMENT ON COLUMN "MINDMATE"."TASK"."ID" IS '任务ID';
COMMENT ON COLUMN "MINDMATE"."TASK"."USER_ID" IS '归属用户ID';
COMMENT ON COLUMN "MINDMATE"."TASK"."TITLE" IS '任务标题';
COMMENT ON COLUMN "MINDMATE"."TASK"."DESCRIPTION" IS '任务描述';
COMMENT ON COLUMN "MINDMATE"."TASK"."STATUS" IS '状态：0待办 1进行中 2已完成 3已取消 4已归档';
COMMENT ON COLUMN "MINDMATE"."TASK"."PRIORITY" IS '优先级：0低 1中 2高 3紧急';
COMMENT ON COLUMN "MINDMATE"."TASK"."PLANNED_DATE" IS '计划执行日';
COMMENT ON COLUMN "MINDMATE"."TASK"."DUE_AT" IS '截止时间';
COMMENT ON COLUMN "MINDMATE"."TASK"."ESTIMATED_MINUTES" IS '预估耗时（分钟）';
COMMENT ON COLUMN "MINDMATE"."TASK"."ACTUAL_MINUTES" IS '实际耗时（分钟）';
COMMENT ON COLUMN "MINDMATE"."TASK"."STARTED_AT" IS '首次开始时间';
COMMENT ON COLUMN "MINDMATE"."TASK"."COMPLETED_AT" IS '完成时间';
COMMENT ON COLUMN "MINDMATE"."TASK"."DEFER_COUNT" IS '累计推迟次数';
COMMENT ON COLUMN "MINDMATE"."TASK"."FIRST_PLANNED_DATE" IS '首次计划执行日，与 PLANNED_DATE 差值即推迟跨度';
COMMENT ON COLUMN "MINDMATE"."TASK"."LAST_DEFERRED_AT" IS '最后一次推迟的操作时间';
COMMENT ON COLUMN "MINDMATE"."TASK"."SOURCE" IS '来源：1用户创建 2AI拆分 3AI建议采纳';
COMMENT ON COLUMN "MINDMATE"."TASK"."CREATED_AT" IS '创建时间';
COMMENT ON COLUMN "MINDMATE"."TASK"."UPDATED_AT" IS '更新时间';
COMMENT ON COLUMN "MINDMATE"."TASK"."DELETED" IS '逻辑删除：0正常 1已删除';

CREATE INDEX "IDX_TASK_USER_STATUS_PLAN" ON "MINDMATE"."TASK" ("USER_ID", "STATUS", "PLANNED_DATE");
CREATE INDEX "IDX_TASK_USER_DUE" ON "MINDMATE"."TASK" ("USER_ID", "DUE_AT");

-- -----------------------------------------------------------
-- 任务事件流（不可变，行为历史的唯一事实来源）
-- -----------------------------------------------------------
CREATE TABLE "MINDMATE"."TASK_EVENT"
(
    "ID"          BIGINT IDENTITY(1, 1) NOT NULL,
    "TASK_ID"     BIGINT      NOT NULL,
    "USER_ID"     BIGINT      NOT NULL,
    "EVENT_TYPE"  VARCHAR(32) NOT NULL,
    "PAYLOAD"     CLOB,
    "OCCURRED_AT" TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "CREATED_AT"  TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    NOT CLUSTER PRIMARY KEY ("ID")
) STORAGE(ON "MAIN", CLUSTERBTR);

COMMENT ON TABLE "MINDMATE"."TASK_EVENT" IS '任务事件流（不可变，行为历史的唯一事实来源）';
COMMENT ON COLUMN "MINDMATE"."TASK_EVENT"."ID" IS '事件ID';
COMMENT ON COLUMN "MINDMATE"."TASK_EVENT"."TASK_ID" IS '任务ID';
COMMENT ON COLUMN "MINDMATE"."TASK_EVENT"."USER_ID" IS '归属用户ID，冗余存储以支持按用户全量扫事件流';
COMMENT ON COLUMN "MINDMATE"."TASK_EVENT"."EVENT_TYPE" IS '事件类型：CREATED/SCHEDULED/DEFERRED/STARTED/COMPLETED/REOPENED/CANCELLED/DELETED/UPDATED/SUGGESTED';
COMMENT ON COLUMN "MINDMATE"."TASK_EVENT"."PAYLOAD" IS '事件载荷：变更字段的前后值，或事件附加上下文（JSON 文本）';
COMMENT ON COLUMN "MINDMATE"."TASK_EVENT"."OCCURRED_AT" IS '事件发生时间，毫秒精度保证同秒内排序稳定';
COMMENT ON COLUMN "MINDMATE"."TASK_EVENT"."CREATED_AT" IS '入库时间';

CREATE INDEX "IDX_TASK_EVENT_TASK_TIME" ON "MINDMATE"."TASK_EVENT" ("TASK_ID", "OCCURRED_AT");
CREATE INDEX "IDX_TASK_EVENT_USER_TYPE_TIME" ON "MINDMATE"."TASK_EVENT" ("USER_ID", "EVENT_TYPE", "OCCURRED_AT");
