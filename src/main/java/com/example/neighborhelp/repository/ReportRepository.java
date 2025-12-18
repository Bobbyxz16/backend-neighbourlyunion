package com.example.neighborhelp.repository;

import com.example.neighborhelp.entity.Report;
import com.example.neighborhelp.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long>, JpaSpecificationExecutor<Report> {

    Page<Report> findByStatus(Report.ReportStatus status, Pageable pageable);

    Page<Report> findByReporter(User reporter, Pageable pageable);

    @Query("SELECT COUNT(r) FROM Report r WHERE r.status = :status")
    long countByStatus(@Param("status") Report.ReportStatus status);

    @Query("SELECT COUNT(r) FROM Report r WHERE r.createdAt >= :startDate")
    long countByCreatedAtAfter(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT r.reportType, COUNT(r) FROM Report r GROUP BY r.reportType")
    Object[][] countByReportType();

    @Query("SELECT r.severity, COUNT(r) FROM Report r GROUP BY r.severity")
    Object[][] countBySeverity();
}