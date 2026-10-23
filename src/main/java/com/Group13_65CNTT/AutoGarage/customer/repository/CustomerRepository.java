package com.Group13_65CNTT.AutoGarage.customer.repository;

import com.Group13_65CNTT.AutoGarage.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByCustomerCode(String customerCode);

    Optional<Customer> findByPhone(String phone);

    Optional<Customer> findByUserId(Long userId);

    Optional<Customer> findFirstByEmailIgnoreCaseAndUserIdIsNullOrderByIdAsc(String email);

    boolean existsByCustomerCode(String customerCode);

    boolean existsByPhone(String phone);

    boolean existsByPhoneAndIdNot(String phone, Long id);

    @Query("SELECT c FROM Customer c WHERE " +
           "(:search IS NULL OR LOWER(c.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR c.phone LIKE CONCAT('%', :search, '%') " +
           "OR LOWER(c.customerCode) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:isActive IS NULL OR c.isActive = :isActive) " +
           "ORDER BY c.id DESC")
    List<Customer> searchCustomers(@Param("search") String search, @Param("isActive") Boolean isActive);
}
