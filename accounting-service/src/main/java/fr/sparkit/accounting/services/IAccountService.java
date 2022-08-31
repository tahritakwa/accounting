package fr.sparkit.accounting.services;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import fr.sparkit.accounting.dto.AccountDto;
import fr.sparkit.accounting.dto.FileUploadDto;
import fr.sparkit.accounting.dto.Filter;
import fr.sparkit.accounting.entities.Account;

@Service
public interface IAccountService extends IGenericService<Account, Long> {

    AccountDto findById(Long id);

    AccountDto saveAccount(AccountDto accountDto);

    int generateAccountCode(Long planId);

    AccountDto updateAccount(AccountDto accountDto);

    List<Account> findByPlanCode(Integer planCode);

    AccountDto findByCode(int code);

    AccountDto findAccount(Integer code, String extremum);

    boolean isDeleteAccount(Long id);

    Optional<Account> findAccountByCode(int code);

    boolean isAccountCodeUsedInMultipleAccounts(int code);

    Account findTierAccount(Long tierId, String tierName, boolean isSupplier);

    boolean existsById(Long accountId);

    boolean checkUsableAccount(Long idAccount);

    List<Account> findReconcilableAccounts();

    Map<String, List<AccountDto>> getAmortizationAndImmobilizationAccounts();

    Page<AccountDto> filterAccount(List<Filter> filters, Pageable pageable);

    Map<String, List<AccountDto>> findTaxAccounts();

    Map<String, List<AccountDto>> findTiersAccounts();

    List<AccountDto> findBankAccounts();

    List<AccountDto> findCofferAccounts();

    List<AccountDto> findWithHoldingTaxAccounts();

    byte[] exportAccountsExcelModel();

    List<AccountDto> getResultAccounts();

    FileUploadDto loadAccountsExcelData(FileUploadDto fileUploadDto);

    byte[] exportAccountsAsExcelFile();

}
