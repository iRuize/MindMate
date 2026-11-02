package ruize.mindmateauth.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .findFirst().orElse(ResultCode.PARAM_INVALID.getMessage());
        return Result.fail(ResultCode.PARAM_INVALID.getCode(), message);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleUploadSizeException(MaxUploadSizeExceededException e) {
        return Result.fail(ResultCode.PARAM_INVALID.getCode(),
                "上传文件过大，图片不能超过5MB，视频不能超过50MB，请求总大小不能超过65MB");
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public Result<Void> handleMissingFileException(MissingServletRequestPartException e) {
        return Result.fail(ResultCode.PARAM_INVALID.getCode(), "缺少上传内容，表单字段名为file");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public Result<Void> handleContentTypeException(HttpMediaTypeNotSupportedException e) {
        return Result.fail(ResultCode.PARAM_INVALID.getCode(),
                "请求格式不支持，资料编辑使用application/json，头像与语录上传使用multipart/form-data");
    }

    @ExceptionHandler(MultipartException.class)
    public Result<Void> handleMultipartException(MultipartException e) {
        return Result.fail(ResultCode.PARAM_INVALID.getCode(), "上传请求无效，请使用multipart/form-data提交文件");
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("未预期的异常", e);
        return Result.fail(ResultCode.SERVER_ERROR);
    }
}
