package ruize.mindmateauth.dto;

import lombok.Data;
import ruize.mindmateauth.entity.User;

import java.time.LocalDateTime;

/**
 * 用户视图对象。只暴露可对外字段，避免实体直接穿透到接口层。
 *
 * @author 邸睿泽
 * @since 2026-10-27
 */
@Data
public class UserVO {
    private Long userId;
    private String phoneNumber;
    private String username;
    private String avatarUrl;
    private String personalizedSignature;
    private String email;
    private LocalDateTime lastLoginAt;

    public static UserVO from(User user) {
        UserVO vo = new UserVO();
        vo.setUserId(user.getUserId());
        vo.setPhoneNumber(user.getPhoneNumber());
        vo.setUsername(user.getUsername());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setPersonalizedSignature(user.getPersonalizedSignature());
        vo.setEmail(user.getEmail());
        vo.setLastLoginAt(user.getLastLoginAt());
        return vo;
    }
}
