package fr.sparkit.accounting.services.impl;

import static fr.sparkit.accounting.constants.AccountingConstants.ENTITY_NAME_BILL_CODE;
import static fr.sparkit.accounting.constants.AccountingConstants.ERROR_CREATING_FILE;
import static fr.sparkit.accounting.constants.AccountingConstants.EXPORT_FILE_NAME;
import static fr.sparkit.accounting.constants.AccountingConstants.IMPORT_MODEL_FILE_NAME;
import static fr.sparkit.accounting.constants.AccountingConstants.LOG_ENTITY_CREATED;
import static fr.sparkit.accounting.constants.AccountingConstants.LOG_ENTITY_DELETED;
import static fr.sparkit.accounting.constants.AccountingConstants.NO_ACCOUNT_PREFIXED_BY_THIS_CODE;
import static fr.sparkit.accounting.constants.AccountingConstants.PLAN_ACCOUNT_NOT_EXIST;
import static fr.sparkit.accounting.constants.AccountingConstants.PREFIX_ERROR;
import static fr.sparkit.accounting.constants.AccountingConstants.SIMULATION_EXPORT_FILE_NAME;
import static fr.sparkit.accounting.constants.AccountingConstants.THOUSANDS_SEPARATOR;
import static fr.sparkit.accounting.constants.AccountingConstants.TIER_ACCOUNT_IS_NON_EXISTENT;
import static fr.sparkit.accounting.constants.AccountingConstants.TRYING_TO_SAVE_ACCOUNT_WITH_ALREADY_USED_ACCOUNT;
import static fr.sparkit.accounting.constants.AccountingConstants.TRYING_TO_SAVE_ACC_WITH_ACCOUNT_CODE_ALREADY_USED;
import static fr.sparkit.accounting.constants.AccountingConstants.TRYING_TO_SORT_USING_NO_EXIST_FILED;
import static fr.sparkit.accounting.services.utils.excel.ExcelCellStyleHelper.setInvalidCell;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import fr.sparkit.accounting.auditing.AccountExcelCell;
import fr.sparkit.accounting.constants.AccountingConstants;
import fr.sparkit.accounting.constants.LanguageConstants;
import fr.sparkit.accounting.constants.NumberConstant;
import fr.sparkit.accounting.constants.XLSXErrors;
import fr.sparkit.accounting.convertor.AccountConvertor;
import fr.sparkit.accounting.dao.AccountCustomerDao;
import fr.sparkit.accounting.dao.AccountDao;
import fr.sparkit.accounting.dao.AccountSupplierDao;
import fr.sparkit.accounting.dao.ChartAccountsDao;
import fr.sparkit.accounting.dto.AccountDto;
import fr.sparkit.accounting.dto.AccountingConfigurationDto;
import fr.sparkit.accounting.dto.ChartAccountsDto;
import fr.sparkit.accounting.dto.FileUploadDto;
import fr.sparkit.accounting.dto.Filter;
import fr.sparkit.accounting.dto.excel.AccountXLSXFormatDto;
import fr.sparkit.accounting.entities.Account;
import fr.sparkit.accounting.entities.ChartAccounts;
import fr.sparkit.accounting.entities.account.relations.GenericAccountRelation;
import fr.sparkit.accounting.services.IAccountService;
import fr.sparkit.accounting.services.IAccountingConfigurationService;
import fr.sparkit.accounting.services.IChartAccountsService;
import fr.sparkit.accounting.services.IDocumentAccountLineService;
import fr.sparkit.accounting.services.utils.AccountingServiceUtil;
import fr.sparkit.accounting.services.utils.ImportDocumentUtil;
import fr.sparkit.accounting.services.utils.excel.ExcelCellStyleHelper;
import fr.sparkit.accounting.services.utils.excel.GenericExcelPOIHelper;
import fr.sparkit.accounting.util.errors.ApiErrors;
import fr.sparkit.accounting.util.errors.ErrorsResponse;
import fr.sparkit.accounting.util.http.HttpCustomException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AccountService extends GenericService<Account, Long> implements IAccountService {

    public static final int MAX_ACCOUNT_CODE_LENGTH = 8;
    public static final int ZERO = 0;
    public static final String MIN = "MIN";
    public static final char ZERO_CHAR = '0';
    private static final String PLAN_CODE_AND_ACCOUNT_CODE_FORMAT = "%d_%d";
    private static final DataFormatter dataFormatter = new DataFormatter();
    @Value("${accounting.excel.storage-directory}")
    private Path excelStoragePath;

    private final List<Field> excelHeaderFields;
    private List<String> acceptedHeaders;
    private final IChartAccountsService chartAccountsService;
    private final IAccountingConfigurationService accountingConfigurationService;
    private final AccountDao accountDao;
    private final ChartAccountsDao chartAccountsDao;
    private final AccountSupplierDao accountSupplierDao;
    private final AccountCustomerDao accountCustomerDao;
    private final IDocumentAccountLineService documentAccountLineService;

    @Autowired
    public AccountService(IChartAccountsService chartAccountsService,
            @Lazy IAccountingConfigurationService accountingConfigurationService, AccountDao accountDao,
            ChartAccountsDao chartAccountsDao, AccountSupplierDao accountSupplierDao,
            AccountCustomerDao accountCustomerDao, @Lazy IDocumentAccountLineService documentAccountLineService) {
        this.accountingConfigurationService = accountingConfigurationService;
        this.accountDao = accountDao;
        this.chartAccountsService = chartAccountsService;
        this.chartAccountsDao = chartAccountsDao;
        this.accountSupplierDao = accountSupplierDao;
        this.accountCustomerDao = accountCustomerDao;
        this.documentAccountLineService = documentAccountLineService;
        excelHeaderFields = AccountXLSXFormatDto.getAccountExcelHeaderFields();
        acceptedHeaders = excelHeaderFields.stream()
                .map(field -> field.getAnnotation(AccountExcelCell.class).headerName()).collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "AccountCache", key = "'AccountCache_'"
            + "+T(fr.sparkit.accounting.util.CompanyContextHolder).getCompanyContext()", unless = "#result==null")
    public List<Account> findAll() {
        return accountDao.findAllByIsDeletedFalseOrderByCode();
    }

    @Override
    @Cacheable(value = "AccountCache", key = "'AccountCache_'"
            + "+T(fr.sparkit.accounting.util.CompanyContextHolder).getCompanyContext()+'_'"
            + "+T(java.util.Arrays).toString(#root.args)", unless = "#result==null")
    public AccountDto findById(Long id) {
        return AccountConvertor.modelToDto(Optional.ofNullable(accountDao.findOne(id)).orElseThrow(
                () -> new HttpCustomException(ApiErrors.Accounting.ENTITY_NOT_FOUND, new ErrorsResponse().error(id))));
    }

    @Override
    @Caching(evict = { @CacheEvict(value = "AccountCache", allEntries = true),
            @CacheEvict(value = "GeneralLedgerAccounts", allEntries = true),
            @CacheEvict(value = "GeneralLedgerAccountDetails", allEntries = true),
            @CacheEvict(value = "TrialBalanceAccounts", allEntries = true) })
    public AccountDto updateAccount(AccountDto accountDto) {
        ChartAccounts chartAccount = chartAccountsService.findOne(accountDto.getPlanId());
        checkIsChartAccountValid(chartAccount, accountDto.getPlanId());
        checkValidCodePrefix(accountDto.getPlanCode(), accountDto.getCode());
        Account account = accountDao.findOne(accountDto.getId());
        boolean checkCodeAccount = account.getCode() != accountDto.getCode();
        if (checkCodeAccount && checkUsableAccount(account.getId())) {
            log.error("Account with code {} is already used, can't change it's code", account.getCode());
            throw new HttpCustomException(ApiErrors.Accounting.ACCOUNT_IS_USED);
        }
        if (!(checkCodeAccount
                && accountDao.existsByCodeAndPlanIdAndIsDeletedFalse(accountDto.getCode(), accountDto.getPlanId()))) {
            checkIsCodeConstraintValid(accountDto);

            Account updatedAccount = accountDao.saveAndFlush(AccountConvertor.dtoToModel(accountDto, chartAccount));
            accountDto.setId(updatedAccount.getId());
            log.info("Account updated successfully {}", updatedAccount);
            return accountDto;
        } else {
            log.error(TRYING_TO_SAVE_ACCOUNT_WITH_ALREADY_USED_ACCOUNT, accountDto.getCode());
            throw new HttpCustomException(ApiErrors.Accounting.ACCOUNT_CODE_EXISTS,
                    new ErrorsResponse().error(accountDto.getCode()));
        }
    }

    @Override
    @Caching(evict = { @CacheEvict(value = "AccountCache", allEntries = true),
            @CacheEvict(value = "GeneralLedgerAccounts", allEntries = true),
            @CacheEvict(value = "GeneralLedgerAccountDetails", allEntries = true),
            @CacheEvict(value = "TrialBalanceAccounts", allEntries = true) })
    public AccountDto saveAccount(AccountDto accountDto) {
        checkValidCodePrefix(accountDto.getPlanCode(), accountDto.getCode());
        ChartAccounts chartAccount = chartAccountsService.findOne(accountDto.getPlanId());
        checkIsChartAccountValid(chartAccount, accountDto.getPlanId());
        boolean codeExists = accountDao.existsByCodeAndIsDeletedFalse(accountDto.getCode());
        if (!codeExists) {
            checkIsCodeConstraintValid(accountDto);
            Account savedAccount = saveAndFlush(AccountConvertor.dtoToModel(accountDto, chartAccount));
            accountDto.setId(savedAccount.getId());
            log.info(LOG_ENTITY_CREATED, savedAccount);
            return accountDto;
        } else {
            log.error(TRYING_TO_SAVE_ACC_WITH_ACCOUNT_CODE_ALREADY_USED, accountDto.getCode());
            throw new HttpCustomException(ApiErrors.Accounting.ACCOUNT_CODE_EXISTS,
                    new ErrorsResponse().error(accountDto.getCode()));
        }
    }

    @Override
    @Caching(evict = { @CacheEvict(value = "AccountCache", allEntries = true),
            @CacheEvict(value = "GeneralLedgerAccounts", allEntries = true),
            @CacheEvict(value = "GeneralLedgerAccountDetails", allEntries = true),
            @CacheEvict(value = "TrialBalanceAccounts", allEntries = true) })
    public boolean isDeleteAccount(Long id) {
        log.info(LOG_ENTITY_DELETED, "Account", id);
        AccountDto accountToDelete = findById(id);
        return isDynamicSoftDelete(id, Account.class.getName(), String.valueOf(accountToDelete.getCode()),
                "MESSAGE_ACCOUNT_TO_DELETE");
    }

    @Override
    public int generateAccountCode(Long planId) {
        Integer lastAccountWithPlan = null;

        int lastCode = chartAccountsDao.findOne(planId).getCode();
        StringBuilder code = new StringBuilder().append(lastCode);

        for (int i = code.length(); i < MAX_ACCOUNT_CODE_LENGTH; i++) {
            code.append(ZERO_CHAR);
        }
        lastCode = Integer.parseInt(code.toString());

        List<Integer> listPlan = Optional
                .ofNullable(accountDao.findCodeAccountByPlanIdAndIsDeletedFalseOrderByCode(planId)).orElse(null);
        while (lastAccountWithPlan == null) {
            if (listPlan != null && listPlan.contains(lastCode)) {
                lastCode++;
            } else {
                lastAccountWithPlan = lastCode;
            }
        }
        return lastAccountWithPlan;

    }

    void checkValidCodePrefix(int parentCode, int childCode) {
        if (!String.valueOf(childCode).startsWith(String.valueOf(parentCode))) {
            log.error(PREFIX_ERROR);
            throw new HttpCustomException(ApiErrors.Accounting.ACCOUNT_CODE_DIFFERENT_THAN_PARENT);
        }
    }

    public void checkIsChartAccountValid(ChartAccounts chartAccount, Long planIdDto) {
        if (chartAccount == null) {
            log.error(PLAN_ACCOUNT_NOT_EXIST);
            throw new HttpCustomException(ApiErrors.Accounting.CHART_ACCOUNT_PARENT_CHART_ACCOUNT_DONT_EXIST,
                    new ErrorsResponse().error(planIdDto));
        }
    }

    public void checkIsCodeConstraintValid(AccountDto accountDto) {
        if (!String.valueOf(accountDto.getCode()).startsWith(String.valueOf(accountDto.getPlanCode()))) {
            log.error(PREFIX_ERROR);
            throw new HttpCustomException(ApiErrors.Accounting.ACCOUNT_CODE_DIFFERENT_THAN_PARENT);
        }
    }

    public String getDefaultSortFieldForAccount() {
        String defaultSortFieldForAccount = ENTITY_NAME_BILL_CODE;
        if (AccountingServiceUtil.fieldExistsInEntity(defaultSortFieldForAccount, Account.class)) {
            return defaultSortFieldForAccount;
        } else {
            log.error(TRYING_TO_SORT_USING_NO_EXIST_FILED);
            throw new HttpCustomException(ApiErrors.Accounting.TRYING_TO_SORT_USING_NON_EXISTENT_FIELD);
        }
    }

    @Override
    public List<Account> findByPlanCode(Integer planCode) {
        if (planCode != null) {
            return accountDao.findByPlanCode(Integer.toString(planCode));
        } else {
            return findAll();
        }
    }

    @Override
    public Account findTierAccount(Long tierId, String tierName, boolean isSupplier) {
        Optional<GenericAccountRelation> tierAccountRelation;
        if (isSupplier) {
            tierAccountRelation = accountSupplierDao.findByRelationEntityIdAndIsDeletedFalse(tierId);
        } else {
            tierAccountRelation = accountCustomerDao.findByRelationEntityIdAndIsDeletedFalse(tierId);
        }
        if (tierAccountRelation.isPresent()) {
            return tierAccountRelation.get().getAccount();
        } else {
            Optional<Account> accountTaxOpt;
            if (isSupplier) {
                accountTaxOpt = findAccountByCode(Integer.parseInt(ImportDocumentUtil
                        .generateAccountCode(accountingConfigurationService.findLastConfig().getSupplierAccountCode())
                        .toString()));
            } else {
                accountTaxOpt = findAccountByCode(Integer.parseInt(ImportDocumentUtil
                        .generateAccountCode(accountingConfigurationService.findLastConfig().getCustomerAccountCode())
                        .toString()));
            }
            if (accountTaxOpt.isPresent()) {
                return accountTaxOpt.get();
            } else {
                log.error(TIER_ACCOUNT_IS_NON_EXISTENT, tierName);
                throw new HttpCustomException(ApiErrors.Accounting.NULL_TIER_ACCOUNT_ID);
            }
        }
    }

    @Override
    public AccountDto findByCode(int code) {
        return accountDao.findByCodeAndIsDeletedFalse(code).map(AccountConvertor::modelToDto)
                .orElseGet(AccountDto::new);
    }

    @Override
    public Optional<Account> findAccountByCode(int code) {
        return accountDao.findByCodeAndIsDeletedFalse(code);
    }

    @Override
    public boolean isAccountCodeUsedInMultipleAccounts(int code) {
        return accountDao.findAccountsByCode(code).size() > 1;
    }

    @Override
    public AccountDto findAccount(Integer code, String extremum) {
        int accountCode;
        if (extremum.equals(MIN)) {
            accountCode = accountDao.findMinCode(code.toString()).orElse(ZERO);
        } else {
            accountCode = accountDao.findMaxCode(code.toString()).orElse(ZERO);
        }
        if (accountCode != ZERO) {
            return findByCode(accountCode);
        } else {
            log.error(NO_ACCOUNT_PREFIXED_BY_THIS_CODE);
            throw new HttpCustomException(ApiErrors.Accounting.NO_ACCOUNT_PREFIXED_BY_CODE);
        }
    }

    @Override
    public List<Account> findReconcilableAccounts() {
        return accountDao.findAllByIsDeletedFalseAndReconcilableTrueOrderByCode();
    }

    @Override
    public boolean existsById(Long accountId) {
        return accountDao.existsByIdAndIsDeletedFalse(accountId);
    }

    @Override
    public boolean checkUsableAccount(Long idAccount) {
        return !isRelated(idAccount, Account.class.getName());
    }

    @Override
    public Map<String, List<AccountDto>> getAmortizationAndImmobilizationAccounts() {
        AccountingConfigurationDto accountingConfiguration = accountingConfigurationService.findLastConfig();
        ChartAccounts tangibleImmobilizationChartAccount = chartAccountsDao
                .findById(accountingConfiguration.getTangibleImmobilizationAccount())
                .orElseThrow(() -> new HttpCustomException(ApiErrors.Accounting.CHART_ACCOUNT_INEXISTANT));
        ChartAccounts tangibleAmortizationChartAccount = chartAccountsDao
                .findById(accountingConfiguration.getTangibleAmortizationAccount())
                .orElseThrow(() -> new HttpCustomException(ApiErrors.Accounting.CHART_ACCOUNT_INEXISTANT));
        ChartAccounts intangibleImmobilizationChartAccount = chartAccountsDao
                .findById(accountingConfiguration.getIntangibleImmobilizationAccount())
                .orElseThrow(() -> new HttpCustomException(ApiErrors.Accounting.CHART_ACCOUNT_INEXISTANT));
        ChartAccounts intangibleAmortizationChartAccount = chartAccountsDao
                .findById(accountingConfiguration.getIntangibleAmortizationAccount())
                .orElseThrow(() -> new HttpCustomException(ApiErrors.Accounting.CHART_ACCOUNT_INEXISTANT));
        ChartAccounts dotationAmortizationChartAccount = chartAccountsDao
                .findById(accountingConfiguration.getDotationAmortizationAccount())
                .orElseThrow(() -> new HttpCustomException(ApiErrors.Accounting.CHART_ACCOUNT_INEXISTANT));
        Map<String, List<AccountDto>> accountMap = new HashMap<>();
        accountMap.put(AccountingConstants.TANGIBLE_IMMOBILIZATION_ACCOUNTS,
                AccountConvertor.modelsToDtos(findByPlanCode(tangibleImmobilizationChartAccount.getCode())));
        accountMap.put(AccountingConstants.TANGIBLE_AMORTIZATION_ACCOUNTS,
                AccountConvertor.modelsToDtos(findByPlanCode(tangibleAmortizationChartAccount.getCode())));
        accountMap.put(AccountingConstants.INTANGIBLE_IMMOBILIZATION_ACCOUNTS,
                AccountConvertor.modelsToDtos(findByPlanCode(intangibleImmobilizationChartAccount.getCode())));
        accountMap.put(AccountingConstants.INTANGIBLE_AMORTIZATION_ACCOUNTS,
                AccountConvertor.modelsToDtos(findByPlanCode(intangibleAmortizationChartAccount.getCode())));
        accountMap.put(AccountingConstants.DOTATION_AMORTIZATION_ACCOUNTS,
                AccountConvertor.modelsToDtos(findByPlanCode(dotationAmortizationChartAccount.getCode())));
        return accountMap;
    }

    @Override
    public List<AccountDto> getResultAccounts() {
        AccountingConfigurationDto currentConfiguration = accountingConfigurationService.findLastConfig();
        ChartAccountsDto resultAccountsChart = chartAccountsService.findById(currentConfiguration.getResultAccount());
        return AccountConvertor.modelsToDtos(findByPlanCode(resultAccountsChart.getCode()));
    }

    @Override
    public Page<AccountDto> filterAccount(List<Filter> filters, Pageable pageable) {

        if (!pageable.getSort().get().findFirst().isPresent()) {
            pageable = AccountingServiceUtil.getPageable(pageable.getPageNumber(), pageable.getPageSize(),
                    getDefaultSortFieldForAccount(), Sort.Direction.ASC.toString());
        }

        Page<Account> page = FilterService.getPageOfFilterableEntity(Account.class, accountDao, filters, pageable);

        return new PageImpl<>(AccountConvertor.modelsToDtos(page.getContent()), pageable, page.getTotalElements());

    }

    @Override
    public Map<String, List<AccountDto>> findTaxAccounts() {
        AccountingConfigurationDto accountingConfiguration = accountingConfigurationService.findLastConfig();
        ChartAccounts taxPurchaseChartAccount = chartAccountsDao
                .findById(accountingConfiguration.getTaxPurchasesAccount())
                .orElseThrow(() -> new HttpCustomException(ApiErrors.Accounting.CHART_ACCOUNT_INEXISTANT));
        ChartAccounts taxSalesCartAccount = chartAccountsDao.findById(accountingConfiguration.getTaxSalesAccount())
                .orElseThrow(() -> new HttpCustomException(ApiErrors.Accounting.CHART_ACCOUNT_INEXISTANT));
        ChartAccounts hTaxPurchasesChartAccount = chartAccountsDao
                .findById(accountingConfiguration.getHtaxPurchasesAccount())
                .orElseThrow(() -> new HttpCustomException(ApiErrors.Accounting.CHART_ACCOUNT_INEXISTANT));
        ChartAccounts hTaxSalesChartAccount = chartAccountsDao.findById(accountingConfiguration.getHtaxSalesAccount())
                .orElseThrow(() -> new HttpCustomException(ApiErrors.Accounting.CHART_ACCOUNT_INEXISTANT));
        ChartAccounts fodecSalesChartAccount = chartAccountsDao.findById(accountingConfiguration.getFodecSalesAccount())
                .orElseThrow(() -> new HttpCustomException(ApiErrors.Accounting.CHART_ACCOUNT_INEXISTANT));
        ChartAccounts fodecPurchasesChartAccount = chartAccountsDao
                .findById(accountingConfiguration.getFodecPurchasesAccount())
                .orElseThrow(() -> new HttpCustomException(ApiErrors.Accounting.CHART_ACCOUNT_INEXISTANT));
        Map<String, List<AccountDto>> accountsMap = new HashMap<>();
        accountsMap.put(AccountingConstants.TAX_PURCHASES_ACCOUNTS,
                AccountConvertor.modelsToDtos(findByPlanCode(taxPurchaseChartAccount.getCode())));
        accountsMap.put(AccountingConstants.TAX_SALES_ACCOUNTS,
                AccountConvertor.modelsToDtos(findByPlanCode(taxSalesCartAccount.getCode())));
        accountsMap.put(AccountingConstants.HTAX_PURCHASES_ACCOUNTS,
                AccountConvertor.modelsToDtos(findByPlanCode(hTaxPurchasesChartAccount.getCode())));
        accountsMap.put(AccountingConstants.HTAX_SALES_ACCOUNTS,
                AccountConvertor.modelsToDtos(findByPlanCode(hTaxSalesChartAccount.getCode())));
        accountsMap.put(AccountingConstants.FODEC_SALES_ACCOUNTS,
                AccountConvertor.modelsToDtos(findByPlanCode(fodecSalesChartAccount.getCode())));
        accountsMap.put(AccountingConstants.FODEC_PURCHASES_ACCOUNTS,
                AccountConvertor.modelsToDtos(findByPlanCode(fodecPurchasesChartAccount.getCode())));
        return accountsMap;
    }

    @Override
    public Map<String, List<AccountDto>> findTiersAccounts() {
        AccountingConfigurationDto accountingConfiguration = accountingConfigurationService.findLastConfig();
        ChartAccounts supplierChartAccount = chartAccountsDao.findById(accountingConfiguration.getSupplierAccount())
                .orElseThrow(() -> new HttpCustomException(ApiErrors.Accounting.CHART_ACCOUNT_INEXISTANT));
        ChartAccounts customerCartAccount = chartAccountsDao.findById(accountingConfiguration.getCustomerAccount())
                .orElseThrow(() -> new HttpCustomException(ApiErrors.Accounting.CHART_ACCOUNT_INEXISTANT));
        Map<String, List<AccountDto>> accountsMap = new HashMap<>();
        accountsMap.put(AccountingConstants.SUPPLIER_ACCOUNTS,
                AccountConvertor.modelsToDtos(findByPlanCode(supplierChartAccount.getCode())));
        accountsMap.put(AccountingConstants.CUSTOMER_ACCOUNTS,
                AccountConvertor.modelsToDtos(findByPlanCode(customerCartAccount.getCode())));
        return accountsMap;
    }

    @Override
    public List<AccountDto> findBankAccounts() {
        AccountingConfigurationDto accountingConfiguration = accountingConfigurationService.findLastConfig();
        ChartAccounts chartAccount = chartAccountsDao.findById(accountingConfiguration.getBankIdAccountingAccount())
                .orElseThrow(() -> new HttpCustomException(ApiErrors.Accounting.CHART_ACCOUNT_INEXISTANT));
        return AccountConvertor.modelsToDtos(findByPlanCode(chartAccount.getCode()));

    }

    @Override
    public List<AccountDto> findCofferAccounts() {
        AccountingConfigurationDto accountingConfiguration = accountingConfigurationService.findLastConfig();
        ChartAccounts chartAccount = chartAccountsDao.findById(accountingConfiguration.getCofferIdAccountingAccount())
                .orElseThrow(() -> new HttpCustomException(ApiErrors.Accounting.CHART_ACCOUNT_INEXISTANT));
        return AccountConvertor.modelsToDtos(findByPlanCode(chartAccount.getCode()));
    }

    @Override
    public List<AccountDto> findWithHoldingTaxAccounts() {
        AccountingConfigurationDto accountingConfiguration = accountingConfigurationService.findLastConfig();
        ChartAccounts chartAccount = chartAccountsDao
                .findById(accountingConfiguration.getWithHoldingTaxIdAccountingAccount())
                .orElseThrow(() -> new HttpCustomException(ApiErrors.Accounting.CHART_ACCOUNT_INEXISTANT));
        return AccountConvertor.modelsToDtos(findByPlanCode(chartAccount.getCode()));
    }

    @Override
    public byte[] exportAccountsExcelModel() {
        File file = GenericExcelPOIHelper.generateXLSXFileFromData(new ArrayList<>(),
                String.format(IMPORT_MODEL_FILE_NAME, LanguageConstants.ACCOUNT_SHEET_NAME), excelStoragePath.toFile(),
                acceptedHeaders, excelHeaderFields, LanguageConstants.ACCOUNT_SHEET_NAME);
        return GenericExcelPOIHelper.convertFileToByteArray(file);
    }

    @Override
    @Caching(evict = { @CacheEvict(value = "AccountCache", allEntries = true),
            @CacheEvict(value = "GeneralLedgerAccounts", allEntries = true),
            @CacheEvict(value = "GeneralLedgerAccountDetails", allEntries = true),
            @CacheEvict(value = "TrialBalanceAccounts", allEntries = true) })
    public FileUploadDto loadAccountsExcelData(FileUploadDto fileUploadDto) {
        List<AccountDto> accounts = new ArrayList<>();
        boolean allSheetsAreEmpty;
        boolean accountsAreValid = true;
        ExcelCellStyleHelper.resetStyles();
        try (Workbook workbook = GenericExcelPOIHelper
                .createWorkBookFromBase64String(fileUploadDto.getBase64Content())) {
            allSheetsAreEmpty = true;
            if (workbook.getNumberOfSheets() == 0) {
                throw new HttpCustomException(ApiErrors.Accounting.EXCEL_EMPTY_FILE);
            }
            GenericExcelPOIHelper.validateWorkbookSheetsHeaders(workbook, acceptedHeaders);
            List<String> previousAccountCodes = new ArrayList<>();
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                log.info("Parsing sheet #{}", sheetIndex + 1);
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                boolean isSheetEmpty = GenericExcelPOIHelper.isSheetEmpty(sheet);
                allSheetsAreEmpty &= isSheetEmpty;
                if (isSheetEmpty) {
                    continue;
                }
                accountsAreValid &= isAccountValuesAddedToSheet(accounts, accountsAreValid, sheet,
                        previousAccountCodes);
            }
            if (allSheetsAreEmpty) {
                log.error("Trying to import empty document");
                throw new HttpCustomException(ApiErrors.Accounting.EXCEL_EMPTY_FILE);
            } else if (accountsAreValid) {
                log.info("Saving accounts");
                saveAccountsComingFromExcel(accounts);
                return new FileUploadDto();
            } else {
                return GenericExcelPOIHelper.getFileUploadDtoFromWorkbook(workbook, excelStoragePath.toFile(),
                        String.format(SIMULATION_EXPORT_FILE_NAME, LanguageConstants.ACCOUNT_SHEET_NAME));
            }
        } catch (IOException e) {
            log.error(ERROR_CREATING_FILE, e);
            throw new HttpCustomException(ApiErrors.Accounting.EXCEL_FILE_CREATION_FAIL);
        }
    }

    public void saveAccountsComingFromExcel(Collection<AccountDto> accounts) {
        if (accounts.isEmpty()) {
            throw new HttpCustomException(ApiErrors.Accounting.EXCEL_NO_ACCOUNTS_TO_BE_SAVED);
        }
        for (AccountDto account : accounts) {
            saveAccount(account);
        }
    }

    public boolean isAccountValuesAddedToSheet(Collection<AccountDto> accounts, boolean accountsAreValid, Sheet sheet,
            List<String> previousAccountCodes) {
        AccountDto account;
        for (int rowIndex = sheet.getFirstRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            log.info("Parsing row #{} in sheet {}", rowIndex, sheet.getSheetName());
            Row row = sheet.getRow(rowIndex);
            if (GenericExcelPOIHelper.isRowNotEmpty(row)) {
                GenericExcelPOIHelper.validateNumberOfCellsInRowAgainstHeaders(row, acceptedHeaders.size());
                account = new AccountDto();
                accounts.add(account);
                accountsAreValid &= accountValuesAddedToRow(account, row, excelHeaderFields, previousAccountCodes);
            }
        }
        return accountsAreValid;
    }

    public boolean accountValuesAddedToRow(AccountDto account, Row row, List<Field> excelHeaderFields,
            List<String> previousAccountCodes) {
        boolean isValid = true;
        for (int i = 0; i < excelHeaderFields.size(); i++) {
            Cell cell = row.getCell(i);
            if (cell == null) {
                cell = row.createCell(i);
            }
            cell.setCellComment(null);
            switch (excelHeaderFields.get(i).getAnnotation(AccountExcelCell.class).headerName()) {
            case LanguageConstants.XLSXHeaders.PARENT_PLAN_CODE_HEADER_NAME:
                isValid &= isParentPlanCodeSet(cell, account);
                break;
            case LanguageConstants.XLSXHeaders.CODE_HEADER_NAME:
                isValid &= isAccountCodeSet(cell, account, previousAccountCodes);
                break;
            case LanguageConstants.XLSXHeaders.LABEL_HEADER_NAME:
                isValid &= isAccountLabelSet(cell, account);
                break;
            case LanguageConstants.XLSXHeaders.RECONCILABLE_HEADER_NAME:
                isValid &= isAccountReconcilableSet(cell, account);
                break;
            case LanguageConstants.XLSXHeaders.LITERABLE_HEADER_NAME:
                isValid &= isAccountLiterableSet(cell, account);
                break;
            case LanguageConstants.XLSXHeaders.ACCOUNT_OPENING_CREDIT_HEADER_NAME:
                isValid &= isAccountOpeningDebitSet(cell, account);
                break;
            case LanguageConstants.XLSXHeaders.ACCOUNT_OPENING_DEBIT_HEADER_NAME:
                isValid &= isAccountOpeningCreditSet(cell, account);
                break;
            default:
                isValid = false;
            }
        }
        return isValid;
    }

    public boolean isAccountOpeningCreditSet(Cell cell, AccountDto account) {
        if (documentAccountLineService.isMonetaryValueInCellValid(cell)) {
            BigDecimal creditOpening = BigDecimal.valueOf(Double.parseDouble(dataFormatter.formatCellValue(cell).trim()
                    .replace(String.valueOf(THOUSANDS_SEPARATOR), StringUtils.EMPTY)));
            account.setCreditOpening(creditOpening);
            return true;
        }
        return false;
    }

    public boolean isAccountOpeningDebitSet(Cell cell, AccountDto account) {
        if (documentAccountLineService.isMonetaryValueInCellValid(cell)) {
            BigDecimal debitOpening = BigDecimal.valueOf(Double.parseDouble(dataFormatter.formatCellValue(cell).trim()
                    .replace(String.valueOf(THOUSANDS_SEPARATOR), StringUtils.EMPTY)));
            account.setDebitOpening(debitOpening);
            return true;
        }
        return false;
    }

    public boolean isAccountLiterableSet(Cell cell, AccountDto account) {
        String cellValue = dataFormatter.formatCellValue(cell).trim();
        CellType cellType = cell.getCellType();
        if (CellType.BOOLEAN.equals(cellType)) {
            account.setLiterable(cell.getBooleanCellValue());
            return true;
        } else if (CellType.STRING.equals(cellType) && (LanguageConstants.TRUE.equalsIgnoreCase(cellValue)
                || LanguageConstants.FALSE.equalsIgnoreCase(cellValue))) {
            account.setLiterable(LanguageConstants.TRUE.equals(cellValue));
            return true;
        } else {
            ExcelCellStyleHelper.setInvalidCell(cell, String.format(XLSXErrors.RECONCILABLE_VALUE_NOT_ALLOWED,
                    LanguageConstants.TRUE, LanguageConstants.FALSE));
        }
        return false;
    }

    public boolean isAccountReconcilableSet(Cell cell, AccountDto account) {
        String cellValue = dataFormatter.formatCellValue(cell).trim();
        CellType cellType = cell.getCellType();
        if (CellType.BOOLEAN.equals(cellType)) {
            account.setReconcilable(cell.getBooleanCellValue());
            return true;
        } else if (CellType.STRING.equals(cellType) && (LanguageConstants.TRUE.equalsIgnoreCase(cellValue)
                || LanguageConstants.FALSE.equalsIgnoreCase(cellValue))) {
            account.setReconcilable(LanguageConstants.TRUE.equals(cellValue));
            return true;
        } else {
            ExcelCellStyleHelper.setInvalidCell(cell, String.format(XLSXErrors.RECONCILABLE_VALUE_NOT_ALLOWED,
                    LanguageConstants.TRUE, LanguageConstants.FALSE));
        }
        return false;
    }

    public boolean isAccountLabelSet(Cell cell, AccountDto account) {
        if (GenericExcelPOIHelper.isLabelInCellValid(cell)) {
            account.setLabel(dataFormatter.formatCellValue(cell).trim());
            return true;
        }
        return false;
    }

    public boolean isAccountCodeSet(Cell cell, AccountDto account, Collection<String> previousAccountCodes) {
        String cellValue = dataFormatter.formatCellValue(cell).trim();
        if (isAccountCodeInCellValid(cell, previousAccountCodes)) {
            int accountCode = Integer.parseInt(cellValue);
            if (!previousAccountCodes
                    .contains(String.format(PLAN_CODE_AND_ACCOUNT_CODE_FORMAT, account.getPlanCode(), accountCode))
                    && !accountDao.existsByCodeAndPlanIdAndIsDeletedFalse(account.getCode(), account.getPlanId())) {
                if (cellValue.startsWith(String.valueOf(account.getPlanCode()))) {
                    account.setCode(accountCode);
                    previousAccountCodes
                            .add(String.format(PLAN_CODE_AND_ACCOUNT_CODE_FORMAT, account.getPlanCode(), accountCode));
                    return true;
                } else {
                    ExcelCellStyleHelper.setInvalidCell(cell,
                            XLSXErrors.AccountXLSXErrors.ACCOUNT_CODE_SHOULD_BE_IMMEDIATE_CHILD_TO_PARENT_CODE);
                }
            } else {
                ExcelCellStyleHelper.setInvalidCell(cell, XLSXErrors.AccountXLSXErrors.ACCOUNT_WITH_CODE_EXISTS);
            }
        }
        return false;
    }

    public boolean isParentPlanCodeSet(Cell cell, AccountDto account) {
        String planCodeValue = dataFormatter.formatCellValue(cell).trim();
        if (planCodeValue.isEmpty()) {
            setInvalidCell(cell, XLSXErrors.REQUIRED_FIELD);
        } else {
            Integer planCode = Integer.parseInt(planCodeValue);
            account.setPlanCode(planCode);
            ChartAccountsDto chartAccountsDto = chartAccountsService.findByCode(planCode);
            if (chartAccountsDto.getId() == null) {
                setInvalidCell(cell,
                        String.format(XLSXErrors.ChartAccountXLSXErrors.NO_CHART_ACCOUNT_WITH_CODE, planCode));
            } else {
                account.setPlanId(chartAccountsDto.getId());
                return true;
            }
        }
        return false;
    }

    @Override
    public byte[] exportAccountsAsExcelFile() {
        List<Account> accounts = findAll();
        accounts.sort(Comparator.comparing(Account::getCode));
        List<Object> accountsXLSXFormatDtoList = new ArrayList<>();
        for (Account account : accounts) {
            AccountXLSXFormatDto accountXLSXFormatDto = new AccountXLSXFormatDto();
            accountXLSXFormatDto.setAccountCode(account.getCode());
            accountXLSXFormatDto.setLabel(account.getLabel());
            accountXLSXFormatDto.setAccountOpeningCreditAmount(account.getCreditOpening());
            accountXLSXFormatDto.setAccountOpeningDebitAmount(account.getDebitOpening());
            accountXLSXFormatDto.setLabel(account.getLabel());
            accountXLSXFormatDto.setParentPlanCode(account.getPlan().getCode());
            if (account.isReconcilable()) {
                accountXLSXFormatDto.setReconcilable(LanguageConstants.TRUE);
            } else {
                accountXLSXFormatDto.setReconcilable(LanguageConstants.FALSE);
            }
            if (account.isLiterable()) {
                accountXLSXFormatDto.setLiterable(LanguageConstants.TRUE);
            } else {
                accountXLSXFormatDto.setLiterable(LanguageConstants.FALSE);
            }
            accountsXLSXFormatDtoList.add(accountXLSXFormatDto);
        }
        File file = GenericExcelPOIHelper.generateXLSXFileFromData(accountsXLSXFormatDtoList,
                String.format(EXPORT_FILE_NAME, LanguageConstants.ACCOUNT_SHEET_NAME), excelStoragePath.toFile(),
                acceptedHeaders, excelHeaderFields, LanguageConstants.ACCOUNT_SHEET_NAME);
        return GenericExcelPOIHelper.convertFileToByteArray(file);
    }

    public boolean isAccountCodeInCellValid(Cell cell, Collection<String> previousAccountCodes) {
        String cellValue = dataFormatter.formatCellValue(cell).trim();
        if (!cellValue.isEmpty()) {
            try {
                int accountCode = Integer.parseInt(cellValue);
                if (accountCode < 0 || cellValue.length() != MAX_ACCOUNT_CODE_LENGTH) {
                    setInvalidCell(cell, String.format(XLSXErrors.AccountXLSXErrors.ACCOUNT_CODE_INVALID_FORMAT,
                            MAX_ACCOUNT_CODE_LENGTH));
                } else {
                    Row row = cell.getRow();
                    // TODO : Dynamically get chartCell column index
                    Cell chartCell = row.getCell(cell.getColumnIndex() - NumberConstant.ONE);
                    String chartAccountCode = dataFormatter.formatCellValue(chartCell).trim();
                    Account accountWithCode = accountDao.findByCodeAndPlanCode(accountCode, chartAccountCode);
                    if (accountWithCode != null || previousAccountCodes.contains(cellValue)) {
                        ExcelCellStyleHelper.setInvalidCell(cell,
                                XLSXErrors.AccountXLSXErrors.ACCOUNT_WITH_CODE_EXISTS);
                    } else {
                        return true;
                    }
                }
            } catch (NumberFormatException e) {
                setInvalidCell(cell, XLSXErrors.ACCOUNT_ACCOUNT_CODE_CELL_SHOULD_BE_OF_TYPE_NUMBER);
            }
        } else {
            setInvalidCell(cell, XLSXErrors.REQUIRED_FIELD);
        }
        return false;
    }
}
