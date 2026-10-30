package ruize.mindmateauth.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ruize.mindmateauth.dto.TaskRequest;
import ruize.mindmateauth.dto.TaskStatusRequest;
import ruize.mindmateauth.entity.TodoTask;
import ruize.mindmateauth.result.Result;
import ruize.mindmateauth.service.TaskService;

import javax.validation.Valid;
import java.util.List;

/**
 * 当前登录用户的任务清单接口。
 *
 * @author 邸睿泽
 * @since 2026-10-30
 */
@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public Result<List<TodoTask>> listTasks() {
        return Result.success(taskService.listTasks());
    }

    @PostMapping
    public Result<TodoTask> createTask(@Valid @RequestBody TaskRequest request) {
        return Result.success(taskService.createTask(request));
    }

    @PutMapping("/{id}")
    public Result<TodoTask> updateTask(@PathVariable("id") Long id, @Valid @RequestBody TaskRequest request) {
        return Result.success(taskService.updateTask(id, request));
    }

    @PutMapping("/{id}/status")
    public Result<TodoTask> updateStatus(@PathVariable("id") Long id, @Valid @RequestBody TaskStatusRequest request) {
        return Result.success(taskService.updateStatus(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteTask(@PathVariable("id") Long id) {
        taskService.deleteTask(id);
        return Result.success();
    }
}
