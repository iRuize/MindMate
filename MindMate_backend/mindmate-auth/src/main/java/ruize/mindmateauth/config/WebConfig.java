package ruize.mindmateauth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import ruize.mindmateauth.interceptor.AuthInterceptor;

/**
 * 拦截器注册。仅放行发码与登录两个入口，其余接口一律要求登录态。
 *
 * @author 邸睿泽
 * @since 2026-10-27
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public WebConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                // /error 必须放行，否则异常转发会被二次拦截，掩盖真实错误
                .excludePathPatterns("/auth/sendSmsCode", "/auth/login", "/error");
    }
}
