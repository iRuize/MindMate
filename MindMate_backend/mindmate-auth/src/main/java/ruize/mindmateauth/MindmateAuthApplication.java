package ruize.mindmateauth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy
@SpringBootApplication
public class MindmateAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(MindmateAuthApplication.class, args);
    }

}
