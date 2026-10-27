-- ===========================================================
-- 达梦数据库 · 用户登录功能补强
--
-- 索引现状（实查，无需调整）：
--   INDEX33555505  UNIQUE  USER_ID        主键
--   INDEX33555506  UNIQUE  PHONE_NUMBER   唯一约束，登录查询走此索引
--   INDEX33555504  NONUNIQUE（无列）      建表时 CLUSTERBTR 自动生成的表聚簇索引
--   三者职责不重叠，不存在冗余或缺失。
--
-- 本次仅补一个字段：登录成功后需记录最近登录时间，原表无处可写。
--
-- 执行编码：iconv -f UTF-8 -t GBK
-- ===========================================================

ALTER TABLE "MINDMATE"."SYS_USER" ADD COLUMN "LAST_LOGIN_AT" TIMESTAMP;

COMMENT ON COLUMN "MINDMATE"."SYS_USER"."LAST_LOGIN_AT" IS '最近登录时间';
