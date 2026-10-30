package ruize.mindmateauth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import ruize.mindmateauth.interceptor.AuthInterceptor;

/**
 * 拦截器与头像静态资源注册。头像允许公开读取，用户资料与上传接口要求登录态。
 *
 * @author 邸睿泽
 * @since 2026-10-27
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final AvatarProperties avatarProperties;

    public WebConfig(AuthInterceptor authInterceptor, AvatarProperties avatarProperties) {
        this.authInterceptor = authInterceptor;
        this.avatarProperties = avatarProperties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                // /error 必须放行，否则异常转发会被二次拦截，掩盖真实错误
                .excludePathPatterns("/auth/sendSmsCode", "/auth/login", "/error", "/avatars/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = avatarProperties.storagePath().toUri().toString();
        registry.addResourceHandler("/avatars/**")
                .addResourceLocations(location.endsWith("/") ? location : location + "/");
    }
}
