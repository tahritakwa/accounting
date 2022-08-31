package fr.sparkit.accounting.restcontroller;

import java.util.List;

import javax.validation.Valid;

import fr.sparkit.accounting.auditing.HasRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import fr.sparkit.accounting.constants.AccountingConstants;
import fr.sparkit.accounting.dto.ChartAccountsDto;
import fr.sparkit.accounting.dto.ChartAccountsToBalancedDto;
import fr.sparkit.accounting.dto.FileUploadDto;
import fr.sparkit.accounting.entities.ChartAccounts;
import fr.sparkit.accounting.services.IChartAccountsService;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/accounting/chart-account")
public class ChartAccountsController {

    private final IChartAccountsService chartAccountsService;

    @Autowired
    public ChartAccountsController(IChartAccountsService chartAccountsService) {
        this.chartAccountsService = chartAccountsService;
    }

    @GetMapping(value = "/charts")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "VIEW_CHART_OF_ACCOUNTS" })
    public List<ChartAccountsDto> findAllCharts() {
        return chartAccountsService.findAllCharts();
    }

    @GetMapping(value = "/search-by-label")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "VIEW_CHART_OF_ACCOUNTS" })
    public List<ChartAccountsDto> findSubTreeById(@RequestParam String value) {
        return chartAccountsService.findSubTreeByLabelOrCode(value);
    }

    @GetMapping(value = "/search-by-code")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "VIEW_CHART_OF_ACCOUNTS" })
    public ChartAccountsDto findByCode(@RequestParam int code) {
        return chartAccountsService.findByCode(code);
    }

    @GetMapping(value = "/search-by-code-iteration")
    public ChartAccountsDto findByCodeIteration(@RequestParam int code) {
        return chartAccountsService.findByCodeIteration(code);
    }

    @GetMapping(value = "/{id}")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "VIEW_CHART_OF_ACCOUNTS" })
    public ChartAccountsDto findById(@PathVariable Long id) {
        return chartAccountsService.findById(id);
    }

    @GetMapping(value = "/build-charts-account")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "DELETE_CHART_OF_ACCOUNTS", "VIEW_CHART_OF_ACCOUNTS" })
    public List<ChartAccountsDto> buildAllTree() {
        return chartAccountsService.buildAllTree();
    }

    @GetMapping(value = "/search-by-codes")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "VIEW_CHART_OF_ACCOUNTS" })
    public List<ChartAccountsDto> findByCodes(@RequestParam int code) {
        return chartAccountsService.findByCodes(code);
    }

    @DeleteMapping(value = "/{id}")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "DELETE_CHART_OF_ACCOUNTS" })
    public boolean deleteSubTree(@PathVariable Long id) {
        return chartAccountsService.deleteSubTree(id);
    }

    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "ADD_CHART_OF_ACCOUNTS" })
    public ChartAccounts save(@RequestBody @Valid ChartAccountsDto chartAccountsDto) {
        return chartAccountsService.save(chartAccountsDto);
    }

    @PutMapping(value = "/{id}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('EDIT_CHART_ACCOUNT')")
    public ChartAccounts update(@RequestBody @Valid ChartAccountsDto accountingPlanDto) {
        return chartAccountsService.update(accountingPlanDto);
    }

    @PostMapping(value = "/chart-account-to-balanced")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "UPDATE_CHART_OF_ACCOUNTS" })
    public void balanceChartAccounts(@RequestBody List<Long> chartAccounts) {
        chartAccountsService.balanceChartAccounts(chartAccounts);
    }

    @GetMapping(value = "/chart-account-to-balanced")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "VIEW_CHART_OF_ACCOUNTS" })
    public ChartAccountsToBalancedDto getChartAccountsToBalanced() {
        return chartAccountsService.getChartAccountsToBalanced();
    }

    @GetMapping(value = "/excel-template")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "EXPORT_MODEL_CHART_OF_ACCOUNTS" })
    public ResponseEntity<byte[]> exportModel() {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add(HttpHeaders.CONTENT_TYPE, AccountingConstants.EXCEL_DOC_TYPE);
        return new ResponseEntity<>(chartAccountsService.exportChartAccountsExcelModel(), responseHeaders,
                HttpStatus.OK);
    }

    @PostMapping(value = "/import-charts")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "IMPORT_CHART_OF_ACCOUNTS" })
    public ResponseEntity<FileUploadDto> importChartAccountsFromExcelFile(@RequestBody FileUploadDto fileUploadDto) {
        return new ResponseEntity<>(chartAccountsService.loadChartAccountsExcelData(fileUploadDto), HttpStatus.OK);
    }

    @GetMapping(value = "/export-charts")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "EXPORT_CHART_OF_ACCOUNTS" })
    public ResponseEntity<byte[]> exportChartsAsExcelFile() {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add(HttpHeaders.CONTENT_TYPE, AccountingConstants.EXCEL_DOC_TYPE);
        return new ResponseEntity<>(chartAccountsService.exportChartAccountsAsExcelFile(), responseHeaders,
                HttpStatus.OK);
    }

}
