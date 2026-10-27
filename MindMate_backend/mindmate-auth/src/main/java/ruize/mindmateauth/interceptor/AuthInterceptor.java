package ruize.mindmateauth.interceptor;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import ruize.mindmateauth.config.AuthProperties;
import ruize.mindmateauth.constant.RedisKeys;
import ruize.mindmateauth.context.UserContext;
import ruize.mindmateauth.enums.ResultCode;
import ruize.mindmateauth.exception.BizException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Duration;

/**
 * 登录态拦截器。
 * 从 Authorization 头解析 Bearer 凭证，到 Redis 反查用户ID并写入上下文；
 * 通过后顺延凭证有效期，实现滑动过期。
 *
 * @author 邸睿泽
 * @since 2026-10-27
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final StringRedisTemplate stringRedisTemplate;
    private final AuthProperties authProperties;

    public AuthInterceptor(StringRedisTemplate stringRedisTemplate, AuthProperties authProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.authProperties = authProperties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 跨域预检不带凭证，直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = resolveToken(request.getHeader(HEADER));
        if (token == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }

        String tokenKey = RedisKeys.authToken(token);
        String userId = stringRedisTemplate.opsForValue().get(tokenKey);
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }

        // 滑动过期：有访问就续期
        stringRedisTemplate.expire(tokenKey, Duration.ofHours(authProperties.getTokenTtlHours()));

        UserContext.set(Long.valueOf(userId), token);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private String resolveToken(String header) {
        if (header == null || !header.startsWith(PREFIX)) {
            return null;
        }
        String token = header.substring(PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }
}
