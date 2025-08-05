package ruize.mindmateauth.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// 该注解只能加在方法上，不可以加载类或者是变量上
@Target(ElementType.METHOD)
// 仅在运行时保留，JVM可以通过反射获取信息
@Retention(RetentionPolicy.RUNTIME)
// 自定义了一个@SmsSendRecord注解
public @interface SmsSendRecord {
}