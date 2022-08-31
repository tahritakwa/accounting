package fr.sparkit.accounting.restcontroller;

import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import fr.sparkit.accounting.auditing.HasRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
import fr.sparkit.accounting.convertor.AccountConvertor;
import fr.sparkit.accounting.dto.AccountDto;
import fr.sparkit.accounting.dto.FileUploadDto;
import fr.sparkit.accounting.dto.Filter;
import fr.sparkit.accounting.dto.GenericAccountRelationDto;
import fr.sparkit.accounting.entities.account.relations.GenericAccountRelation;
import fr.sparkit.accounting.enumuration.AccountRelationType;
import fr.sparkit.accounting.services.IAccountService;
import fr.sparkit.accounting.services.IGenericAccountRelationService;
import fr.sparkit.accounting.services.impl.AccountService;
import fr.sparkit.accounting.util.errors.ApiErrors;
import fr.sparkit.accounting.util.errors.ErrorsResponse;
import fr.sparkit.accounting.util.http.HttpCustomException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@CrossOrigin("*")
@RequestMapping("/api/accounting/account")
public class AccountController {

    private final IAccountService accountService;
    private final IGenericAccountRelationService accountCustomerService;
    private final IGenericAccountRelationService accountSupplierService;
    private final IGenericAccountRelationService accountBankService;
    private final IGenericAccountRelationService accountCofferService;
    private final IGenericAccountRelationService accountWithHoldingTaxService;

    @Autowired
    public AccountController(AccountService accountService,
            @Qualifier("AccountCustomerService") IGenericAccountRelationService accountCustomerService,
            @Qualifier("AccountSupplierService") IGenericAccountRelationService accountSupplierService,
            @Qualifier("AccountBankService") IGenericAccountRelationService accountBankService,
            @Qualifier("AccountCofferService") IGenericAccountRelationService accountCofferService,
            @Qualifier("AccountWithHoldingTaxService") IGenericAccountRelationService accountWithHoldingTaxService) {
        this.accountService = accountService;
        this.accountCustomerService = accountCustomerService;
        this.accountSupplierService = accountSupplierService;
        this.accountBankService = accountBankService;
        this.accountCofferService = accountCofferService;
        this.accountWithHoldingTaxService = accountWithHoldingTaxService;
    }

    @GetMapping(value = "/accounts")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "ACCOUNTING","VIEW_ACCOUNTING_ACCOUNTS" })
    public List<AccountDto> getAllAccounts() {
        return AccountConvertor.modelsToDtos(accountService.findAll());
    }

    @GetMapping(value = "/reconcilable-accounts")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "ACCOUNTING","VIEW_ACCOUNTING_ACCOUNTS" })
    public List<AccountDto> getReconcilableAccounts() {
        return AccountConvertor.modelsToDtos(accountService.findReconcilableAccounts());
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/{id}")
    @HasRoles(permissions = { "ACCOUNTING","VIEW_ACCOUNTING_ACCOUNTS" })
    public AccountDto findById(@PathVariable Long id) {
        return accountService.findById(id);
    }

    @GetMapping(value = "/accounts/plan-code/{planCode}")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "ACCOUNTING","VIEW_ACCOUNTING_ACCOUNTS" })
    public List<AccountDto> findByPlanCode(@PathVariable Integer planCode) {
        return AccountConvertor.modelsToDtos(accountService.findByPlanCode(planCode));
    }

    @GetMapping(value = "/immobilization-amortization-accounts")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "ACCOUNTING","VIEW_ACCOUNTING_ACCOUNTS" })
    public Map<String, List<AccountDto>> getAmortizationAndImmobilizationAccounts() {
        return accountService.getAmortizationAndImmobilizationAccounts();
    }

    @GetMapping(value = "/result-accounts")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "ACCOUNTING","VIEW_ACCOUNTING_ACCOUNTS" })
    public List<AccountDto> getResultAccounts() {
        return accountService.getResultAccounts();
    }

    @PostMapping()
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "ADD_ACCOUNTING_ACCOUNTS" })
    public AccountDto save(@RequestBody @Valid AccountDto accountDto) {
        return accountService.saveAccount(accountDto);
    }

    @PutMapping(value = "/{id}")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "VIEW_ACCOUNTING_ACCOUNTS" })
    public AccountDto update(@RequestBody @Valid AccountDto accountDto) {
        return this.accountService.updateAccount(accountDto);
    }

    @DeleteMapping(value = "/{id}")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "DELETE_ACCOUNTING_ACCOUNTS" })
    public boolean delete(@PathVariable Long id) {
        return accountService.isDeleteAccount(id);
    }

    private IGenericAccountRelationService getAccountRelationService(String accountRelationType) {
        try {
            IGenericAccountRelationService acountRelationService = null;
            AccountRelationType relationType = AccountRelationType
                    .valueOf(accountRelationType.toUpperCase(AccountingConstants.LANGUAGE));
            switch (relationType) {
            case CUSTOMER:
                acountRelationService = accountCustomerService;
                break;
            case SUPPLIER:
                acountRelationService = accountSupplierService;
                break;
            case BANK:
                acountRelationService = accountBankService;
                break;
            case COFFER:
                acountRelationService = accountCofferService;
                break;
            case WITHHOLDINGTAX:
                acountRelationService = accountWithHoldingTaxService;
                break;
            default:
                throw new HttpCustomException(ApiErrors.Accounting.ACCOUNT_RELATION_IMPLEMENTATION_NONEXISTENT,
                        new ErrorsResponse().error(accountRelationType));
            }
            return acountRelationService;
        } catch (IllegalArgumentException e) {
            log.error(AccountingConstants.LOG_ILLEGAL_ARGUMENT_EXCEPTION, e);
            throw new HttpCustomException(ApiErrors.Accounting.ACCOUNT_RELATION_TYPE_INVALID,
                    new ErrorsResponse().error(accountRelationType));
        }
    }

    @GetMapping(value = "/account-relation/{accountRelationType}/account")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "ACCOUNTING","VIEW_ACCOUNTING_ACCOUNTS" })
    public List<GenericAccountRelationDto> findAllEntitiesInRelationWithAccount(
            @PathVariable String accountRelationType, @RequestParam(value = "id") Long accountId) {
        return getAccountRelationService(accountRelationType).findAllEntitiesInRelationWithAccount(accountId);
    }

    @GetMapping(value = "/account-relation/{accountRelationType}")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "ACCOUNTING","VIEW_ACCOUNTING_ACCOUNTS" })
    public GenericAccountRelation findAccountWithRelation(@PathVariable String accountRelationType,
            @RequestParam(value = "id") Long relationEntityId) {
        try {
            return getAccountRelationService(accountRelationType).findAccountWithRelation(relationEntityId)
                    .orElse(null);
        } catch (IllegalArgumentException e) {
            log.error(AccountingConstants.LOG_ILLEGAL_ARGUMENT_EXCEPTION, e);
            throw new HttpCustomException(ApiErrors.Accounting.ACCOUNT_RELATION_TYPE_INVALID,
                    new ErrorsResponse().error(accountRelationType));
        }
    }

    @PostMapping(value = "/account-relation/{accountRelationType}")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "ACCOUNTING","VIEW_ACCOUNTING_ACCOUNTS" })
    public GenericAccountRelationDto saveAccountRelation(@PathVariable String accountRelationType,
            @RequestBody @Valid GenericAccountRelationDto genericAccountRelationDto) {
        return getAccountRelationService(accountRelationType).saveAccountRelation(genericAccountRelationDto);
    }

    @DeleteMapping(value = "/account-relation/{accountRelationType}/{entityRelationId}")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "DELETE_ACCOUNTING_ACCOUNTS" })
    public void deleteAccountRelation(@PathVariable String accountRelationType, @PathVariable Long entityRelationId) {
        getAccountRelationService(accountRelationType).deleteAccountRelation(entityRelationId);
    }

    @GetMapping(value = "/generate-account-code")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "VIEW_ACCOUNTING_ACCOUNTS" })
    public int generateAccountCode(@RequestParam Long planId) {
        return accountService.generateAccountCode(planId);
    }

    @GetMapping(value = "/search-by-code")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "ACCOUNTING","VIEW_ACCOUNTING_ACCOUNTS" })
    public AccountDto findByCode(@RequestParam int code) {
        return accountService.findByCode(code);
    }

    @GetMapping(value = "/search-account")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "VIEW_ACCOUNTING_ACCOUNTS" })
    public AccountDto findAccountByCode(@RequestParam int code, @RequestParam String extremum) {
        return accountService.findAccount(code, extremum);
    }

    @PostMapping(value = "/filter-account")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "ACCOUNTING","VIEW_ACCOUNTING_ACCOUNTS" })
    public Page<AccountDto> filterAccount(@RequestBody List<Filter> filters, Pageable pageable) {
        return accountService.filterAccount(filters, pageable);
    }

    @GetMapping(value = "/tax-accounts")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "ACCOUNTING","VIEW_ACCOUNTING_ACCOUNTS" })
    public Map<String, List<AccountDto>> findTaxAccounts() {
        return accountService.findTaxAccounts();
    }

    @GetMapping(value = "/tiers-accounts")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "ACCOUNTING","VIEW_ACCOUNTING_ACCOUNTS" })
    public Map<String, List<AccountDto>> findTiersAccounts() {
        return accountService.findTiersAccounts();
    }

    @GetMapping(value = "/bank-accounts")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "ACCOUNTING","VIEW_ACCOUNTING_ACCOUNTS" })
    public List<AccountDto> findBankAccounts() {
        return accountService.findBankAccounts();
    }

    @GetMapping(value = "/coffer-accounts")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "ACCOUNTING","VIEW_ACCOUNTING_ACCOUNTS" })
    public List<AccountDto> findCofferAccounts() {
        return accountService.findCofferAccounts();
    }

    @GetMapping(value = "/with-holding-tax-accounts")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "ACCOUNTING","VIEW_ACCOUNTING_ACCOUNTS" })
    public List<AccountDto> findWithHoldingTaxAccounts() {
        return accountService.findWithHoldingTaxAccounts();
    }

    @GetMapping(value = "/excel-template")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "EXPORT_MODEL_ACCOUNTING_ACCOUNTS" })
    public ResponseEntity<byte[]> exportModel() {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add(HttpHeaders.CONTENT_TYPE, AccountingConstants.EXCEL_DOC_TYPE);
        return new ResponseEntity<>(accountService.exportAccountsExcelModel(), responseHeaders, HttpStatus.OK);
    }

    @PostMapping(value = "/import-accounts")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "IMPORT_ACCOUNTING_ACCOUNTS" })
    public ResponseEntity<FileUploadDto> importAccountsFromExcelFile(@RequestBody FileUploadDto fileUploadDto) {
        return new ResponseEntity<>(accountService.loadAccountsExcelData(fileUploadDto), HttpStatus.OK);
    }

    @GetMapping(value = "/export-accounts")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "EXPORT_ACCOUNTING_ACCOUNTS" })
    public ResponseEntity<byte[]> exportAccountsAsExcelFile() {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add(HttpHeaders.CONTENT_TYPE, AccountingConstants.EXCEL_DOC_TYPE);
        return new ResponseEntity<>(accountService.exportAccountsAsExcelFile(), responseHeaders, HttpStatus.OK);
    }
}
