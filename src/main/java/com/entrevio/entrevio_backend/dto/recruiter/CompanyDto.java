package com.entrevio.entrevio_backend.dto.recruiter;

import com.entrevio.entrevio_backend.model.Company;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyDto {

    private UUID id;
    private UUID ownerUserId;
    private String name;
    private String website;
    private String description;

    public static CompanyDto from(Company c) {
        return CompanyDto.builder()
                .id(c.getId())
                .ownerUserId(c.getOwner().getId())
                .name(c.getName())
                .website(c.getWebsite())
                .description(c.getDescription())
                .build();
    }
}