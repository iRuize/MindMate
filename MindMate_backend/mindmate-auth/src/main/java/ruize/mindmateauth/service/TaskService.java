package ruize.mindmateauth.service;

import ruize.mindmateauth.dto.TaskRequest;
import ruize.mindmateauth.dto.TaskStatusRequest;
import ruize.mindmateauth.entity.TodoTask;

import java.util.List;

/**
 * 当前登录用户的任务清单服务。
 *
 * @author 邸睿泽
 * @since 2026-10-30
 */
public interface TaskService {
    List<TodoTask> listTasks();

    TodoTask createTask(TaskRequest request);

    TodoTask updateTask(Long id, TaskRequest request);

    TodoTask updateStatus(Long id, TaskStatusRequest request);

    void deleteTask(Long id);
}
