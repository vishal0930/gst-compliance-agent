package com.gstcompliance.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class LangChainConfig {

    // This single bean identifier will be used by your agents
    @Bean("invoiceParserModel")
    @ConditionalOnProperty(name = "app.llm.provider", havingValue = "ollama", matchIfMissing = true)
    public ChatLanguageModel localOllamaModel() {
        log.info("🚀 [LOCAL MODE] Wiring InvoiceParser to local Ollama (Llama 3.2:3b)");
        return OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("llama3.2:3b")
                .temperature(0.3)
                .build();
    }

    @Bean("invoiceParserModel")
    @ConditionalOnProperty(name = "app.llm.provider", havingValue = "anthropic")
    public ChatLanguageModel cloudClaudeModel(
            @Value("${anthropic.api-key}") String apiKey) {
        log.info("☁️ [CLOUD MODE] Wiring InvoiceParser to Anthropic Claude Sonnet API");

        // When you add the Anthropic dependency, unpack this:
        // return AnthropicChatModel.builder()
        //         .apiKey(apiKey)
        //         .modelName("claude-3-5-sonnet-latest")
        //         .temperature(0.3)
        //         .build();

        throw new UnsupportedOperationException("Add LangChain4j Anthropic dependency to use this.");
    }
}