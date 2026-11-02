package ruize.mindmateauth.exception;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ruize.mindmateauth.controller.DecisionController;
import ruize.mindmateauth.dto.DecisionMessageRequest;
import ruize.mindmateauth.dto.DecisionPreferenceRequest;
import ruize.mindmateauth.enums.ResultCode;
import ruize.mindmateauth.result.Result;

/**
 * 决策空间专用异常出口，不记录偏好、消息、凭证或远端响应。
 *
 * @author 邸睿泽
 * @since 2026-11-02
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = DecisionController.class)
public class DecisionExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        Object target = e.getBindingResult().getTarget();
        ResultCode code = target instanceof DecisionPreferenceRequest ? ResultCode.DECISION_PREFERENCE_INVALID
                : target instanceof DecisionMessageRequest ? ResultCode.DECISION_MESSAGE_INVALID
                : ResultCode.DECISION_PARAM_INVALID;
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage()).findFirst().orElse(code.getMessage());
        return Result.fail(code.getCode(), message);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            HttpMediaTypeNotSupportedException.class, IllegalArgumentException.class})
    public Result<Void> handleParamException(Exception e) {
        return Result.fail(ResultCode.DECISION_PARAM_INVALID);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        return Result.fail(ResultCode.DECISION_WRITE_FAILED);
    }
}
