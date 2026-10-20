package com.clarify.transaction;

import com.clarify.config.OpenApiConfig;
import com.clarify.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transactions", description = "CRUD for income and expense records")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class TransactionController {

    private final TransactionService service;

    public TransactionController(TransactionService service) {
        this.service = service;
    }

    @Operation(summary = "List your transactions, optionally for one month (YYYY-MM)")
    @GetMapping
    public List<TransactionResponse> list(@AuthenticationPrincipal CurrentUser user,
                                          @RequestParam(required = false) YearMonth month) {
        return service.list(user.id(), month);
    }

    @Operation(summary = "Create a transaction")
    @PostMapping
    public TransactionResponse create(@AuthenticationPrincipal CurrentUser user,
                                      @Valid @RequestBody TransactionRequest request) {
        return service.create(user, request);
    }

    @Operation(summary = "Update a transaction")
    @PutMapping("/{id}")
    public TransactionResponse update(@AuthenticationPrincipal CurrentUser user,
                                      @PathVariable UUID id,
                                      @Valid @RequestBody TransactionRequest request) {
        return service.update(user.id(), id, request);
    }

    @Operation(summary = "Delete a transaction")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
        service.delete(user.id(), id);
        return ResponseEntity.noContent().build();
    }
}
