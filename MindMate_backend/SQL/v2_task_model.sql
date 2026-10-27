/*
 MindMate 数据模型 v2

 目标：为 todolist 建立可采集行为历史的表结构，支撑后续 AI 辅助决策。
 设计原则：
   1. 行为类字段（耗时、推迟、时间戳）必须 v1 就存在，否则历史不可回补；
      分类类字段（项目、标签）可后加，历史缺失无害。
   2. 快照表（task）负责查询性能，事件流（task_event）负责历史真相，同事务双写。
   3. 派生值不入库（紧迫度、四象限由 due_at 在决策时现算，避免与源字段不同步）。
   4. 不建物理外键，关联完整性由应用层保证，避免后续分库受阻。

 执行顺序：v2-1 -> v2-2 -> v2-3，不可跳序（task 依赖 user_id 存在）。
 字符集与排序规则沿用现有库：utf8mb4 / utf8mb4_0900_ai_ci。
*/

SET NAMES utf8mb4;

-- =============================================================
-- v2-1  user 表改造
--   主键由 phone_number 换为自增 user_id，手机号降为唯一索引。
--   原因：业务表外键统一挂 BIGINT，避免 varchar(20) 外键在多表间扩散；
--         手机号属可变属性，不可作为稳定主键。
--   注意：AUTO_INCREMENT 列必须在同一条 ALTER 中即获得索引，
--         故拆为两步执行，不可合并。
-- =============================================================

ALTER TABLE `user`
    ADD COLUMN `user_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID' FIRST,
    ADD UNIQUE KEY `uk_user_id` (`user_id`);

ALTER TABLE `user`
    DROP PRIMARY KEY,
    ADD PRIMARY KEY (`user_id`),
    ADD UNIQUE KEY `uk_phone_number` (`phone_number`),
    ADD COLUMN `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    ADD COLUMN `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';


-- =============================================================
-- v2-2  task  任务表（当前状态快照）
--   每行代表一个任务的"此刻"，允许被覆盖更新。
--   对应的事件流见 v2-3。
-- =============================================================

CREATE TABLE `task`
(
    `id`                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '任务ID',
    `user_id`            BIGINT       NOT NULL COMMENT '归属用户ID',

    -- 内容
    `title`              VARCHAR(200) NOT NULL COMMENT '任务标题',
    `description`        TEXT         NULL COMMENT '任务描述',

    -- 状态：0待办 1进行中 2已完成 3已取消 4已归档
    `status`             TINYINT      NOT NULL DEFAULT 0 COMMENT '状态',

    -- 决策输入：仅保留用户一眼可选的单一主观值，避免填写摩擦导致数据缺失
    `priority`           TINYINT      NOT NULL DEFAULT 1 COMMENT '优先级：0低 1中 2高 3紧急',

    -- 时间计划
    `planned_date`       DATE         NULL COMMENT '计划执行日',
    `due_at`             DATETIME     NULL COMMENT '截止时间',
    `estimated_minutes`  INT          NULL COMMENT '预估耗时（分钟）',

    -- 行为埋点：随操作即时写入，是 AI 估算校准与拖延识别的依据
    `actual_minutes`     INT          NULL COMMENT '实际耗时（分钟）',
    `started_at`         DATETIME     NULL COMMENT '首次开始时间',
    `completed_at`       DATETIME     NULL COMMENT '完成时间',
    `defer_count`        INT          NOT NULL DEFAULT 0 COMMENT '累计推迟次数',
    `first_planned_date` DATE         NULL COMMENT '首次计划执行日，与 planned_date 的差值即推迟跨度',
    `last_deferred_at`   DATETIME     NULL COMMENT '最后一次推迟的操作时间',

    -- 来源溯源：区分人工录入与 AI 产出，用于评估建议采纳率
    `source`             TINYINT      NOT NULL DEFAULT 1 COMMENT '来源：1用户创建 2AI拆分 3AI建议采纳',

    -- 审计
    `created_at`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`            TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1已删除',

    PRIMARY KEY (`id`),
    KEY `idx_user_status_plan` (`user_id`, `status`, `planned_date`),
    KEY `idx_user_due` (`user_id`, `due_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='任务表（当前状态快照）';


-- =============================================================
-- v2-3  task_event  任务事件流（不可变，行为历史的唯一事实来源）
--   只追加、不更新、不删除。快照表中的 defer_count 等累计值是它的缓存。
--   event_type 用可读字符串而非整数：该表是人工排查与模型特征抽取的主要对象，
--   可读性优先，写入频率远低于 status 的过滤频率。
-- =============================================================

CREATE TABLE `task_event`
(
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '事件ID',
    `task_id`     BIGINT      NOT NULL COMMENT '任务ID',
    `user_id`     BIGINT      NOT NULL COMMENT '归属用户ID，冗余存储以支持按用户全量扫事件流',
    `event_type`  VARCHAR(32) NOT NULL COMMENT '事件类型：CREATED/SCHEDULED/DEFERRED/STARTED/COMPLETED/REOPENED/CANCELLED/DELETED/UPDATED/SUGGESTED',
    `payload`     JSON        NULL COMMENT '事件载荷：变更字段的前后值，或事件附加上下文',
    `occurred_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '事件发生时间，毫秒精度保证同秒内排序稳定',
    `created_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',

    PRIMARY KEY (`id`),
    KEY `idx_task_time` (`task_id`, `occurred_at`),
    KEY `idx_user_type_time` (`user_id`, `event_type`, `occurred_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='任务事件流（不可变）';
