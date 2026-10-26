package com.entrevio.entrevio_backend.service;

import com.entrevio.entrevio_backend.dto.recruiter.CompanyDto;
import com.entrevio.entrevio_backend.dto.recruiter.UpdateCompanyRequest;
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

    @Transactional
    public CompanyDto getOrCreateMine() {
        User me = CurrentUser.requireRole(Role.RECRUITER);
        Company company = companyRepository.findByOwnerId(me.getId())
                .orElseGet(() -> companyRepository.save(
                        Company.builder()
                                .owner(me)
                                .name(me.getFullName() + "'s Company")
                                .build()
                ));
        return CompanyDto.from(company);
    }

    @Transactional
    public CompanyDto updateMine(UpdateCompanyRequest req) {
        User me = CurrentUser.requireRole(Role.RECRUITER);

        Company company = companyRepository.findByOwnerId(me.getId())
                .orElseGet(() -> Company.builder().owner(me).build());

        company.setName(req.getName());
        company.setWebsite(req.getWebsite());
        company.setDescription(req.getDescription());

        return CompanyDto.from(companyRepository.save(company));
    }
}