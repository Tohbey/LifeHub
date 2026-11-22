package com.lifehub.app.controller;

import java.time.Duration;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lifehub.app.model.dto.ReferenceDataDTO;
import com.lifehub.app.service.ReferenceDataService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Lists served by lifehub-app itself rather than routed to a service: they are the same for everyone and need
 * no database. Like every /api path, it needs a signed-in user.
 */
@RestController
@Tag(name = "Reference data", description = "Currencies and timezones for the UI's dropdowns.")
@RequiredArgsConstructor
class ReferenceDataController {

    private final ReferenceDataService referenceData;

    /** GET /api/v1/reference-data: every currency and timezone, the preferred ones first. Cached for an hour. */
    @Operation(summary = "Get currencies and timezones",
            description = "ISO 4217 currencies and IANA timezones (with their current UTC offset). The ones configured "
                    + "under lifehub.reference come first and are marked common.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The lists",
                    content = @Content(schema = @Schema(implementation = ReferenceDataDTO.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content)
    })
    @GetMapping("/api/v1/reference-data")
    ResponseEntity<ReferenceDataDTO> getReferenceData() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
                .body(referenceData.getReferenceData());
    }
}
