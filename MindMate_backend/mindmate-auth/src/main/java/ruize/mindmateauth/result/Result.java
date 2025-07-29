package ruize.mindmateauth.result;

import lombok.Data;

@Data
public class Result<T> {
    private Integer code;    // 状态码，比如200表示成功
    private String message;  // 返回信息，比如"成功"或者错误描述
    private T data;          // 具体业务数据

    public Result() {
    }

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // 返回信息
    public static <T> Result<T> getResult(int code, String message, T data) {
        return new Result<>(code, message, data);
    }
}
