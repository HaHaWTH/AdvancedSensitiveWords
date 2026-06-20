package io.wdsj.asw.bukkit.ai;

@FunctionalInterface
interface LlmChatClient {
    String classify(String systemPrompt, String userMessage);
}
