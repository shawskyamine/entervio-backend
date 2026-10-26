package com.entrevio.entrevio_backend.model.enums;

public enum ChunkKind {
    HEADER,   // headline + years + top skills
    RAW,      // fixed-size text slice
    SKILLS    // a single chunk listing all parsed skills
}