package fr.sparkit.accounting.restcontroller;

import java.util.List;

import javax.validation.Valid;

import fr.sparkit.accounting.auditing.HasRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.sparkit.accounting.dto.AccountingConfigurationDto;
import fr.sparkit.accounting.dto.AccountingEnvironmentDto;
import fr.sparkit.accounting.dto.DepreciationAssetsConfigurationDto;
import fr.sparkit.accounting.dto.FiscalYearDto;
import fr.sparkit.accounting.entities.DepreciationAssetsConfiguration;
import fr.sparkit.accounting.services.IAccountingConfigurationService;
import fr.sparkit.accounting.services.IDepreciationAssetConfigurationService;
import fr.sparkit.accounting.services.IDepreciationAssetService;
import fr.sparkit.accounting.services.impl.AccountingConfigurationService;
import fr.sparkit.accounting.services.impl.DepreciationAssetConfigurationService;
import fr.sparkit.accounting.util.DBConfig;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/accounting/configuration")
public class AccountingConfigurationController {

    private final IAccountingConfigurationService accountingConfigurationService;
    private final IDepreciationAssetConfigurationService depreciationAssetConfigurationService;
    private final IDepreciationAssetService depreciationAssetService;
    private final DBConfig dbConfig;

    @Autowired
    public AccountingConfigurationController(AccountingConfigurationService accountingConfigurationService,
            DepreciationAssetConfigurationService depreciationAssetConfigurationService,
            IDepreciationAssetService depreciationAssetService, DBConfig dbConfig) {
        this.accountingConfigurationService = accountingConfigurationService;
        this.depreciationAssetConfigurationService = depreciationAssetConfigurationService;
        this.depreciationAssetService = depreciationAssetService;
        this.dbConfig = dbConfig;
    }

    @GetMapping(value = "build-properties")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "ACCOUNTING","ACCOUNTING_SETTINGS" })
    public AccountingEnvironmentDto buildProperties() {
        return accountingConfigurationService.getBuildProperties();
    }

    @GetMapping()
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "ACCOUNTING","ACCOUNTING_SETTINGS" })
    public AccountingConfigurationDto findLastConfig() {
        return accountingConfigurationService.findLastConfig();
    }

    @GetMapping(value = "current-fiscal-year")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "ACCOUNTING","ACCOUNTING_SETTINGS", "VIEW_FISCAL_YEARS" })
    public FiscalYearDto getCurrentFiscalYear() {
        return this.accountingConfigurationService.getCurrentFiscalYear();
    }

    @GetMapping(value = "refresh-db")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "ACCOUNTING_SETTINGS" })
    public boolean refreshDataSource() {
        dbConfig.dataSource();
        return true;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "ACCOUNTING_SETTINGS" })
    public AccountingConfigurationDto save(@RequestBody @Valid AccountingConfigurationDto accountingConfigurationDto) {
        return accountingConfigurationService.saveConfiguration(accountingConfigurationDto);
    }

    @GetMapping(value = "depreciation-assets-configuration")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "ACCOUNTING","ACCOUNTING_SETTINGS" })
    public List<DepreciationAssetsConfiguration> getAllDepreciationAssetsConfiguration() {
        return depreciationAssetConfigurationService.findAll();
    }

    @GetMapping(value = "depreciation-assets-configuration/{idCategory}")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "ACCOUNTING","ACCOUNTING_SETTINGS" })
    public DepreciationAssetsConfiguration getDepreciationAssetsConfigurationByIdCategory(
            @PathVariable Long idCategory) {
        return depreciationAssetConfigurationService.findByIdCategory(idCategory);
    }

    @DeleteMapping(value = "/{idCategory}")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "ACCOUNTING_SETTINGS","ADD_ACTIVE","UPDATE_ACTIVE" })
    public boolean delete(@PathVariable Long idCategory) {
        return depreciationAssetConfigurationService.deleteByIdCategory(idCategory);
    }

    @PostMapping(value = "depreciation-assets-configuration")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "ACCOUNTING_SETTINGS" })
    public DepreciationAssetsConfiguration saveOrUpdateDepreciationAssetsConfiguration(
            @RequestBody DepreciationAssetsConfigurationDto depreciationAssetsConfigurationDto) {
        return depreciationAssetConfigurationService
                .saveOrUpdateDepreciationAssetConfiguration(depreciationAssetsConfigurationDto);
    }

    @GetMapping(value = "current-fiscal-year-configuration/{id}")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "ACCOUNTING","ACCOUNTING_SETTINGS" })
    public FiscalYearDto updateCurrentFiscalYear(@RequestBody @PathVariable Long id) {
        return accountingConfigurationService.updateCurrentFiscalYear(id);
    }

    @DeleteMapping(value = "reset-depreciation-assets-configuration")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "ACCOUNTING_SETTINGS" })
    public boolean deleteAllDepreciationAssetsConfiguration() {
        return depreciationAssetConfigurationService.deleteAllDepreciationAssetsConfiguration();
    }

    @DeleteMapping(value = "reset-depreciation-assets")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "ACCOUNTING_SETTINGS" })
    public boolean deleteAllDepreciationAssets() {
        return depreciationAssetService.deleteAllDepreciationAssets();
    }

    @PostMapping(value = "depreciation-assets-configuration/{idCategory}")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "ACCOUNTING_SETTINGS" })
    public void saveDepreciationsAssetsByIdCategory(
            @RequestHeader("User") String user, @RequestHeader("Content-Type") String contentType,
            @RequestHeader("Authorization") String authorization, @RequestBody @PathVariable Long idCategory) {
        depreciationAssetService.saveDepreciationsAssetsByIdCategory(user, contentType, authorization,
                idCategory);
    }
}
