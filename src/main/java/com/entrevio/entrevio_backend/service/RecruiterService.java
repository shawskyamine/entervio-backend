package com.entrevio.entrevio_backend.service;

import com.entrevio.entrevio_backend.dto.recruiter.CompanyDto;
import com.entrevio.entrevio_backend.dto.recruiter.UpdateCompanyRequest;
import com.entrevio.entrevio_backend.exception.NotFoundException;
import com.entrevio.entrevio_backend.exception.ValidationException;
import com.entrevio.entrevio_backend.model.Company;
import com.entrevio.entrevio_backend.model.User;
import com.entrevio.entrevio_backend.model.enums.Role;
import com.entrevio.entrevio_backend.repository.CompanyRepository;
import com.entrevio.entrevio_backend.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecruiterService {

    private final CompanyRepository companyRepository;

    public RecruiterService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    /**
     * Returns the company for the currently logged-in recruiter.
     * If none exists, auto-creates an empty one so the frontend
     * always has something to render.
     */
    @Transactional
    public CompanyDto getOrCreateMine() {
        User me = CurrentUser.requireRole(Role.RECRUITER);

        Company company = companyRepository.findByOwnerId(me.getId())
                .orElseGet(() -> {
                    Company c = Company.builder()
                            .owner(me)
                            .name(me.getFullName() + "'s Company")
                            .build();
                    return companyRepository.save(c);
                });

        return CompanyDto.from(company);
    }

    /**
     * Updates the recruiter's company profile.
     */
    @Transactional
    public CompanyDto updateMine(UpdateCompanyRequest req) {
        User me = CurrentUser.requireRole(Role.RECRUITER);

        if (req.getName() == null || req.getName().isBlank()) {
            throw new ValidationException("Company name is required");
        }

        Company company = companyRepository.findByOwnerId(me.getId())
                .orElseGet(() -> Company.builder().owner(me).build());

        company.setName(req.getName().trim());
        company.setWebsite(req.getWebsite() != null ? req.getWebsite().trim() : null);
        company.setDescription(req.getDescription() != null ? req.getDescription().trim() : null);

        return CompanyDto.from(companyRepository.save(company));
    }

    /**
     * Fetch by ID — used by job aggregation (e.g. displaying the company on a job).
     */
    @Transactional(readOnly = true)
    public CompanyDto getById(java.util.UUID companyId) {
        Company c = companyRepository.findById(companyId)
                .orElseThrow(() -> new NotFoundException("Company not found"));
        return CompanyDto.from(c);
    }
}