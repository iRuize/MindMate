package ruize.mindmateauth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 任务变更事件。
 *
 * @author 邸睿泽
 * @since 2026-10-30
 */
@Data
@Accessors(chain = true)
@TableName("TASK_EVENT")
public class TaskEvent {
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    private Long taskId;
    private Long userId;
    private String eventType;
    private String payload;
    private LocalDateTime occurredAt;
    private LocalDateTime createdAt;
}
