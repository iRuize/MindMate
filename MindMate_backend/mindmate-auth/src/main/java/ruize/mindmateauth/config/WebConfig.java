package ruize.mindmateauth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import ruize.mindmateauth.interceptor.AuthInterceptor;

/**
 * 拦截器与本地静态资源注册。头像与语录媒体允许公开读取，
 * 用户资料、任务、决策与上传接口要求登录态。
 *
 * @author 邸睿泽
 * @since 2026-10-27
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final AvatarProperties avatarProperties;
    private final QuoteProperties quoteProperties;

    public WebConfig(AuthInterceptor authInterceptor, AvatarProperties avatarProperties,
                     QuoteProperties quoteProperties) {
        this.authInterceptor = authInterceptor;
        this.avatarProperties = avatarProperties;
        this.quoteProperties = quoteProperties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                // /error 必须放行，否则异常转发会被二次拦截，掩盖真实错误
                // 语录媒体 /media/quotes/** 在 AuthInterceptor 内部放行，与头像保持一致
                .excludePathPatterns("/auth/sendSmsCode", "/auth/login", "/error", "/avatars/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String avatarLocation = avatarProperties.storagePath().toUri().toString();
        registry.addResourceHandler("/avatars/**")
                .addResourceLocations(avatarLocation.endsWith("/") ? avatarLocation : avatarLocation + "/");

        // API 前缀是 /quotes，媒体静态路径统一走 /media/quotes/**，两者不冲突
        String quoteLocation = quoteProperties.storagePath().toUri().toString();
        registry.addResourceHandler("/media/quotes/**")
                .addResourceLocations(quoteLocation.endsWith("/") ? quoteLocation : quoteLocation + "/");
    }
}
