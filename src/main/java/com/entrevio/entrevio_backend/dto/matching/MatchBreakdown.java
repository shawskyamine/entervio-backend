package com.entrevio.entrevio_backend.dto.matching;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchBreakdown {
    private Integer skills;      // 0-100
    private Integer experience;  // 0-100
    private Integer education;   // 0-100
    private Integer seniority;   // 0-100
}