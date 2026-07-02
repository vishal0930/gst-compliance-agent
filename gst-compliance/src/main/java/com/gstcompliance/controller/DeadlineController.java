package com.gstcompliance.controller;

import com.gstcompliance.agent.DeadlineTrackerAgent;
import com.gstcompliance.agent.base.AgentResult;
import com.gstcompliance.dto.response.ApiResponse;
import com.gstcompliance.model.User;
import com.gstcompliance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/deadlines")
@RequiredArgsConstructor
public class DeadlineController {

    private final DeadlineTrackerAgent deadlineTrackerAgent;
    private final UserRepository userRepository;

    /**
     * Returns upcoming GST filing deadlines for the authenticated user.
     * Uses month/year query params (defaults to current period).
     */
    @GetMapping("/upcoming")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getUpcomingDeadlines(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            Authentication authentication) {

        int effectiveMonth = month != null ? month : LocalDate.now().getMonthValue();
        int effectiveYear  = year  != null ? year  : LocalDate.now().getYear();

        Map<String, Object> request = new HashMap<>();
        request.put("month", effectiveMonth);
        request.put("year",  effectiveYear);

        // Include turnover slab from user profile if available
        if (authentication != null) {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found: " + email));
            request.put("userId", user.getId().toString());
            request.put("gstin", user.getGstin());
            request.put("turnoverSlab", user.getTurnoverSlab());
        }

        log.info("Fetching deadlines for period {}-{}", effectiveMonth, effectiveYear);
        AgentResult<List<Map<String, Object>>> result = deadlineTrackerAgent.execute(request);

        if (!result.isSuccess()) {
            return ResponseEntity.ok(
                    ApiResponse.error("Could not calculate deadlines: " + result.getErrorMessage(), "DEADLINE_ERROR")
            );
        }

        return ResponseEntity.ok(ApiResponse.success("Deadlines fetched successfully", result.getData()));
    }
}
