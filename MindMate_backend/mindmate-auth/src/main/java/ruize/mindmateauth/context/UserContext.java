package ruize.mindmateauth.context;

/**
 * 当前请求的登录用户上下文。
 * 由 AuthInterceptor 在 preHandle 写入、afterCompletion 清理，
 * 业务层无需再关心凭证如何解析。
 *
 * @author 邸睿泽
 * @since 2026-10-27
 */
public final class UserContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> TOKEN = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(Long userId, String token) {
        USER_ID.set(userId);
        TOKEN.set(token);
    }

    public static Long getUserId() {
        return USER_ID.get();
    }

    public static String getToken() {
        return TOKEN.get();
    }

    public static void clear() {
        USER_ID.remove();
        TOKEN.remove();
    }
}
