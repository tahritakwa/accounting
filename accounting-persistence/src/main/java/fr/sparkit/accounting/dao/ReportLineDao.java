package fr.sparkit.accounting.dao;

import java.util.List;
import java.util.Optional;

import fr.sparkit.accounting.entities.ReportLine;
import fr.sparkit.accounting.enumuration.ReportType;

public interface ReportLineDao extends BaseRepository<ReportLine, Long> {

    List<ReportLine> findByReportTypeAndFiscalYearIdAndIsDeletedFalseOrderById(ReportType reportType,
            Long fiscalYearId);

    List<ReportLine> findByFiscalYearIdAndIsDeletedFalseOrderById(Long fiscalYearId);

    Optional<ReportLine> findByReportTypeAndLineIndexAndFiscalYearIdAndIsDeletedFalse(ReportType reportType,
            String index, Long fiscalYearId);

    Optional<ReportLine> findFirstByAnnexCodeAndReportTypeAndFiscalYearId(String annexCode, ReportType reportType,
            Long fiscalYearId);

    Optional<ReportLine> findFirstByReportTypeAndFiscalYearId(ReportType reportType, Long fiscalYearId);

    boolean existsByIdAndIsDeletedFalse(Long reportLineId);

    List<ReportLine> findByReportTypeAndFiscalYearIdAndIsDeletedFalseAndIdInOrderById(ReportType reportType,
            Long fiscalYearId, List<Long> ids);
}
