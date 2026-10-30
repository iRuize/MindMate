package ruize.mindmateauth.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 完成或重开任务，状态合法性由服务层校验。
 *
 * @author 邸睿泽
 * @since 2026-10-30
 */
@Data
public class TaskStatusRequest {
    @NotNull(message = "任务状态不能为空")
    private Integer status;
}
