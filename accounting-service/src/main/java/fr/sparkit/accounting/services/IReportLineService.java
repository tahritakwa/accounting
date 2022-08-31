package fr.sparkit.accounting.services;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import fr.sparkit.accounting.entities.Account;
import fr.sparkit.accounting.entities.ChartAccounts;
import org.springframework.stereotype.Service;

import fr.sparkit.accounting.dto.AmortizationTableReportDto;
import fr.sparkit.accounting.dto.AnnexeDetailsDto;
import fr.sparkit.accounting.dto.AnnexeReportDto;
import fr.sparkit.accounting.dto.ReportLineDto;
import fr.sparkit.accounting.entities.ReportLine;
import fr.sparkit.accounting.enumuration.ReportType;

@Service
public interface IReportLineService extends IGenericService<ReportLine, Long> {

    List<ReportLineDto> getReportLinesForReportType(ReportType reportType);

    void validateFormula(ReportLine reportLine);

    ReportLineDto saveReportLine(ReportLineDto reportLine, String user);

    ReportLineDto findById(Long id);

    BigDecimal interpretSimpleFormula(Collection<String> formula, Long fiscalYearId,
            List<ReportLine> previousFiscalYearReportLines, List<ChartAccounts> chartAccounts, List<Account> accounts,
            boolean isCashFlowReport);

    BigDecimal interpretComplexFormula(Collection<Integer> formula, List<ReportLineDto> reportLines);

    ReportLine getReportLineByReportTypeAndLineIndexAndFiscalYear(ReportType reportType, String index,
            Long fiscalYearId);

    void checkForRepetitionsInFormula(Collection<Object> formulaElements, String formula);

    List<ReportLineDto> generateAnnualReport(ReportType reportType, Long fiscalYearId, String user);

    List<AnnexeDetailsDto> findAnnexDetails(ReportType reportType, Long fiscalYearId);

    List<AnnexeReportDto> generateAnnualReportAnnex(ReportType reportType, Long fiscalYearId);

    List<ReportLineDto> initReportLinesForFiscalYear(ReportType reportType, Long fiscalYearId);

    void resetReportLinesForFiscalYear(Long fiscalYearId);

    List<ReportLine> findByFiscalYearIdAndIsDeletedFalseOrderById(Long fiscalYearId);

    List<String> setConfigurationCookie(String user, String contentType, String authorization);

    List<AmortizationTableReportDto> generateAmortizationReport(Long fiscalYearId, String user,
            String authorization, String contentType);

    List<ReportLineDto> getAnnualReport(ReportType reportType, Long fiscalYearId);

    ReportLineDto resetReportLine(Long reportLineId, String user);

    void checkAccountsBalanced(Long fiscalYearId);

    List<Integer> getUnbalancedChartAccounts(Long fiscalYearId);

    boolean existsById(Long reportLineId);
}
