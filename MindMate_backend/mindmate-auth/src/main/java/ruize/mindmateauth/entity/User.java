package ruize.mindmateauth.entity;

import io.lettuce.core.dynamic.annotation.Key;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

// 使用链式setters
@Accessors(chain = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {
    private String phoneNumber;
    private String username;
    private String personalizedSignature;
    private String email;
}
