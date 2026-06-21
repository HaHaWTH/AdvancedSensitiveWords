package io.wdsj.asw.bukkit.ai;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiResponsesChatModel;

import java.time.Duration;

final class CompatibleLlmChatClient implements LlmChatClient {
    private final ChatModel model;

    CompatibleLlmChatClient(LlmChatDetectionService.LlmSettings settings) {
        this.model = switch (settings.apiMode()) {
            case CHAT_COMPLETIONS -> createChatCompletionsModel(settings);
            case RESPONSES -> createResponsesModel(settings);
            case ANTHROPIC_MESSAGES -> createAnthropicMessagesModel(settings);
        };
    }

    @Override
    public String classify(String systemPrompt, String userMessage) {
        return model
                .chat(ChatRequest.builder()
                        .messages(SystemMessage.from(systemPrompt), UserMessage.from(userMessage))
                        .build())
                .aiMessage()
                .text();
    }

    private static ChatModel createChatCompletionsModel(LlmChatDetectionService.LlmSettings settings) {
        var builder = OpenAiChatModel.builder()
                .baseUrl(settings.baseUrl())
                .apiKey(settings.apiKey())
                .modelName(settings.modelName())
                .temperature(settings.temperature())
                .maxCompletionTokens(settings.maxOutputTokens())
                .timeout(timeout(settings))
                .maxRetries(0)
                .logRequests(false)
                .logResponses(false);
        return builder.build();
    }

    private static ChatModel createResponsesModel(LlmChatDetectionService.LlmSettings settings) {
        var builder = OpenAiResponsesChatModel.builder()
                .httpClientBuilder(new JdkHttpClientBuilder()
                        .connectTimeout(timeout(settings))
                        .readTimeout(timeout(settings)))
                .baseUrl(settings.baseUrl())
                .apiKey(settings.apiKey())
                .modelName(settings.modelName())
                .temperature(settings.temperature())
                .maxOutputTokens(settings.maxOutputTokens())
                .store(false)
                .logRequests(false)
                .logResponses(false);
        return builder.build();
    }

    private static ChatModel createAnthropicMessagesModel(LlmChatDetectionService.LlmSettings settings) {
        var builder = AnthropicChatModel.builder()
                .baseUrl(settings.baseUrl())
                .apiKey(settings.apiKey())
                .version(settings.anthropicVersion())
                .modelName(settings.modelName())
                .temperature(settings.temperature())
                .maxTokens(settings.maxOutputTokens())
                .thinkingType(settings.anthropicThinkingEnabled() ? "enabled" : "disabled")
                .timeout(timeout(settings))
                .maxRetries(0)
                .logRequests(false)
                .logResponses(false);
        return builder.build();
    }

    private static Duration timeout(LlmChatDetectionService.LlmSettings settings) {
        return Duration.ofSeconds(settings.requestTimeoutSeconds());
    }
}
