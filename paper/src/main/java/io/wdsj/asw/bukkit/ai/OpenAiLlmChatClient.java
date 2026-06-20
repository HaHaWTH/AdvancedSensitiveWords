package io.wdsj.asw.bukkit.ai;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.time.Duration;

final class OpenAiLlmChatClient implements LlmChatClient {
    private final OpenAiChatModel model;

    OpenAiLlmChatClient(LlmChatDetectionService.LlmSettings settings) {
        this.model = OpenAiChatModel.builder()
                .baseUrl(settings.baseUrl())
                .apiKey(settings.apiKey())
                .modelName(settings.modelName())
                .temperature(settings.temperature())
                .maxCompletionTokens(settings.maxOutputTokens())
                .timeout(Duration.ofSeconds(settings.requestTimeoutSeconds()))
                .maxRetries(0)
                .logRequests(false)
                .logResponses(false)
                .build();
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
}
