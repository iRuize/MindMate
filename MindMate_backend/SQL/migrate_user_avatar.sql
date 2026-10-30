-- MindMate 已有数据库头像字段迁移（达梦 DM8）。
-- @author 邸睿泽
-- @since 2026-10-30
-- 仅用于 SYS_USER 已存在且尚无 AVATAR_URL 列的数据库，部署新后端前执行一次。
-- 新建数据库直接使用 mindmate_schema.sql，不要重复执行本迁移。
-- 保留已有 REAL_NAME 列及其数据；新代码不再映射或返回真实姓名。

ALTER TABLE "MINDMATE"."SYS_USER" ADD "AVATAR_URL" VARCHAR(255);
COMMENT ON COLUMN "MINDMATE"."SYS_USER"."AVATAR_URL" IS '头像公开访问路径';
