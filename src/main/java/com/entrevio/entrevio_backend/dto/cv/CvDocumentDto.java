package com.entrevio.entrevio_backend.dto.cv;

import com.entrevio.entrevio_backend.model.CvDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CvDocumentDto {
    private UUID id;
    private String filename;
    private String status;
    private String errorMessage;
    private ParsedCv parsed;
    private LocalDateTime uploadedAt;

    public static CvDocumentDto from(CvDocument doc, ParsedCv parsed) {
        return CvDocumentDto.builder()
                .id(doc.getId())
                .filename(doc.getFilename())
                .status(doc.getStatus().name().toLowerCase())
                .errorMessage(doc.getErrorMessage())
                .parsed(parsed)
                .uploadedAt(doc.getUpdatedAt())
                .build();
    }
}