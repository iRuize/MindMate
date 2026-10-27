-- ===========================================================
-- 达梦数据库 · 新建 MINDMATE 模式与表结构
--
-- 设计依据：SQL/v2_task_model.sql（MySQL 版）的达梦适配
--   1. 快照表 TASK + 事件流 TASK_EVENT 双写，快照负责查询、事件流负责历史
--   2. 行为埋点字段一次建齐（耗时、推迟次数、首次计划日），历史不可回补
--   3. 派生值不入库（紧迫度、四象限由 DUE_AT 在决策时现算）
--   4. 不建物理外键，关联完整性由应用层保证
--
-- 相对 MySQL 版的适配点：
--   表名 user -> SYS_USER        USER 是达梦保留字，不加引号无法作为表名，且 MyBatis-Plus
--                                生成的 SQL 不会给表名加引号，故改用非保留字命名
--   AUTO_INCREMENT -> IDENTITY(1,1)
--   去掉 ENGINE / CHARSET 子句     达梦无存储引擎与字符集概念，字符集由实例统一决定
--   JSON -> CLOB                   达梦无原生 JSON 类型，载荷存 CLOB 由应用层序列化
--   ON UPDATE CURRENT_TIMESTAMP    达梦不支持，更新时间由 MyBatis-Plus 自动填充维护
--   DATETIME(3) -> TIMESTAMP(3)    毫秒精度保留
--
-- 执行编码：iconv -f UTF-8 -t GBK
-- 注意：CREATE SCHEMA 必须以 / 终止，见 01 脚本头部说明
-- ===========================================================

-- -----------------------------------------------------------
-- 第 1 步  新建模式
-- -----------------------------------------------------------
CREATE SCHEMA MINDMATE;
/

-- -----------------------------------------------------------
-- 第 2 步  用户表
-- -----------------------------------------------------------
CREATE TABLE "MINDMATE"."SYS_USER"
(
    "USER_ID"                BIGINT IDENTITY(1, 1) NOT NULL,
    "PHONE_NUMBER"           VARCHAR(20)  NOT NULL,
    "USERNAME"               VARCHAR(50),
    "REAL_NAME"              VARCHAR(50),
    "PERSONALIZED_SIGNATURE" VARCHAR(255),
    "EMAIL"                  VARCHAR(100),
    "CREATED_AT"             TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "UPDATED_AT"             TIMESTAMP,
    NOT CLUSTER PRIMARY KEY ("USER_ID"),
    CONSTRAINT "UK_SYS_USER_PHONE" UNIQUE ("PHONE_NUMBER")
) STORAGE(ON "MAIN", CLUSTERBTR);

COMMENT ON TABLE "MINDMATE"."SYS_USER" IS '用户表';
COMMENT ON COLUMN "MINDMATE"."SYS_USER"."USER_ID" IS '用户ID';
COMMENT ON COLUMN "MINDMATE"."SYS_USER"."PHONE_NUMBER" IS '手机号，全局唯一，作为登录账号';
COMMENT ON COLUMN "MINDMATE"."SYS_USER"."USERNAME" IS '用户名';
COMMENT ON COLUMN "MINDMATE"."SYS_USER"."REAL_NAME" IS '真实姓名';
COMMENT ON COLUMN "MINDMATE"."SYS_USER"."PERSONALIZED_SIGNATURE" IS '个性签名';
COMMENT ON COLUMN "MINDMATE"."SYS_USER"."EMAIL" IS '电子邮箱';
COMMENT ON COLUMN "MINDMATE"."SYS_USER"."CREATED_AT" IS '创建时间';
COMMENT ON COLUMN "MINDMATE"."SYS_USER"."UPDATED_AT" IS '更新时间';

-- -----------------------------------------------------------
-- 第 3 步  任务表（当前状态快照）
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
-- 第 4 步  任务事件流（不可变，行为历史的唯一事实来源）
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
