package com.entrevio.entrevio_backend.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PipelineDto {
    private long applied;
    private long reviewing;
    private long accepted;
    private long rejected;
    private long withdrawn;
}