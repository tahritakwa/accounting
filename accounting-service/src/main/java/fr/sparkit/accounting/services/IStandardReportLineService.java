package fr.sparkit.accounting.services;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Service;

import fr.sparkit.accounting.dto.StandardReportLineDto;
import fr.sparkit.accounting.entities.StandardReportLine;
import fr.sparkit.accounting.enumuration.ReportType;

@Service
public interface IStandardReportLineService extends IGenericService<StandardReportLine, Long> {

    List<StandardReportLineDto> getReportLinesForReportType(ReportType reportType);

    StandardReportLine findByReportTypeAndLineIndexAndIsDeletedFalse(ReportType reportType, String index);

    List<StandardReportLine> findAllStandardReportLines();

    StandardReportLineDto updateStandardReport(StandardReportLineDto standardReportLine);

    StandardReportLine findById(Long id);

    void validateFormula(StandardReportLine standardReportLine);

    void checkForRepetitionsInFormula(Collection<Object> formulaElements, String formula);
}
