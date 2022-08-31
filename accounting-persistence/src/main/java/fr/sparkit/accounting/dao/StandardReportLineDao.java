package fr.sparkit.accounting.dao;

import java.util.List;
import java.util.Optional;

import fr.sparkit.accounting.entities.StandardReportLine;
import fr.sparkit.accounting.enumuration.ReportType;

public interface StandardReportLineDao extends BaseRepository<StandardReportLine, Long> {

    List<StandardReportLine> findByReportTypeAndIsDeletedFalseOrderById(ReportType reportType);

    StandardReportLine findByReportTypeAndLineIndexAndIsDeletedFalse(ReportType reportType, String index);

    Optional<StandardReportLine> findFirstByAnnexCodeAndReportType(String annexCode, ReportType reportType);
}
