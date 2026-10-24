package com.bookleaf.support.controller;

import com.bookleaf.support.config.UserPrincipal;
import com.bookleaf.support.dto.AdminDtos.*;
import com.bookleaf.support.entity.Priority;
import com.bookleaf.support.entity.TicketCategory;
import com.bookleaf.support.entity.TicketStatus;
import com.bookleaf.support.service.AdminService;
import com.bookleaf.support.service.AuthorService;
import com.bookleaf.support.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ContentDisposition;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "Admin")
@SecurityRequirement(name = "bearerAuth")
@Validated
@RestController
@RequestMapping("/api/admin/tickets")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @Operation(summary = "Ticket queue",
            description = "Filter by status/category/priority/assignee/date range/subject text. Sorted most-urgent first, then oldest; resolved and closed tickets sink to the bottom.")
    @GetMapping
    public PageResponse<TicketSummary> queue(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketCategory category,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Long assignedAdminId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return adminService.search(status, category, priority, assignedAdminId, from, to, q, page, size);
    }

    @Operation(summary = "Admin users available for ticket assignment")
    @GetMapping("/agents")
    public java.util.List<AdminUser> agents() {
        return adminService.listAdmins();
    }

    @Operation(summary = "Ticket detail with thread, internal notes and book data")
    @GetMapping("/{id}")
    public AdminTicketDetail get(@PathVariable Long id) {
        return adminService.getTicket(id);
    }

    @Operation(summary = "Download a ticket attachment")
    @GetMapping("/{id}/attachments/{attachmentId}")
    public ResponseEntity<Resource> attachment(@PathVariable Long id, @PathVariable Long attachmentId) {
        AuthorService.DownloadableAttachment attachment = adminService.getAttachment(id, attachmentId);
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

    @Operation(summary = "Update ticket status")
    @PatchMapping("/{id}/status")
    public AdminTicketDetail status(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest req) {
        return adminService.updateStatus(id, req.status());
    }

    @Operation(summary = "Override the AI category")
    @PatchMapping("/{id}/category")
    public AdminTicketDetail category(@PathVariable Long id, @Valid @RequestBody UpdateCategoryRequest req) {
        return adminService.overrideCategory(id, req.category());
    }

    @Operation(summary = "Override the AI priority")
    @PatchMapping("/{id}/priority")
    public AdminTicketDetail priority(@PathVariable Long id, @Valid @RequestBody UpdatePriorityRequest req) {
        return adminService.overridePriority(id, req.priority());
    }

    @Operation(summary = "Assign the ticket to the calling admin")
    @PostMapping("/{id}/assign")
    public AdminTicketDetail assign(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
        return adminService.assignToMe(id, me.getId());
    }

    @Operation(summary = "Assign or unassign a ticket")
    @PatchMapping("/{id}/assignee")
    public AdminTicketDetail assignee(@PathVariable Long id, @Valid @RequestBody UpdateAssigneeRequest req) {
        return adminService.assign(id, req.adminId());
    }

    @Operation(summary = "Add an internal note (never visible to the author)")
    @PostMapping("/{id}/notes")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminTicketDetail note(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id,
                                  @Valid @RequestBody AddNoteRequest req) {
        return adminService.addNote(id, me.getId(), req);
    }

    @Operation(summary = "Send a response to the author",
            description = "Set aiDrafted=true if the text started from the AI draft. Optional newStatus; otherwise Open becomes In Progress.")
    @PostMapping("/{id}/responses")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminTicketDetail respond(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id,
                                     @Valid @RequestBody AdminReplyRequest req) {
        return adminService.reply(id, me.getId(), req);
    }

    @Operation(summary = "Get an AI-drafted response (cached unless regenerate=true)",
            description = "Always 200; when the LLM is unavailable, available=false and the admin writes the reply manually.")
    @PostMapping("/{id}/ai-draft")
    public AiDraftResponse aiDraft(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean regenerate) {
        return adminService.generateDraft(id, regenerate);
    }

    @Operation(summary = "Re-run AI classification and priority scoring (respects manual overrides)")
    @PostMapping("/{id}/retriage")
    public AdminTicketDetail retriage(@PathVariable Long id) {
        return adminService.retriage(id);
    }
}
