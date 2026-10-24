package com.bookleaf.support.service;

import com.bookleaf.support.entity.Priority;
import com.bookleaf.support.entity.TicketCategory;
import com.bookleaf.support.exception.AiUnavailableException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiServiceTest {

    static Stream<RuntimeException> providerFailures() {
        return Stream.of(
                HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, "Unauthorized", HttpHeaders.EMPTY,
                        "invalid credentials".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8),
                HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "Bad Request", HttpHeaders.EMPTY,
                        "invalid model".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8),
                HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "Rate limited", HttpHeaders.EMPTY,
                        "rate limit".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8),
                HttpClientErrorException.create(HttpStatus.INTERNAL_SERVER_ERROR, "Server error", HttpHeaders.EMPTY,
                        "provider failure".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8),
                new ResourceAccessException("simulated network timeout"));
    }

    @ParameterizedTest
    @MethodSource("providerFailures")
    void triageFallsBackWhenGroqReturnsAnErrorOrCannotBeReached(RuntimeException failure) throws IOException {
        ChatModel model = mock(ChatModel.class);
        when(model.call(any(Prompt.class))).thenThrow(failure);
        AiService service = service(model, true);

        AiService.TriageOutcome result = service.triage("Incorrect ISBN", "The ISBN is wrong on the listing.");

        assertEquals(TicketCategory.ISBN_METADATA, result.category());
        assertEquals(Priority.HIGH, result.priority());
        assertFalse(result.aiGenerated());
    }

    @Test
    void draftingReportsProviderFailureSoTheAdminCanReplyManually() throws IOException {
        ChatModel model = mock(ChatModel.class);
        when(model.call(any(Prompt.class))).thenThrow(
                new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS));
        AiService service = service(model, true);

        assertThrows(AiUnavailableException.class, () -> service.draft(new AiService.DraftContext(
                "Author", "Royalty query", "Please check my royalty.", TicketCategory.ROYALTY_PAYMENTS,
                null, List.of())));
        verify(model).call(any(Prompt.class));
    }

    @Test
    void disabledAiDoesNotMakeProviderRequests() throws IOException {
        ChatModel model = mock(ChatModel.class);
        AiService service = service(model, false);

        assertFalse(service.triage("Question", "How do I update my profile?").aiGenerated());
        assertThrows(AiUnavailableException.class, () -> service.draft(new AiService.DraftContext(
                "Author", "Question", "How do I update my profile?", TicketCategory.GENERAL_INQUIRY,
                null, List.of())));
        verify(model, never()).call(any(Prompt.class));
    }

    private static AiService service(ChatModel model, boolean enabled) throws IOException {
        KnowledgeBase kb = new KnowledgeBase(new ClassPathResource("knowledge-base.md"));
        return new AiService(ChatClient.builder(model), kb, enabled);
    }
}
