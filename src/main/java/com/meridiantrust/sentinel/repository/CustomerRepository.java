package com.meridiantrust.sentinel.repository;

import com.meridiantrust.sentinel.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findByCustomerRef(String customerRef);
}
