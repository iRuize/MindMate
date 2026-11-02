package ruize.mindmateauth.ai;

/**
 * 发往模型的聊天消息。
 *
 * @author 邸睿泽
 * @since 2026-11-02
 */
public record AiMessage(String role, String content) {
}
