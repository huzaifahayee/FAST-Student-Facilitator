package com.fast.fsf.repository;

import com.fast.fsf.model.PaperReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaperReportRepository extends JpaRepository<PaperReport, Long> {
    List<PaperReport> findByResolvedFalse();
    List<PaperReport> findByPaperId(Long paperId);
    long countByPaperIdAndResolvedFalse(Long paperId);
}
