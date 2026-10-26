package com.entrevio.entrevio_backend.service;

import com.entrevio.entrevio_backend.ai.CvAgent;
import com.entrevio.entrevio_backend.dto.cv.CvDocumentDto;
import com.entrevio.entrevio_backend.dto.cv.ParsedCv;
import com.entrevio.entrevio_backend.exception.ValidationException;
import com.entrevio.entrevio_backend.model.CvDocument;
import com.entrevio.entrevio_backend.model.User;
import com.entrevio.entrevio_backend.model.enums.CvStatus;
import com.entrevio.entrevio_backend.model.enums.Role;
import com.entrevio.entrevio_backend.repository.CvRepository;
import com.entrevio.entrevio_backend.security.CurrentUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class CvService {

    private static final Logger log = LoggerFactory.getLogger(CvService.class);

    private final CvRepository cvRepository;
    private final CvAgent cvAgent;
    private final ObjectMapper objectMapper;
    private final CvRagService cvRagService;

    @Value("${entrevio.cv.upload-dir}")
    private String uploadDir;

    public CvService(CvRepository cvRepository,
                     CvAgent cvAgent,
                     ObjectMapper objectMapper,
                     CvRagService cvRagService) {
        this.cvRepository = cvRepository;
        this.cvAgent = cvAgent;
        this.objectMapper = objectMapper;
        this.cvRagService = cvRagService;
    }

    // =============== Public API ===============

    @Transactional(readOnly = true)
    public CvDocumentDto getMine() {
        User me = CurrentUser.requireRole(Role.CANDIDATE);
        return cvRepository.findByUserId(me.getId())
                .map(this::toDto)
                .orElse(null);
    }

    @Transactional
    public CvDocumentDto upload(MultipartFile file) {
        User me = CurrentUser.requireRole(Role.CANDIDATE);

        validateFile(file);

        // 1. Save the file to disk (overwrites any previous CV for this user)
        Path target = saveFileToDisk(file, me.getId());

        // 2. Get or create the CV record (one per candidate)
        CvDocument doc = cvRepository.findByUserId(me.getId())
                .orElseGet(() -> CvDocument.builder().user(me).build());
        doc.setFilename(file.getOriginalFilename());
        doc.setFilePath(target.toString());
        doc.setErrorMessage(null);

        // 3. Extract text
        String rawText;
        try {
            rawText = extractText(target);
        } catch (Exception e) {
            log.warn("PDF text extraction failed for user {}: {}", me.getId(), e.getMessage());
            doc.setStatus(CvStatus.FAILED);
            doc.setErrorMessage("Could not read PDF: " + e.getMessage());
            cvRepository.save(doc);
            return toDto(doc);
        }
        doc.setRawText(rawText);

        // 4. Send to LLM (fallback B — save raw text even if LLM fails)
        try {
            ParsedCv parsed = cvAgent.parse(rawText);
            doc.setParsedJson(objectMapper.writeValueAsString(parsed));
            doc.setStatus(CvStatus.PARSED);
        } catch (Exception e) {
            log.warn("LLM parsing failed for user {}: {}", me.getId(), e.getMessage());
            doc.setStatus(CvStatus.FAILED);
            doc.setErrorMessage("AI parsing failed: " + e.getMessage());
        }

        cvRepository.save(doc);

        // 5. Index into the RAG store (chunks + embeddings) — non-fatal
        if (doc.getStatus() == CvStatus.PARSED) {
            try {
                cvRagService.indexCv(doc);
            } catch (Exception e) {
                log.warn("RAG indexing failed for CV {}: {}", doc.getId(), e.getMessage());
            }
        }

        return toDto(doc);
    }

    // =============== Helpers ===============

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("No file provided");
        }
        if (file.getSize() > 10L * 1024 * 1024) {
            throw new ValidationException("File exceeds 10MB limit");
        }
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(".pdf")) {
            throw new ValidationException("Only PDF files are accepted");
        }
    }

    private Path saveFileToDisk(MultipartFile file, java.util.UUID userId) {
        try {
            Path dir = Paths.get(uploadDir);
            Files.createDirectories(dir);
            Path target = dir.resolve(userId + ".pdf");
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return target;
        } catch (IOException e) {
            throw new ValidationException("Could not save file: " + e.getMessage());
        }
    }

    private String extractText(Path pdfPath) throws IOException {
        try (PDDocument pdf = Loader.loadPDF(pdfPath.toFile())) {
            return new PDFTextStripper().getText(pdf);
        }
    }

    private CvDocumentDto toDto(CvDocument doc) {
        ParsedCv parsed = null;
        if (doc.getParsedJson() != null) {
            try {
                parsed = objectMapper.readValue(doc.getParsedJson(), ParsedCv.class);
            } catch (Exception e) {
                log.warn("Could not deserialize parsedJson: {}", e.getMessage());
            }
        }
        return CvDocumentDto.from(doc, parsed);
    }
}