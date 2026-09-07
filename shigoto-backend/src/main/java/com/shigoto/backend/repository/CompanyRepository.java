package com.shigoto.backend.repository;

import com.shigoto.backend.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Declares application-specific persistence queries for company data.
 */
@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    /**
     * Finds a company by its exact name.
     * @param name the name to look up
     * @return the named company, if present
     */
    Optional<Company> findByName(String name);
}
