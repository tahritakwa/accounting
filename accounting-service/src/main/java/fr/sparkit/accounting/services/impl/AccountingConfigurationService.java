package fr.sparkit.accounting.services.impl;

import static fr.sparkit.accounting.constants.AccountingConstants.ENTITY_NAME_ACCOUNTING_CONFIG;
import static fr.sparkit.accounting.constants.AccountingConstants.LOG_ENTITY_CREATED;
import static fr.sparkit.accounting.constants.AccountingConstants.LOG_ENTITY_DELETED;
import static fr.sparkit.accounting.constants.AccountingConstants.MISSING_PARAMETERS_IN_ACCOUNTING_CONFIGURATION;
import static fr.sparkit.accounting.constants.AccountingConstants.NO_ACCOUNTING_CONFIGURATION_FOUND;
import static fr.sparkit.accounting.constants.AccountingConstants.TRYING_TO_SAVE_ACC_CONFIG_WITH_NO_EXISTING_CHART_ACC;
import static fr.sparkit.accounting.constants.AccountingConstants.TRYING_TO_SAVE_ACC_CONFIG_WITH_NO_EXISTING_CODE;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import fr.sparkit.accounting.convertor.AccountingConfigurationConvertor;
import fr.sparkit.accounting.convertor.JournalConverter;
import fr.sparkit.accounting.dao.AccountingConfigurationDao;
import fr.sparkit.accounting.dto.AccountingConfigurationDto;
import fr.sparkit.accounting.dto.AccountingEnvironmentDto;
import fr.sparkit.accounting.dto.FiscalYearDto;
import fr.sparkit.accounting.entities.Account;
import fr.sparkit.accounting.entities.AccountingConfiguration;
import fr.sparkit.accounting.entities.ChartAccounts;
import fr.sparkit.accounting.entities.Journal;
import fr.sparkit.accounting.services.IAccountService;
import fr.sparkit.accounting.services.IAccountingConfigurationService;
import fr.sparkit.accounting.services.IChartAccountsService;
import fr.sparkit.accounting.services.IFiscalYearService;
import fr.sparkit.accounting.services.IJournalService;
import fr.sparkit.accounting.services.IUserPreferenceService;
import fr.sparkit.accounting.util.errors.ApiErrors;
import fr.sparkit.accounting.util.errors.ErrorsResponse;
import fr.sparkit.accounting.util.http.HttpCustomException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AccountingConfigurationService extends GenericService<AccountingConfiguration, Long>
        implements IAccountingConfigurationService {

    private final IAccountService accountService;
    private final IFiscalYearService fiscalYearService;
    private final IJournalService journalService;
    private final IChartAccountsService chartaccountService;
    private final AccountingConfigurationDao accountingConfigurationDao;
    private final IUserPreferenceService userPreferenceService;

    private BuildProperties buildProperties;
    @Value("${tomcat.server.url}")
    private String accountingServerUrl;

    @Autowired
    public AccountingConfigurationService(AccountingConfigurationDao accountingConfigurationDao,
            IAccountService accountService, IFiscalYearService fiscalYearService, IJournalService journalService,
            IChartAccountsService chartAccountService, IUserPreferenceService userPreferenceService,
            BuildProperties buildProperties) {
        this.accountingConfigurationDao = accountingConfigurationDao;
        this.accountService = accountService;
        this.fiscalYearService = fiscalYearService;
        this.journalService = journalService;
        this.chartaccountService = chartAccountService;
        this.userPreferenceService = userPreferenceService;
        this.buildProperties = buildProperties;
    }

    @Override
    @Cacheable(value = "AccountingConfigurationCache", key = "'AccountingConfigurationCache_'"
            + "+T(fr.sparkit.accounting.util.CompanyContextHolder).getCompanyContext()", unless = "#result==null")
    public AccountingConfigurationDto findLastConfig() {
        AccountingConfiguration accountingConfiguration = accountingConfigurationDao
                .findFirstByIsDeletedFalseOrderByIdDesc();
        if (accountingConfiguration != null) {
            return AccountingConfigurationConvertor.modelToDto(accountingConfiguration);
        }
        log.error(NO_ACCOUNTING_CONFIGURATION_FOUND);
        throw new HttpCustomException(ApiErrors.Accounting.ACCOUNTING_CONFIGURATION_NO_CONFIGURATION_FOUND);
    }

    @Override
    public Long getCurrentFiscalYearId() {
        return getCurrentFiscalYear().getId();
    }

    @Override
    public FiscalYearDto getCurrentFiscalYear() {
        return userPreferenceService.getCurrentFiscalYear();
    }

    @Override
    public AccountingEnvironmentDto getBuildProperties() {
        log.info(accountingServerUrl);
        return AccountingEnvironmentDto.builder().buildVersion(buildProperties.getVersion())
                .accountingUrl(accountingServerUrl).build();
    }

    @Override
    @CacheEvict(value = "AccountingConfigurationCache", allEntries = true)
    public FiscalYearDto updateCurrentFiscalYear(Long fiscalYearId) {
        return userPreferenceService.saveCurrentFiscalYear(fiscalYearId);
    }

    @Override
    public boolean isDateInCurrentFiscalYear(LocalDateTime date) {
        Long fiscalYearOfDate = fiscalYearService.findFiscalYearOfDate(date);
        return fiscalYearOfDate != null && fiscalYearOfDate.equals(getCurrentFiscalYearId());
    }

    void checkValuesNotNull(AccountingConfigurationDto accountingConfigurationDto) {
        if (accountingConfigurationDto.getTaxStampIdAccountingAccountPurchase() == null
                || accountingConfigurationDto.getTaxStampIdAccountingAccountSales() == null) {
            log.error(MISSING_PARAMETERS_IN_ACCOUNTING_CONFIGURATION);
            throw new HttpCustomException(ApiErrors.Accounting.ACCOUNTING_CONFIGURATION_MISSING_PARAMETERS);
        }
    }

    // TODO refactor this method
    @Override
    @CacheEvict(value = "AccountingConfigurationCache", allEntries = true)
    public AccountingConfigurationDto saveConfiguration(AccountingConfigurationDto accountingConfigurationDto) {
        checkValuesNotNull(accountingConfigurationDto);
        Map<Long, Account> configurationAccounts = new HashMap<>();
        Map<Long, ChartAccounts> configurationChartAccounts = new HashMap<>();

        initConfigurationAccounts(configurationAccounts, accountingConfigurationDto);
        initConfigurationChartAccounts(configurationChartAccounts, accountingConfigurationDto);

        Long[] nonExistingAccountsIds = configurationAccounts.entrySet().stream().filter(id -> id.getValue() == null)
                .map(Map.Entry::getKey).toArray(Long[]::new);

        if (nonExistingAccountsIds.length != 0) {
            log.error(TRYING_TO_SAVE_ACC_CONFIG_WITH_NO_EXISTING_CODE, Arrays.toString(nonExistingAccountsIds));
            throw new HttpCustomException(ApiErrors.Accounting.ACCOUNTING_CONFIGURATION_ACCOUNTS_NOT_FOUND,
                    new ErrorsResponse().error(Arrays.toString(nonExistingAccountsIds)));
        }

        Long[] nonExistingChartAccountsIds = configurationChartAccounts.entrySet().stream()
                .filter(id -> id.getValue() == null).map(Map.Entry::getKey).toArray(Long[]::new);

        if (nonExistingChartAccountsIds.length != 0) {
            log.error(TRYING_TO_SAVE_ACC_CONFIG_WITH_NO_EXISTING_CHART_ACC,
                    Arrays.toString(nonExistingChartAccountsIds));
            throw new HttpCustomException(ApiErrors.Accounting.ACCOUNTING_CONFIGURATION_CHART_ACCOUNTS_NOT_FOUND,
                    new ErrorsResponse().error(Arrays.toString(nonExistingChartAccountsIds)));
        }

        Journal journalANew = JournalConverter
                .dtoToModel(journalService.findById(accountingConfigurationDto.getJournalANewId()));
        Journal journalSales = JournalConverter
                .dtoToModel(journalService.findById(accountingConfigurationDto.getJournalSalesId()));
        Journal journalPurchases = JournalConverter
                .dtoToModel(journalService.findById(accountingConfigurationDto.getJournalPurchasesId()));
        Journal journalCoffer = JournalConverter
                .dtoToModel(journalService.findById(accountingConfigurationDto.getJournalCofferId()));
        Journal journalBank = JournalConverter
                .dtoToModel(journalService.findById(accountingConfigurationDto.getJournalBankId()));

        AccountingConfiguration previousAccountingConfiguration = accountingConfigurationDao
                .findFirstByIsDeletedFalseOrderByIdDesc();

        AccountingConfiguration newAccountingConfiguration = saveAndFlush(AccountingConfigurationConvertor.dtoToModel(
                configurationAccounts.get(accountingConfigurationDto.getTaxStampIdAccountingAccountSales()),
                configurationAccounts.get(accountingConfigurationDto.getTaxStampIdAccountingAccountPurchase()),
                configurationAccounts.get(accountingConfigurationDto.getDiscountIdAccountingAccountSales()),
                configurationAccounts.get(accountingConfigurationDto.getDiscountIdAccountingAccountPurchase()),
                configurationChartAccounts.get(accountingConfigurationDto.getWithHoldingTaxIdAccountingAccount()),
                configurationChartAccounts.get(accountingConfigurationDto.getCofferIdAccountingAccount()),
                configurationChartAccounts.get(accountingConfigurationDto.getIntermediateIdAccountingAccount()),
                configurationChartAccounts.get(accountingConfigurationDto.getBankIdAccountingAccount()),
                configurationChartAccounts.get(accountingConfigurationDto.getCustomerAccount()),
                configurationChartAccounts.get(accountingConfigurationDto.getSupplierAccount()),
                configurationChartAccounts.get(accountingConfigurationDto.getTaxSalesAccount()),
                configurationChartAccounts.get(accountingConfigurationDto.getHtaxSalesAccount()),
                configurationChartAccounts.get(accountingConfigurationDto.getTaxPurchasesAccount()),
                configurationChartAccounts.get(accountingConfigurationDto.getHtaxPurchasesAccount()),
                configurationChartAccounts.get(accountingConfigurationDto.getFodecPurchasesAccount()),
                configurationChartAccounts.get(accountingConfigurationDto.getFodecSalesAccount()), journalANew,
                journalSales, journalPurchases, journalCoffer, journalBank,
                configurationChartAccounts.get(accountingConfigurationDto.getResultAccount()),
                configurationChartAccounts.get(accountingConfigurationDto.getTangibleImmobilizationAccount()),
                configurationChartAccounts.get(accountingConfigurationDto.getTangibleAmortizationAccount()),
                configurationChartAccounts.get(accountingConfigurationDto.getIntangibleImmobilizationAccount()),
                configurationChartAccounts.get(accountingConfigurationDto.getIntangibleAmortizationAccount()),
                configurationChartAccounts.get(accountingConfigurationDto.getDotationAmortizationAccount())));

        if (previousAccountingConfiguration != null) {
            delete(previousAccountingConfiguration);
            log.info(LOG_ENTITY_DELETED, ENTITY_NAME_ACCOUNTING_CONFIG, previousAccountingConfiguration.getId());
        }

        log.info(LOG_ENTITY_CREATED, newAccountingConfiguration);
        return AccountingConfigurationConvertor.modelToDto(newAccountingConfiguration);
    }

    private void initConfigurationAccounts(Map<Long, Account> configurationAccounts,
            AccountingConfigurationDto accountingConfigurationDto) {
        configurationAccounts.replaceAll((accountId, account) -> accountService.findOne(accountId));

        configurationAccounts.put(accountingConfigurationDto.getTaxStampIdAccountingAccountSales(),
                accountService.findOne(accountingConfigurationDto.getTaxStampIdAccountingAccountSales()));
        configurationAccounts.put(accountingConfigurationDto.getTaxStampIdAccountingAccountPurchase(),
                accountService.findOne(accountingConfigurationDto.getTaxStampIdAccountingAccountPurchase()));
        configurationAccounts.put(accountingConfigurationDto.getDiscountIdAccountingAccountSales(),
                accountService.findOne(accountingConfigurationDto.getDiscountIdAccountingAccountSales()));
        configurationAccounts.put(accountingConfigurationDto.getDiscountIdAccountingAccountPurchase(),
                accountService.findOne(accountingConfigurationDto.getDiscountIdAccountingAccountPurchase()));
    }

    private void initConfigurationChartAccounts(Map<Long, ChartAccounts> configurationChartAccounts,
            AccountingConfigurationDto accountingConfigurationDto) {
        configurationChartAccounts.put(accountingConfigurationDto.getWithHoldingTaxIdAccountingAccount(),
                chartaccountService.findOne(accountingConfigurationDto.getWithHoldingTaxIdAccountingAccount()));
        configurationChartAccounts.put(accountingConfigurationDto.getCofferIdAccountingAccount(),
                chartaccountService.findOne(accountingConfigurationDto.getCofferIdAccountingAccount()));
        configurationChartAccounts.put(accountingConfigurationDto.getIntermediateIdAccountingAccount(),
                chartaccountService.findOne(accountingConfigurationDto.getIntermediateIdAccountingAccount()));
        configurationChartAccounts.put(accountingConfigurationDto.getBankIdAccountingAccount(),
                chartaccountService.findOne(accountingConfigurationDto.getBankIdAccountingAccount()));
        configurationChartAccounts.put(accountingConfigurationDto.getCustomerAccount(),
                chartaccountService.findOne(accountingConfigurationDto.getCustomerAccount()));
        configurationChartAccounts.put(accountingConfigurationDto.getSupplierAccount(),
                chartaccountService.findOne(accountingConfigurationDto.getSupplierAccount()));
        configurationChartAccounts.put(accountingConfigurationDto.getTaxSalesAccount(),
                chartaccountService.findOne(accountingConfigurationDto.getTaxSalesAccount()));
        configurationChartAccounts.put(accountingConfigurationDto.getHtaxSalesAccount(),
                chartaccountService.findOne(accountingConfigurationDto.getHtaxSalesAccount()));
        configurationChartAccounts.put(accountingConfigurationDto.getTaxPurchasesAccount(),
                chartaccountService.findOne(accountingConfigurationDto.getTaxPurchasesAccount()));
        configurationChartAccounts.put(accountingConfigurationDto.getHtaxPurchasesAccount(),
                chartaccountService.findOne(accountingConfigurationDto.getHtaxPurchasesAccount()));
        configurationChartAccounts.put(accountingConfigurationDto.getFodecPurchasesAccount(),
                chartaccountService.findOne(accountingConfigurationDto.getFodecPurchasesAccount()));
        configurationChartAccounts.put(accountingConfigurationDto.getFodecSalesAccount(),
                chartaccountService.findOne(accountingConfigurationDto.getFodecSalesAccount()));
        configurationChartAccounts.put(accountingConfigurationDto.getResultAccount(),
                chartaccountService.findOne(accountingConfigurationDto.getResultAccount()));
        configurationChartAccounts.put(accountingConfigurationDto.getTangibleImmobilizationAccount(),
                chartaccountService.findOne(accountingConfigurationDto.getTangibleImmobilizationAccount()));
        configurationChartAccounts.put(accountingConfigurationDto.getTangibleAmortizationAccount(),
                chartaccountService.findOne(accountingConfigurationDto.getTangibleAmortizationAccount()));
        configurationChartAccounts.put(accountingConfigurationDto.getIntangibleImmobilizationAccount(),
                chartaccountService.findOne(accountingConfigurationDto.getIntangibleImmobilizationAccount()));
        configurationChartAccounts.put(accountingConfigurationDto.getIntangibleAmortizationAccount(),
                chartaccountService.findOne(accountingConfigurationDto.getIntangibleAmortizationAccount()));
        configurationChartAccounts.put(accountingConfigurationDto.getDotationAmortizationAccount(),
                chartaccountService.findOne(accountingConfigurationDto.getDotationAmortizationAccount()));
    }
}
