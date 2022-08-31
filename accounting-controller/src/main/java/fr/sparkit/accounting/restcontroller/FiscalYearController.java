package fr.sparkit.accounting.restcontroller;

import java.time.LocalDateTime;
import java.util.List;

import javax.validation.Valid;

import fr.sparkit.accounting.auditing.HasRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.sparkit.accounting.constants.AccountingConstants;
import fr.sparkit.accounting.convertor.FiscalYearConvertor;
import fr.sparkit.accounting.dto.CloseAndReopeningFiscalYearDto;
import fr.sparkit.accounting.dto.Filter;
import fr.sparkit.accounting.dto.FiscalYearDto;
import fr.sparkit.accounting.entities.FiscalYear;
import fr.sparkit.accounting.services.IDepreciationAssetService;
import fr.sparkit.accounting.services.IFiscalYearClosingAndReOpeningService;
import fr.sparkit.accounting.services.IFiscalYearService;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/accounting/fiscalyear")
public class FiscalYearController {

    private final IFiscalYearService fiscalYearService;
    private final IFiscalYearClosingAndReOpeningService fiscalYearClosingAndReOpeningService;
    private final IDepreciationAssetService depreciationAssetService;

    @Autowired
    public FiscalYearController(IFiscalYearService fiscalYearService,
            IFiscalYearClosingAndReOpeningService fiscalYearClosingAndReOpeningService,
            IDepreciationAssetService depreciationAssetService) {
        this.fiscalYearService = fiscalYearService;
        this.fiscalYearClosingAndReOpeningService = fiscalYearClosingAndReOpeningService;
        this.depreciationAssetService = depreciationAssetService;
    }

    @GetMapping(value = "/{id}")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "ACCOUNTING", "VIEW_FISCAL_YEARS" })
    public FiscalYearDto findById(@PathVariable Long id) {
        return fiscalYearService.findById(id);
    }

    @GetMapping(value = "previous-fiscal-year/{id}")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "ACCOUNTING", "VIEW_FISCAL_YEARS" })
    public FiscalYearDto findPreviousFiscalYear(@PathVariable Long id) {
        return FiscalYearConvertor.modelToDto(fiscalYearService.findPreviousFiscalYear(id).orElse(new FiscalYear()));
    }

    @GetMapping(value = "/first-fiscal-year-not-concluded")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "ACCOUNTING", "VIEW_FISCAL_YEARS" })
    public FiscalYearDto firstFiscalYearNotConcluded() {
        return fiscalYearService.firstFiscalYearNotConcluded();
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "ADD_FISCAL_YEARS", "UPDATE_FISCAL_YEARS" })
    public FiscalYearDto saveOrUpdate(@RequestBody @Valid FiscalYearDto fiscalYearDto) {
        return fiscalYearService.saveOrUpdate(fiscalYearDto);
    }

    @GetMapping(value = "/fiscal-years")
    @HasRoles(permissions = { "ACCOUNTING", "VIEW_FISCAL_YEARS" })
    public List<FiscalYear> findAll() {
        return fiscalYearService.findAll();
    }

    @PostMapping(value = "/start-date-fiscalyear")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "ACCOUNTING", "VIEW_FISCAL_YEARS" })
    public LocalDateTime startDateFiscalYear() {
        return fiscalYearService.startDateFiscalYear();
    }

    @PostMapping(value = "/close/{id}")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "CLOSE_FISCAL_YEARS", "OPEN_FISCAL_YEARS" })
    public FiscalYear closeOrOpenFiscalYear(@PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime endDate,
            @RequestHeader(value = "User") String user) {
        return fiscalYearService.closePeriod(id, endDate, user);
    }

    @PostMapping(value = "/date-in-closed-period")
    @HasRoles(permissions = { "ACCOUNTING", "VIEW_FISCAL_YEARS", "UPDATE_ACTIVE" })
    public boolean isDateInClosedPeriod(
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime date) {
        return fiscalYearService.isDateInClosedPeriod(date);
    }

    @GetMapping(value = "/closed-fiscal-years")
    @HasRoles(permissions = { "ACCOUNTING", "VIEW_FISCAL_YEARS" })
    public List<FiscalYear> findClosedFiscalYears() {
        return fiscalYearService.findClosedFiscalYears();
    }

    @DeleteMapping(value = "/open-fiscal-year/{fiscalYearId}")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "OPEN_FISCAL_YEARS" })
    public FiscalYear openFiscalYear(@PathVariable Long fiscalYearId) {
        return fiscalYearService.openFiscalYear(fiscalYearId);
    }

    @PostMapping(value = "/close-and-reopening-fiscal-year")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "CONCLUDE_FISCAL_YEARS" })
    public boolean closeFiscalYear(@RequestHeader(value = "Content-Type") String contentType,
            @RequestHeader(value = "User") String user,
            @RequestHeader("Authorization") String authorization,
            @RequestBody CloseAndReopeningFiscalYearDto closeAndReopeningFiscalYear) {
        fiscalYearClosingAndReOpeningService.closeAndReOpenFiscalYear(contentType, user, authorization,
                closeAndReopeningFiscalYear);
        return true;
    }

    @GetMapping(value = "/all-fiscal-years-after-current-fiscal-year")
    @HasRoles(permissions = { "ACCOUNTING", "VIEW_FISCAL_YEARS" })
    public List<FiscalYear> findAllFiscalYearsAfterCurrentFiscalYear(
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime currentFiscalYearStartDate) {
        return fiscalYearService.findAllFiscalYearsAfterDate(currentFiscalYearStartDate);
    }

    @GetMapping(value = "/fiscal-years-not-closed")
    @HasRoles(permissions = { "ACCOUNTING", "VIEW_FISCAL_YEARS" })
    public List<FiscalYearDto> findAllFiscalYearsNotClosed() {
        return fiscalYearService.findAllFiscalYearsNotClosed();
    }

    @GetMapping(value = "/opening-target-fiscal-years")
    @HasRoles(permissions = { "ACCOUNTING", "VIEW_FISCAL_YEARS" })
    public List<FiscalYearDto> findOpeningTargets() {
        return fiscalYearService.findOpeningTargets();
    }

    @PostMapping(value = "/filter-fiscal-year")
    @HasRoles(permissions = { "ACCOUNTING", "VIEW_FISCAL_YEARS" })
    public Page<FiscalYearDto> filterFiscalYear(@RequestBody List<Filter> filters, Pageable pageable) {
        return fiscalYearService.filterFiscalYear(filters, pageable);
    }

    @PostMapping(value = "is-consomming-dates-in-fiscal-year")
    @HasRoles(permissions = { "ACCOUNTING", "VIEW_FISCAL_YEARS", "LIST_ACTIVE", "UPDATE_ACTIVE" })
    public List<Boolean> isConsommingDatesInFiscalYear(@RequestBody List<LocalDateTime> datesOfCommissioning) {
        return depreciationAssetService.isConsommingDatesInFiscalYear(datesOfCommissioning);
    }
}
