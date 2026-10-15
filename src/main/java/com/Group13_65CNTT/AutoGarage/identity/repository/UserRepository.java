package com.Group13_65CNTT.AutoGarage.identity.repository;

import com.Group13_65CNTT.AutoGarage.common.enums.Role;
import com.Group13_65CNTT.AutoGarage.identity.entity.User;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    default Optional<User> findByIdentifier(String identifier) {
        return identifier.contains("@") ? findByEmail(identifier) : findByUsername(identifier);
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findLockedById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.active = true AND u.deletedAt IS NULL ORDER BY u.id")
    List<User> lockActiveAdministrators(@Param("role") Role role);

    @Query(
            """
            SELECT u FROM User u
            WHERE u.deletedAt IS NULL AND (:keyword IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))
                AND (:active IS NULL OR u.active = :active)
            """)
    Page<User> searchUsers(
            @Param("keyword") String keyword, @Param("active") Boolean active, Pageable pageable);
}
