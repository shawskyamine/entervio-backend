package com.entrevio.entrevio_backend.service;

import com.entrevio.entrevio_backend.dto.candidate.CandidateProfileDto;
import com.entrevio.entrevio_backend.dto.candidate.UpdateCandidateProfileRequest;
import com.entrevio.entrevio_backend.exception.ValidationException;
import com.entrevio.entrevio_backend.model.CandidateProfile;
import com.entrevio.entrevio_backend.model.User;
import com.entrevio.entrevio_backend.model.enums.Role;
import com.entrevio.entrevio_backend.repository.CandidateProfileRepository;
import com.entrevio.entrevio_backend.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CandidateService {

    private final CandidateProfileRepository profileRepository;

    public CandidateService(CandidateProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @Transactional
    public CandidateProfileDto getOrCreateMine() {
        User me = CurrentUser.requireRole(Role.CANDIDATE);
        CandidateProfile profile = profileRepository.findByUserId(me.getId())
                .orElseGet(() -> profileRepository.save(
                        CandidateProfile.builder().user(me).build()
                ));
        return CandidateProfileDto.from(profile);
    }

    @Transactional
    public CandidateProfileDto updateMine(UpdateCandidateProfileRequest req) {
        User me = CurrentUser.requireRole(Role.CANDIDATE);

        if (req.getYearsExperience() != null && req.getYearsExperience() < 0) {
            throw new ValidationException("yearsExperience must be >= 0");
        }

        CandidateProfile profile = profileRepository.findByUserId(me.getId())
                .orElseGet(() -> CandidateProfile.builder().user(me).build());

        profile.setHeadline(req.getHeadline());
        profile.setLocation(req.getLocation());
        profile.setYearsExperience(req.getYearsExperience());
        profile.setSkills(req.getSkills());

        return CandidateProfileDto.from(profileRepository.save(profile));
    }
}