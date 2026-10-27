package ruize.mindmateauth.exception;

import lombok.Getter;
import ruize.mindmateauth.enums.ResultCode;

/**
 * 业务异常。由全局异常处理器统一转换为 Result，
 * 业务代码只负责在失败处抛出，不必层层返回 Result。
 *
 * @author 邸睿泽
 * @since 2026-10-27
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BizException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }
}
