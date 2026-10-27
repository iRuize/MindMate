-- ===========================================================
-- 达梦数据库 · 模式改名迁移
-- 目标：SOFTDEMO -> ONEPAGE
--
-- 背景：达梦不支持 ALTER SCHEMA ... RENAME（本实例 8.1.3.62 语法解析报 -2007），
--       也无对应的系统存储过程，因此采用「新建模式 -> 重建对象 -> 迁移数据 -> 删除旧模式」路径。
--
-- 迁移对象清单（来自 dba_objects 实查）：
--   表   4    SYS_USER / SYS_DIARY / SYS_ESSAY / SYS_BILL
--   索引 12   其中 4 个主键索引 + 1 个唯一约束索引随建表自动生成，
--             3 个业务索引需手工重建：IDX_BILL_USER_DATE / IDX_DIARY_USER_CREATED / IDX_ESSAY_USER_CREATED
--   约束 5    4 个主键 + UK_SYS_USER_USERNAME
--   注释      4 张表 + 26 个列
--   无视图、无序列、无触发器
--
-- 编码：DIsql 按 GBK 读取脚本，本文件为 UTF-8，执行前须 iconv -f UTF-8 -t GBK 转码。
--       另注意：脚本中任何位置（包括行注释内）都不可出现块注释定界符，
--       DIsql 会无视行注释上下文，扫描到即进入块注释状态，导致整份脚本被吞。
--       同理，CREATE SCHEMA 必须另起一行以 / 终止：分号不足以让 DIsql 结束该语句，
--       否则后续整份脚本会被当作未结束语句而全部不执行（本脚本已按此处理）。
-- 顺序：本文件 -> 校验 -> 02_drop_softdemo.sql
-- ===========================================================

-- -----------------------------------------------------------
-- 第 1 步  新建模式
--   注意：末尾的 / 不可删除
-- -----------------------------------------------------------
CREATE SCHEMA ONEPAGE;
/

-- -----------------------------------------------------------
-- 第 2 步  重建表结构（字段、类型、主键、唯一约束与源库一致）
-- -----------------------------------------------------------
CREATE TABLE "ONEPAGE"."SYS_USER"
(
    "ID"            BIGINT IDENTITY(1, 1) NOT NULL,
    "USERNAME"      VARCHAR(20)  NOT NULL,
    "NICKNAME"      VARCHAR(32),
    "AVATAR"        VARCHAR(16),
    "PASSWORD_HASH" VARCHAR(255) NOT NULL,
    "CREATED_AT"    BIGINT,
    "LAST_LOGIN_AT" BIGINT,
    NOT CLUSTER PRIMARY KEY ("ID"),
    CONSTRAINT "UK_SYS_USER_USERNAME" UNIQUE ("USERNAME")
) STORAGE(ON "MAIN", CLUSTERBTR);

CREATE TABLE "ONEPAGE"."SYS_DIARY"
(
    "ID"         BIGINT IDENTITY(1, 1) NOT NULL,
    "USER_ID"    BIGINT       NOT NULL,
    "TITLE"      VARCHAR(100) NOT NULL,
    "CONTENT"    CLOB,
    "MOOD"       VARCHAR(20),
    "IMAGES"     VARCHAR(2000),
    "CREATED_AT" BIGINT       NOT NULL,
    "UPDATED_AT" BIGINT,
    NOT CLUSTER PRIMARY KEY ("ID")
) STORAGE(ON "MAIN", CLUSTERBTR);

CREATE TABLE "ONEPAGE"."SYS_ESSAY"
(
    "ID"         BIGINT IDENTITY(1, 1) NOT NULL,
    "USER_ID"    BIGINT       NOT NULL,
    "TITLE"      VARCHAR(100) NOT NULL,
    "CONTENT"    CLOB,
    "CREATED_AT" BIGINT       NOT NULL,
    "UPDATED_AT" BIGINT,
    NOT CLUSTER PRIMARY KEY ("ID")
) STORAGE(ON "MAIN", CLUSTERBTR);

CREATE TABLE "ONEPAGE"."SYS_BILL"
(
    "ID"         BIGINT        IDENTITY(1, 1) NOT NULL,
    "USER_ID"    BIGINT        NOT NULL,
    "TYPE"       SMALLINT      NOT NULL,
    "AMOUNT"     DECIMAL(12, 2) NOT NULL,
    "CATEGORY"   VARCHAR(32),
    "REMARK"     VARCHAR(255),
    "BILL_DATE"  BIGINT        NOT NULL,
    "CREATED_AT" BIGINT        NOT NULL,
    "UPDATED_AT" BIGINT,
    NOT CLUSTER PRIMARY KEY ("ID")
) STORAGE(ON "MAIN", CLUSTERBTR);

-- -----------------------------------------------------------
-- 第 3 步  重建业务索引（主键与唯一约束索引已随建表生成，无需重复创建）
-- -----------------------------------------------------------
CREATE INDEX "IDX_BILL_USER_DATE" ON "ONEPAGE"."SYS_BILL" ("USER_ID", "BILL_DATE");
CREATE INDEX "IDX_DIARY_USER_CREATED" ON "ONEPAGE"."SYS_DIARY" ("USER_ID", "CREATED_AT");
CREATE INDEX "IDX_ESSAY_USER_CREATED" ON "ONEPAGE"."SYS_ESSAY" ("USER_ID", "CREATED_AT");

-- -----------------------------------------------------------
-- 第 4 步  迁移表注释
-- -----------------------------------------------------------
COMMENT ON TABLE "ONEPAGE"."SYS_USER" IS '用户表：登录注册账号信息';
COMMENT ON TABLE "ONEPAGE"."SYS_DIARY" IS '日记表：用户日记内容与图片';
COMMENT ON TABLE "ONEPAGE"."SYS_ESSAY" IS '随笔表：用户的短篇随笔';
COMMENT ON TABLE "ONEPAGE"."SYS_BILL" IS '账本表：用户收支记录';

-- -----------------------------------------------------------
-- 第 5 步  迁移列注释
-- -----------------------------------------------------------
COMMENT ON COLUMN "ONEPAGE"."SYS_USER"."ID" IS '用户主键，自增';
COMMENT ON COLUMN "ONEPAGE"."SYS_USER"."USERNAME" IS '登录用户名，全局唯一';
COMMENT ON COLUMN "ONEPAGE"."SYS_USER"."NICKNAME" IS '用户昵称，展示用';
COMMENT ON COLUMN "ONEPAGE"."SYS_USER"."AVATAR" IS '头像标识（emoji）';
COMMENT ON COLUMN "ONEPAGE"."SYS_USER"."PASSWORD_HASH" IS 'PBKDF2 密码摘要，不保存明文密码';
COMMENT ON COLUMN "ONEPAGE"."SYS_USER"."CREATED_AT" IS '创建时间（毫秒时间戳）';
COMMENT ON COLUMN "ONEPAGE"."SYS_USER"."LAST_LOGIN_AT" IS '最近登录时间（毫秒时间戳）';

COMMENT ON COLUMN "ONEPAGE"."SYS_DIARY"."ID" IS '日记主键，自增';
COMMENT ON COLUMN "ONEPAGE"."SYS_DIARY"."USER_ID" IS '所属用户 ID，关联 sys_user.id';
COMMENT ON COLUMN "ONEPAGE"."SYS_DIARY"."TITLE" IS '日记标题';
COMMENT ON COLUMN "ONEPAGE"."SYS_DIARY"."CONTENT" IS '日记正文';
COMMENT ON COLUMN "ONEPAGE"."SYS_DIARY"."MOOD" IS '心情标识（calm/happy/down/tired/hope）';
COMMENT ON COLUMN "ONEPAGE"."SYS_DIARY"."IMAGES" IS '图片地址，多个以英文逗号分隔';
COMMENT ON COLUMN "ONEPAGE"."SYS_DIARY"."CREATED_AT" IS '创建时间（毫秒时间戳）';
COMMENT ON COLUMN "ONEPAGE"."SYS_DIARY"."UPDATED_AT" IS '更新时间（毫秒时间戳）';

COMMENT ON COLUMN "ONEPAGE"."SYS_ESSAY"."ID" IS '随笔主键，自增';
COMMENT ON COLUMN "ONEPAGE"."SYS_ESSAY"."USER_ID" IS '所属用户 ID，关联 sys_user.id';
COMMENT ON COLUMN "ONEPAGE"."SYS_ESSAY"."TITLE" IS '随笔标题';
COMMENT ON COLUMN "ONEPAGE"."SYS_ESSAY"."CONTENT" IS '随笔正文';
COMMENT ON COLUMN "ONEPAGE"."SYS_ESSAY"."CREATED_AT" IS '创建时间（毫秒时间戳）';
COMMENT ON COLUMN "ONEPAGE"."SYS_ESSAY"."UPDATED_AT" IS '更新时间（毫秒时间戳）';

COMMENT ON COLUMN "ONEPAGE"."SYS_BILL"."ID" IS '账单主键，自增';
COMMENT ON COLUMN "ONEPAGE"."SYS_BILL"."USER_ID" IS '所属用户 ID，关联 sys_user.id';
COMMENT ON COLUMN "ONEPAGE"."SYS_BILL"."TYPE" IS '类型：1 支出，2 收入';
COMMENT ON COLUMN "ONEPAGE"."SYS_BILL"."AMOUNT" IS '金额';
COMMENT ON COLUMN "ONEPAGE"."SYS_BILL"."CATEGORY" IS '分类';
COMMENT ON COLUMN "ONEPAGE"."SYS_BILL"."REMARK" IS '备注';
COMMENT ON COLUMN "ONEPAGE"."SYS_BILL"."BILL_DATE" IS '账单日期（毫秒时间戳）';
COMMENT ON COLUMN "ONEPAGE"."SYS_BILL"."CREATED_AT" IS '创建时间（毫秒时间戳）';
COMMENT ON COLUMN "ONEPAGE"."SYS_BILL"."UPDATED_AT" IS '更新时间（毫秒时间戳）';

-- -----------------------------------------------------------
-- 第 6 步  迁移数据
--   两个达梦约束导致不能写 INSERT ... SELECT *：
--     1. 必须显式列出目标列清单（否则报 -2723）
--     2. 自增列需先 SET IDENTITY_INSERT ON（已实测会同步推进种子，插入 ID=5 后下一条为 6，
--        因此保留原主键不会造成后续自增冲突）
-- -----------------------------------------------------------
SET IDENTITY_INSERT "ONEPAGE"."SYS_USER" ON;
INSERT INTO "ONEPAGE"."SYS_USER" ("ID", "USERNAME", "NICKNAME", "AVATAR", "PASSWORD_HASH", "CREATED_AT", "LAST_LOGIN_AT")
SELECT "ID", "USERNAME", "NICKNAME", "AVATAR", "PASSWORD_HASH", "CREATED_AT", "LAST_LOGIN_AT"
FROM "SOFTDEMO"."SYS_USER";
SET IDENTITY_INSERT "ONEPAGE"."SYS_USER" OFF;

SET IDENTITY_INSERT "ONEPAGE"."SYS_DIARY" ON;
INSERT INTO "ONEPAGE"."SYS_DIARY" ("ID", "USER_ID", "TITLE", "CONTENT", "MOOD", "IMAGES", "CREATED_AT", "UPDATED_AT")
SELECT "ID", "USER_ID", "TITLE", "CONTENT", "MOOD", "IMAGES", "CREATED_AT", "UPDATED_AT"
FROM "SOFTDEMO"."SYS_DIARY";
SET IDENTITY_INSERT "ONEPAGE"."SYS_DIARY" OFF;

SET IDENTITY_INSERT "ONEPAGE"."SYS_ESSAY" ON;
INSERT INTO "ONEPAGE"."SYS_ESSAY" ("ID", "USER_ID", "TITLE", "CONTENT", "CREATED_AT", "UPDATED_AT")
SELECT "ID", "USER_ID", "TITLE", "CONTENT", "CREATED_AT", "UPDATED_AT"
FROM "SOFTDEMO"."SYS_ESSAY";
SET IDENTITY_INSERT "ONEPAGE"."SYS_ESSAY" OFF;

SET IDENTITY_INSERT "ONEPAGE"."SYS_BILL" ON;
INSERT INTO "ONEPAGE"."SYS_BILL" ("ID", "USER_ID", "TYPE", "AMOUNT", "CATEGORY", "REMARK", "BILL_DATE", "CREATED_AT", "UPDATED_AT")
SELECT "ID", "USER_ID", "TYPE", "AMOUNT", "CATEGORY", "REMARK", "BILL_DATE", "CREATED_AT", "UPDATED_AT"
FROM "SOFTDEMO"."SYS_BILL";
SET IDENTITY_INSERT "ONEPAGE"."SYS_BILL" OFF;

COMMIT;
