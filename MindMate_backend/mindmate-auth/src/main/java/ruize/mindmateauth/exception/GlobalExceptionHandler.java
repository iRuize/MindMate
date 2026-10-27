package ruize.mindmateauth.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ruize.mindmateauth.enums.ResultCode;
import ruize.mindmateauth.result.Result;

/**
 * 全局异常处理。使所有异常出口与正常响应保持同一结构，
 * 避免前端在解析 Result 时遇到 Spring 默认错误体。
 *
 * @author 邸睿泽
 * @since 2026-10-27
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, HttpMessageNotReadableException.class,
            IllegalArgumentException.class})
    public Result<Void> handleParamException(Exception e) {
        log.warn("参数异常: {}", e.getMessage());
        return Result.fail(ResultCode.PARAM_INVALID);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("未预期的异常", e);
        return Result.fail(ResultCode.SERVER_ERROR);
    }
}
