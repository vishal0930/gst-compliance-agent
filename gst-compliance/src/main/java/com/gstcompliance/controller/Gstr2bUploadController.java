package com.gstcompliance.controller;

import com.gstcompliance.dto.response.ApiResponse;
import com.gstcompliance.dto.request.Gstr2bUploadRequest;
import com.gstcompliance.dto.response.Gstr2bUploadResponse;
import com.gstcompliance.service.Gstr2bUploadService;
import com.gstcompliance.service.GstrPortalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/gstr2b")
@RequiredArgsConstructor
public class Gstr2bUploadController {

    private final Gstr2bUploadService gstr2bUploadService;
    private final GstrPortalService gstrPortalService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Gstr2bUploadResponse>> uploadGstr2bData(
            @Valid @RequestBody Gstr2bUploadRequest request,
            Authentication authentication) {

        String email = authentication.getName();
        log.info("Received GSTR2B upload request for user: {}", email);

        Gstr2bUploadResponse response = gstr2bUploadService.uploadGstr2bData(request, email);

        log.info("Successfully processed GSTR2B upload for user: {}, total invoices: {}",
                email, response.getTotalProcessed());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("GSTR2B data uploaded successfully", response));
    }
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSummary(
            @RequestParam int month,
            @RequestParam int year,
            Authentication authentication) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Summary fetched successfully",
                        gstrPortalService.getSummary(
                                authentication.getName(),
                                month,
                                year
                        )
                )
        );
    }
    @GetMapping("/invoices")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getInvoices(
            @RequestParam int month,
            @RequestParam int year,
            Authentication authentication) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "GSTR2B invoices fetched successfully",
                        gstrPortalService.fetchGstr2b(
                                authentication.getName(),
                                month,
                                year
                        )
                )
        );
    }
}

