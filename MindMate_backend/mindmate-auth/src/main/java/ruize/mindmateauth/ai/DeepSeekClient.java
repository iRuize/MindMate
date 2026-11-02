package ruize.mindmateauth.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import ruize.mindmateauth.config.AiProperties;
import ruize.mindmateauth.enums.ResultCode;
import ruize.mindmateauth.exception.BizException;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;

/**
 * DeepSeek Chat Completions 客户端，无自动重试，响应体与读取时间均有上限。
 *
 * @author 邸睿泽
 * @since 2026-11-02
 */
@Component
public class DeepSeekClient {

    private static final int MAX_RESPONSE_BYTES = 2 * 1024 * 1024;

    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public DeepSeekClient(AiProperties aiProperties, ObjectMapper objectMapper) {
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    public String complete(List<AiMessage> messages) {
        if (!aiProperties.isReady()) {
            throw new BizException(ResultCode.DECISION_AI_NOT_CONFIGURED);
        }
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", aiProperties.getModel());
        requestBody.put("stream", false);
        requestBody.put("max_tokens", 2048);
        ArrayNode messageArray = requestBody.putArray("messages");
        for (AiMessage message : messages) {
            ObjectNode messageNode = messageArray.addObject();
            messageNode.put("role", message.role());
            messageNode.put("content", message.content());
        }

        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(URI.create(trimTrailingSlash(aiProperties.getBaseUrl()) + "/chat/completions"))
                    .timeout(Duration.ofSeconds(90))
                    .header("Authorization", "Bearer " + aiProperties.getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();
        } catch (IllegalArgumentException e) {
            throw new BizException(ResultCode.DECISION_AI_NOT_CONFIGURED);
        }

        CompletableFuture<HttpResponse<byte[]>> responseFuture = httpClient.sendAsync(request, info -> {
            int status = info.statusCode();
            if (status < 200 || status >= 300) {
                // 非成功响应直接丢弃，不缓存、不解析、更不返回远端错误体。
                return HttpResponse.BodySubscribers.replacing(new byte[0]);
            }
            return new LimitedBodySubscriber();
        });
        try {
            // get的超时覆盖完整响应体消费，避免仅收到响应头就脱离超时保护。
            HttpResponse<byte[]> response = responseFuture.get(90, TimeUnit.SECONDS);
            checkStatus(response.statusCode());
            return parseContent(response.body());
        } catch (TimeoutException e) {
            responseFuture.cancel(true);
            throw new BizException(ResultCode.DECISION_AI_TIMEOUT);
        } catch (InterruptedException e) {
            responseFuture.cancel(true);
            Thread.currentThread().interrupt();
            throw new BizException(ResultCode.DECISION_AI_TIMEOUT);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof HttpTimeoutException) {
                throw new BizException(ResultCode.DECISION_AI_TIMEOUT);
            }
            throw new BizException(ResultCode.DECISION_AI_SERVICE_ERROR);
        }
    }

    private void checkStatus(int status) {
        if (status == 401) {
            throw new BizException(ResultCode.DECISION_AI_UNAUTHORIZED);
        }
        if (status == 402) {
            throw new BizException(ResultCode.DECISION_AI_PAYMENT_REQUIRED);
        }
        if (status == 429) {
            throw new BizException(ResultCode.DECISION_AI_RATE_LIMITED);
        }
        if (status < 200 || status >= 300) {
            throw new BizException(ResultCode.DECISION_AI_SERVICE_ERROR);
        }
    }

    private String parseContent(byte[] body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode choice = root.path("choices").path(0);
            if ("length".equals(choice.path("finish_reason").asText())) {
                throw new BizException(ResultCode.DECISION_AI_SERVICE_ERROR,
                        "模型输出达到长度上限，未保存本轮消息，请缩短问题后重试");
            }
            JsonNode content = choice.path("message").path("content");
            if (!content.isTextual() || content.textValue().trim().isEmpty()) {
                throw new BizException(ResultCode.DECISION_AI_SERVICE_ERROR);
            }
            return content.textValue().trim();
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(ResultCode.DECISION_AI_SERVICE_ERROR);
        }
    }

    private String trimTrailingSlash(String baseUrl) {
        String value = baseUrl.trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static final class LimitedBodySubscriber implements HttpResponse.BodySubscriber<byte[]> {
        private final CompletableFuture<byte[]> result = new CompletableFuture<>();
        private final ByteArrayOutputStream output = new ByteArrayOutputStream();
        private Flow.Subscription subscription;

        @Override
        public CompletionStage<byte[]> getBody() {
            return result;
        }

        @Override
        public void onSubscribe(Flow.Subscription incoming) {
            subscription = incoming;
            incoming.request(1);
        }

        @Override
        public void onNext(List<ByteBuffer> buffers) {
            for (ByteBuffer buffer : buffers) {
                int size = buffer.remaining();
                if (size > MAX_RESPONSE_BYTES - output.size()) {
                    subscription.cancel();
                    result.completeExceptionally(new IllegalStateException("模型响应超过大小上限"));
                    return;
                }
                byte[] bytes = new byte[size];
                buffer.get(bytes);
                output.write(bytes, 0, bytes.length);
            }
            subscription.request(1);
        }

        @Override
        public void onError(Throwable throwable) {
            result.completeExceptionally(throwable);
        }

        @Override
        public void onComplete() {
            result.complete(output.toByteArray());
        }
    }
}
