package com.entrevio.entrevio_backend.controller;

import com.entrevio.entrevio_backend.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    @GetMapping("/plans")
    public ApiResponse<List<Map<String, Object>>> listPlans() {
        return ApiResponse.ok(List.of(
                Map.of(
                        "id",          "free",
                        "name",        "Free",
                        "priceCents",  0,
                        "features",    List.of("1 active job", "Basic matching")
                ),
                Map.of(
                        "id",          "pro",
                        "name",        "Pro",
                        "priceCents",  2900,
                        "features",    List.of("Unlimited jobs", "AI matching", "Interview agent")
                ),
                Map.of(
                        "id",          "team",
                        "name",        "Team",
                        "priceCents",  9900,
                        "features",    List.of("Everything in Pro", "5 seats", "Analytics")
                )
        ));
    }
}