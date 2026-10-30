package ruize.mindmateauth.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 创建或全量编辑任务内容，不接受任务归属与状态字段。
 *
 * @author 邸睿泽
 * @since 2026-10-30
 */
@Data
public class TaskRequest {
    @NotBlank(message = "任务标题不能为空")
    @Size(max = 200, message = "任务标题不能超过200个字符")
    private String title;

    @Size(max = 5000, message = "任务描述不能超过5000个字符")
    private String description;

    @NotNull(message = "任务优先级不能为空")
    @Min(value = 0, message = "任务优先级必须在0到3之间")
    @Max(value = 3, message = "任务优先级必须在0到3之间")
    private Integer priority = 1;

    private LocalDate plannedDate;
    private LocalDateTime dueAt;

    @Min(value = 1, message = "预估耗时必须在1到525600分钟之间")
    @Max(value = 525600, message = "预估耗时必须在1到525600分钟之间")
    private Integer estimatedMinutes;

    public void setTitle(String title) {
        this.title = title == null ? null : title.trim();
    }
}
