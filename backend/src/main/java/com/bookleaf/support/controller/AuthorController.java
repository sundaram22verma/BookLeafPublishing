package com.bookleaf.support.controller;

import com.bookleaf.support.config.UserPrincipal;
import com.bookleaf.support.dto.AuthorDtos.*;
import com.bookleaf.support.exception.ResourceNotFoundException;
import com.bookleaf.support.service.AuthorService;
import com.bookleaf.support.service.TicketStreamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Author")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/author")
@RequiredArgsConstructor
public class AuthorController {

    private final AuthorService authorService;
    private final TicketStreamService streamService;

    @Operation(summary = "My books with sales and royalty figures")
    @GetMapping("/books")
    public List<BookResponse> books(@AuthenticationPrincipal UserPrincipal me) {
        return authorService.listBooks(me.getId());
    }

    @Operation(summary = "Submit a support ticket (bookId omitted = General / Account Level)")
    @PostMapping(value = "/tickets", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AuthorTicketView createTicket(@AuthenticationPrincipal UserPrincipal me,
                                         @Valid @RequestBody CreateTicketRequest request) {
        return authorService.createTicket(me.getId(), request);
    }

    @Operation(summary = "Submit a support ticket with file attachments")
    @PostMapping(value = "/tickets", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AuthorTicketView createTicketWithAttachments(@AuthenticationPrincipal UserPrincipal me,
            @Valid @RequestPart("ticket") CreateTicketRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        return authorService.createTicket(me.getId(), request, files);
    }

    @Operation(summary = "Download an attachment from one of my tickets")
    @GetMapping("/tickets/{ticketId}/attachments/{attachmentId}")
    public ResponseEntity<Resource> attachment(@AuthenticationPrincipal UserPrincipal me,
                                                @PathVariable Long ticketId, @PathVariable Long attachmentId) {
        return attachmentResponse(authorService.getAttachment(me.getId(), ticketId, attachmentId));
    }

    @Operation(summary = "My tickets with admin responses",
            description = "Poll with ?since=<max lastActivityAt you have seen> to receive only changed tickets.")
    @GetMapping("/tickets")
    public List<AuthorTicketView> tickets(@AuthenticationPrincipal UserPrincipal me,
                                          @Parameter(description = "ISO date-time, e.g. 2026-10-22T10:15:30")
                                          @RequestParam(required = false)
                                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        return authorService.listTickets(me.getId(), since);
    }

    @Operation(summary = "Live ticket updates (Server-Sent Events)")
    @GetMapping(value = "/tickets/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@AuthenticationPrincipal UserPrincipal me) {
        return streamService.subscribe(me.getId());
    }

    @Operation(summary = "One of my tickets")
    @GetMapping("/tickets/{id}")
    public AuthorTicketView ticket(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
        return authorService.getTicket(me.getId(), id);
    }

    @Operation(summary = "Add a follow-up message to my ticket")
    @PostMapping("/tickets/{id}/replies")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthorTicketView reply(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id,
                                  @Valid @RequestBody ReplyRequest request) {
        return authorService.reply(me.getId(), id, request);
    }

    private static ResponseEntity<Resource> attachmentResponse(AuthorService.DownloadableAttachment attachment) {
        FileSystemResource resource = new FileSystemResource(attachment.path());
        if (!resource.exists() || !resource.isReadable()) {
            throw ResourceNotFoundException.of("Attachment", attachment.fileName());
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(attachment.fileName(), java.nio.charset.StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(resource);
    }
}
