package com.gstcompliance.controller;

import com.gstcompliance.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/deadlines")
public class DeadlineController {

    @GetMapping("/upcoming")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getUpcomingDeadlines() {

        // Sample deadlines data
        List<Map<String, Object>> deadlines = new ArrayList<>();

        Map<String, Object> deadline1 = new HashMap<>();
        deadline1.put("formType", "GSTR-3B");
        deadline1.put("dueDate", "2026-02-20");
        deadline1.put("daysRemaining", 25);
        deadline1.put("priority", "MEDIUM");
        deadlines.add(deadline1);

        Map<String, Object> deadline2 = new HashMap<>();
        deadline2.put("formType", "GSTR-1");
        deadline2.put("dueDate", "2026-02-11");
        deadline2.put("daysRemaining", 16);
        deadline2.put("priority", "HIGH");
        deadlines.add(deadline2);

        // ✅ FIX: Use ApiResponse.success() static method
        return ResponseEntity.ok(ApiResponse.success("Deadlines fetched successfully", deadlines));
    }
}