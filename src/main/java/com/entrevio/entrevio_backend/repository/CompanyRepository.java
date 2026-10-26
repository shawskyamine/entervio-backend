package com.entrevio.entrevio_backend.repository;

import com.entrevio.entrevio_backend.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyRepository extends JpaRepository<Company, UUID> {

    Optional<Company> findByOwnerId(UUID ownerId);

    boolean existsByOwnerId(UUID ownerId);

    Optional<Company> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}