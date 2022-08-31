package fr.sparkit.accounting.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.sparkit.accounting.dto.Filter;
import fr.sparkit.accounting.dto.FiscalYearDto;
import fr.sparkit.accounting.entities.FiscalYear;

public interface IFiscalYearService extends IGenericService<FiscalYear, Long> {

    FiscalYearDto findById(Long id);

    Long findFiscalYearOfDate(LocalDateTime ldt);

    FiscalYearDto saveOrUpdate(FiscalYearDto fiscalYearDto);

    Optional<FiscalYear> findPreviousFiscalYear(Long fiscalYearId);

    LocalDateTime startDateFiscalYear();

    boolean existsById(Long fiscalYearId);

    FiscalYear closePeriod(Long fiscalYearId, LocalDateTime closingDate, String user);

    int getClosingStateForFiscalYear(FiscalYear fiscalYear);

    List<FiscalYear> findClosedFiscalYears();

    FiscalYear openFiscalYear(Long fiscalYearId);

    void generateAllAccountingReports(Long fiscalYearId, String user);

    void closeFiscalYear(Long fiscalYearId);

    List<FiscalYear> findAllFiscalYearsAfterDate(LocalDateTime fiscalYearStartDate);

    FiscalYear findLastFiscalYearByEndDate();

    void checkPreviousFiscalYearsAreConcluded(FiscalYear currentFiscalYear);

    List<FiscalYearDto> findAllFiscalYearsNotClosed();

    FiscalYearDto firstFiscalYearNotConcluded();

    String getDefaultSortFieldForFiscalYear();

    List<FiscalYearDto> findOpeningTargets();

    LocalDateTime getStartDateOfFirstFiscalYear();

    boolean isDateInClosedPeriod(LocalDateTime date);

    boolean isDateInClosedPeriod(LocalDateTime date, Long fiscalYearId);

    Page<FiscalYearDto> filterFiscalYear(List<Filter> filters, Pageable pageable);

    boolean isDateInFiscalYear(LocalDateTime date, Long fiscalYearId);

    Optional<FiscalYear> findNextFiscalYear(Long fiscalYearId);
}
