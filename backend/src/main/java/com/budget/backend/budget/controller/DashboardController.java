package com.budget.backend.budget.controller;

import com.budget.backend.budget.dto.DashboardSummaryDto;
import com.budget.backend.budget.service.DashboardSummaryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardSummaryService dashboardSummaryService;

    public DashboardController(DashboardSummaryService dashboardSummaryService) {
        this.dashboardSummaryService = dashboardSummaryService;
    }

    @GetMapping("/summary")
    @PreAuthorize("isAuthenticated()")
    public DashboardSummaryDto getSummary(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return dashboardSummaryService.getSummary(user.getUsername());
    }
}
