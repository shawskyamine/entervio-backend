package com.entrevio.entrevio_backend.service;

import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    public String getDashboard() {
        return "dashboard";
    }
}
