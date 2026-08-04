package com.pfe.predictive.comment.controller;

import com.pfe.predictive.comment.dto.CommentRequest;
import com.pfe.predictive.comment.dto.CommentResponse;
import com.pfe.predictive.comment.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<CommentResponse> add(@Valid @RequestBody CommentRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.add(request, authentication.getName()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<CommentResponse>> list(@RequestParam String entityType, @RequestParam Long entityId) {
        return ResponseEntity.ok(commentService.list(entityType, entityId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        boolean isPrivileged = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_MANAGER")
                        || a.getAuthority().equalsIgnoreCase("ROLE_ADMIN")
                        || a.getAuthority().equalsIgnoreCase("ROLE_SUPER_ADMIN"));
        commentService.delete(id, authentication.getName(), isPrivileged);
        return ResponseEntity.noContent().build();
    }
}
