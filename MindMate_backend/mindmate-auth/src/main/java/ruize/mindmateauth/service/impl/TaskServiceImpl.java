package ruize.mindmateauth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ruize.mindmateauth.context.UserContext;
import ruize.mindmateauth.dto.TaskRequest;
import ruize.mindmateauth.dto.TaskStatusRequest;
import ruize.mindmateauth.entity.TaskEvent;
import ruize.mindmateauth.entity.TodoTask;
import ruize.mindmateauth.enums.ResultCode;
import ruize.mindmateauth.exception.BizException;
import ruize.mindmateauth.mapper.TaskEventMapper;
import ruize.mindmateauth.mapper.TaskMapper;
import ruize.mindmateauth.service.TaskService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务快照与事件流在同一事务内维护。
 *
 * @author 邸睿泽
 * @since 2026-10-30
 */
@Service
public class TaskServiceImpl implements TaskService {

    private final TaskMapper taskMapper;
    private final TaskEventMapper taskEventMapper;
    private final ObjectMapper objectMapper;

    public TaskServiceImpl(TaskMapper taskMapper, TaskEventMapper taskEventMapper, ObjectMapper objectMapper) {
        this.taskMapper = taskMapper;
        this.taskEventMapper = taskEventMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<TodoTask> listTasks() {
        return taskMapper.selectList(new LambdaQueryWrapper<TodoTask>()
                .eq(TodoTask::getUserId, currentUserId())
                .orderByDesc(TodoTask::getCreatedAt, TodoTask::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TodoTask createTask(TaskRequest request) {
        Long userId = currentUserId();
        LocalDateTime now = LocalDateTime.now();
        TodoTask task = new TodoTask()
                .setUserId(userId)
                .setTitle(request.getTitle())
                .setDescription(request.getDescription())
                .setStatus(0)
                .setPriority(request.getPriority())
                .setPlannedDate(request.getPlannedDate())
                .setDueAt(request.getDueAt())
                .setEstimatedMinutes(request.getEstimatedMinutes())
                .setDeferCount(0)
                .setFirstPlannedDate(request.getPlannedDate())
                .setSource(1)
                .setCreatedAt(now)
                .setUpdatedAt(now)
                .setDeleted(0);
        checkWrite(taskMapper.insert(task));
        recordEvent("CREATED", null, task, now);
        return task;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TodoTask updateTask(Long id, TaskRequest request) {
        Long userId = currentUserId();
        TodoTask task = lockTask(id, userId);
        JsonNode before = snapshot(task);
        LocalDateTime now = LocalDateTime.now();
        LocalDate oldPlannedDate = task.getPlannedDate();
        LocalDate plannedDate = request.getPlannedDate();
        String eventType = "UPDATED";
        if (plannedDate != null) {
            if (task.getFirstPlannedDate() == null) {
                task.setFirstPlannedDate(plannedDate);
            }
            if (oldPlannedDate == null) {
                eventType = "SCHEDULED";
            } else if (plannedDate.isAfter(oldPlannedDate)) {
                task.setDeferCount(task.getDeferCount() + 1)
                        .setLastDeferredAt(now);
                eventType = "DEFERRED";
            }
        }
        task.setTitle(request.getTitle())
                .setDescription(request.getDescription())
                .setPriority(request.getPriority())
                .setPlannedDate(plannedDate)
                .setDueAt(request.getDueAt())
                .setEstimatedMinutes(request.getEstimatedMinutes())
                .setUpdatedAt(now);

        // 显式 set 保留 null，允许全量编辑清空可选字段。
        checkWrite(taskMapper.update(null, taskUpdate(id, userId)
                .set(TodoTask::getTitle, task.getTitle())
                .set(TodoTask::getDescription, task.getDescription())
                .set(TodoTask::getPriority, task.getPriority())
                .set(TodoTask::getPlannedDate, task.getPlannedDate())
                .set(TodoTask::getDueAt, task.getDueAt())
                .set(TodoTask::getEstimatedMinutes, task.getEstimatedMinutes())
                .set(TodoTask::getFirstPlannedDate, task.getFirstPlannedDate())
                .set(TodoTask::getDeferCount, task.getDeferCount())
                .set(TodoTask::getLastDeferredAt, task.getLastDeferredAt())
                .set(TodoTask::getUpdatedAt, now)));
        recordEvent(eventType, before, task, now);
        return task;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TodoTask updateStatus(Long id, TaskStatusRequest request) {
        Long userId = currentUserId();
        Integer status = request.getStatus();
        if (status == null || (status != 0 && status != 2)) {
            throw new BizException(ResultCode.TASK_STATUS_INVALID);
        }
        TodoTask task = lockTask(id, userId);
        if (status.equals(task.getStatus())) {
            return task;
        }
        JsonNode before = snapshot(task);
        LocalDateTime now = LocalDateTime.now();
        task.setStatus(status)
                .setCompletedAt(status == 2 ? now : null)
                .setUpdatedAt(now);
        checkWrite(taskMapper.update(null, taskUpdate(id, userId)
                .set(TodoTask::getStatus, status)
                .set(TodoTask::getCompletedAt, task.getCompletedAt())
                .set(TodoTask::getUpdatedAt, now)));
        recordEvent(status == 2 ? "COMPLETED" : "REOPENED", before, task, now);
        return task;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTask(Long id) {
        Long userId = currentUserId();
        TodoTask task = lockTask(id, userId);
        JsonNode before = snapshot(task);
        LocalDateTime now = LocalDateTime.now();
        // update 自动附加 deleted=0；在 SET 中显式写 deleted=1 和更新时间。
        checkWrite(taskMapper.update(null, taskUpdate(id, userId)
                .set(TodoTask::getDeleted, 1)
                .set(TodoTask::getUpdatedAt, now)));
        task.setDeleted(1).setUpdatedAt(now);
        recordEvent("DELETED", before, task, now);
    }

    private Long currentUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return userId;
    }

    private TodoTask lockTask(Long id, Long userId) {
        // @TableLogic 自动限定 deleted=0，行锁持续到快照与事件事务提交。
        TodoTask task = taskMapper.selectOne(new LambdaQueryWrapper<TodoTask>()
                .eq(TodoTask::getId, id)
                .eq(TodoTask::getUserId, userId)
                .last("FOR UPDATE"));
        if (task == null) {
            throw new BizException(ResultCode.TASK_NOT_FOUND);
        }
        return task;
    }

    private LambdaUpdateWrapper<TodoTask> taskUpdate(Long id, Long userId) {
        return new LambdaUpdateWrapper<TodoTask>()
                .eq(TodoTask::getId, id)
                .eq(TodoTask::getUserId, userId);
    }

    private void checkWrite(int affectedRows) {
        if (affectedRows != 1) {
            throw new BizException(ResultCode.TASK_WRITE_FAILED);
        }
    }

    private JsonNode snapshot(TodoTask task) {
        try {
            return objectMapper.valueToTree(task);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("任务快照序列化失败", e);
        }
    }

    private void recordEvent(String eventType, JsonNode before, TodoTask task, LocalDateTime now) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.set("before", before == null ? objectMapper.nullNode() : before);
        payload.set("after", snapshot(task));
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("任务事件序列化失败", e);
        }
        checkWrite(taskEventMapper.insert(new TaskEvent()
                .setTaskId(task.getId())
                .setUserId(task.getUserId())
                .setEventType(eventType)
                .setPayload(json)
                .setOccurredAt(now)
                .setCreatedAt(now)));
    }
}
