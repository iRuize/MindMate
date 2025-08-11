package ruize.mindmateauth.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Aspect
@Component
public class SmsSendRecordAspect {
    @Around("@annotation(ruize.mindmateauth.aop.SmsSendRecord)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 记录当前时间
        LocalDateTime now = LocalDateTime.now();
        String time = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        // 获取请求信息（IP）
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String ip = "unknown";
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
        }
        // TODO将用户发送验证码时间以及IP地址写入数据库
        System.out.println("IP: " + ip);
        System.out.println("发送时间: " + time);
        // 将发送信息写入到Mysql
        // proceed()执行目标方法（必须调用，否则原方法不执行），result是原方法的返回值
        Object result = joinPoint.proceed();
        // 在方法执行后输出
        System.out.println("发送短信验证码方法执行完成");
        return result;
    }
}
