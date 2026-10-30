package ruize.mindmateauth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 任务当前状态快照。
 *
 * @author 邸睿泽
 * @since 2026-10-30
 */
@Data
@Accessors(chain = true)
@TableName("TASK")
public class TodoTask {
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String title;
    private String description;
    private Integer status;
    private Integer priority;
    private LocalDate plannedDate;
    private LocalDateTime dueAt;
    private Integer estimatedMinutes;
    private Integer actualMinutes;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Integer deferCount;
    private LocalDate firstPlannedDate;
    private LocalDateTime lastDeferredAt;
    private Integer source;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic(value = "0", delval = "1")
    private Integer deleted;
}
