package com.bookleaf.support.service;

import com.bookleaf.support.entity.Priority;
import com.bookleaf.support.entity.TicketCategory;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class GroqApiIntegrationTest {

    @Test
    void classifiesARealTicketThroughGroqAndParsesTheStructuredResponse() throws IOException {
        String apiKey = System.getenv("GROQ_API_KEY");
        assumeTrue(apiKey != null && !apiKey.isBlank(), "GROQ_API_KEY is not configured");

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(45));
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl("https://api.groq.com/openai")
                .apiKey(apiKey)
                .restClientBuilder(RestClient.builder().requestFactory(requestFactory))
                .build();
        String modelName = System.getenv().getOrDefault("GROQ_MODEL", "qwen/qwen3.8-27b");
        OpenAiChatModel model = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(OpenAiChatOptions.builder().model(modelName).build())
                .build();
        AiService service = new AiService(
                ChatClient.builder(model),
                new KnowledgeBase(new ClassPathResource("knowledge-base.md")),
                true);

        AiService.TriageOutcome outcome = service.triage(
                "Incorrect ISBN on the listing",
                "The ISBN printed in my book does not match the listing metadata.");

        assertTrue(outcome.aiGenerated(), "Groq response should be parsed as structured triage data");
        assertEquals(TicketCategory.ISBN_METADATA, outcome.category());
        assertEquals(Priority.HIGH, outcome.priority());
    }
}
