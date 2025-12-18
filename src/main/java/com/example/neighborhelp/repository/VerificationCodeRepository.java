package com.example.neighborhelp.repository;

import com.example.neighborhelp.entity.User;
import com.example.neighborhelp.entity.VerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {

    Optional<VerificationCode> findByCodeAndUsedFalse(String code);

    Optional<VerificationCode> findByUserAndUsedFalseAndType(
            User user,
            VerificationCode.VerificationType type
    );

    @Modifying
    @Query("DELETE FROM VerificationCode v WHERE v.expiryDate < :now")
    void deleteExpiredCodes(LocalDateTime now);

    @Modifying
    @Query("DELETE FROM VerificationCode v WHERE v.user = :user AND v.type = :type")
    void deleteByUserAndType(@Param("user") User user, @Param("type") VerificationCode.VerificationType type);

    // Add @Param to any other methods with named parameters
    @Query("SELECT vc FROM VerificationCode vc WHERE vc.user = :user AND vc.type = :type AND vc.used = false")
    VerificationCode findByUserAndTypeAndUsedFalse(@Param("user") User user, @Param("type") VerificationCode.VerificationType type);
}