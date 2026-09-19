package com.meridiantrust.sentinel.repository;

import com.meridiantrust.sentinel.domain.AmlCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AmlCaseRepository extends JpaRepository<AmlCase, UUID> {
}
