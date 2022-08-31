package fr.sparkit.accounting.dao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fr.sparkit.accounting.dto.AccountBalanceDto;
import fr.sparkit.accounting.dto.AuxiliaryJournalDetailsDto;
import fr.sparkit.accounting.dto.AuxiliaryJournalDto;
import fr.sparkit.accounting.dto.CentralizingJournalDetailsByMonthDto;
import fr.sparkit.accounting.dto.DocumentAccountingDto;
import fr.sparkit.accounting.dto.GeneralLedgerAmountDto;
import fr.sparkit.accounting.dto.InitialTrialBalanceDto;
import fr.sparkit.accounting.dto.TrialBalanceAccountAmountDto;
import fr.sparkit.accounting.entities.DocumentAccountLine;

@Repository
public interface DocumentAccountLineDao extends BaseRepository<DocumentAccountLine, Long> {

    List<DocumentAccountLine> findByDocumentAccountIdAndIsDeletedFalseOrderByIdDesc(Long documentAccountId);

    List<DocumentAccountLine> findByDocumentAccountIdInAndIsDeletedFalseOrderByIdDesc(List<Long> documentAccountId);

    @Query("SELECT dal FROM DocumentAccountLine dal where isDeleted=false"
            + " AND (account.plan.code LIKE '1%' OR account.plan.code LIKE '2%'"
            + " OR account.plan.code LIKE '3%' OR account.plan.code LIKE '4%'"
            + " OR account.plan.code LIKE '5%') AND letter IS NULL AND documentAccount.documentDate BETWEEN ?1 AND ?2")
    List<DocumentAccountLine> findLinesWithNoLetterForBalancedAccountsInFiscalYear(LocalDateTime startDate,
            LocalDateTime endDate);

    @Query("SELECT dal FROM DocumentAccountLine dal where isDeleted=false"
            + " AND (account.plan.code LIKE '6%' OR account.plan.code LIKE '7%')"
            + " AND letter IS NULL AND documentAccount.documentDate BETWEEN ?1 AND ?2")
    List<DocumentAccountLine> findLinesWithNoLetterForRevenueAndExpensesAccountsInFiscalYear(LocalDateTime startDate,
            LocalDateTime endDate);

    DocumentAccountLine findByIdAndIsDeletedFalse(Long id);

    List<DocumentAccountLine> findByLetterIsNullAndIsDeletedFalse();

    @Query("SELECT count(id) FROM DocumentAccountLine where isDeleted=false and letter is not NULL"
            + " and documentAccount.id=?1")
    Long getCountLetteredLinesByDocumentAccountId(Long id);

    @Query("SELECT sum(creditAmount)-sum(debitAmount) FROM DocumentAccountLine "
            + "where isDeleted=false and account.plan.code LIKE CONCAT(:chartAccountCode,'%')"
            + " and documentAccount.fiscalYear.id=:fiscalYearId")
    Optional<BigDecimal> totalDifferenceBetweenCreditAndDebitByChartAccountInFiscalYear(
            @Param("chartAccountCode") String chartAccountCode, @Param("fiscalYearId") Long fiscalYearId);

    @Query("SELECT sum(creditAmount)-sum(debitAmount) FROM DocumentAccountLine "
            + "where isDeleted=false and account.plan.code LIKE CONCAT(:chartAccountCode,'%')"
            + " and documentAccount.fiscalYear.id=:fiscalYearId and documentAccount.journal.cashFlow=true")
    Optional<BigDecimal> totalDifferenceBetweenCreditAndDebitByChartAccountInFiscalYearAndJournalIsCashFlow(
            @Param("chartAccountCode") String chartAccountCode, @Param("fiscalYearId") Long fiscalYearId);

    @Query("SELECT sum(creditAmount)-sum(debitAmount) FROM DocumentAccountLine "
            + "where isDeleted=false and account.id=:accountId and documentAccount.fiscalYear.id=:fiscalYearId")
    Optional<BigDecimal> totalDifferenceBetweenCreditAndDebitByAccountInFiscalYear(@Param("accountId") Long accountId,
            @Param("fiscalYearId") Long fiscalYearId);

    @Query("SELECT sum(creditAmount)-sum(debitAmount) FROM DocumentAccountLine "
            + "where isDeleted=false and account.id=:accountId and documentAccount.fiscalYear.id=:fiscalYearId and documentAccount.journal.cashFlow=true")
    Optional<BigDecimal> totalDifferenceBetweenCreditAndDebitByAccountInFiscalYearAndJournalIsCashFlow(
            @Param("accountId") Long accountId, @Param("fiscalYearId") Long fiscalYearId);

    @Query("SELECT sum(debitAmount) FROM DocumentAccountLine where isDeleted=false and documentAccount.journal.id=?1"
            + " and documentAccount.documentDate between ?2 and ?3")
    Optional<BigDecimal> totalAmountForJournalInBetween(Long journalId, LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT distinct account.code FROM DocumentAccountLine where isDeleted=false"
            + " and account.code between ?1 and ?2 and documentAccount.documentDate between ?3 and ?4"
            + " group by account.code order by account.code")
    Page<Long> getAllAccountsInCurrentDatesAndAccountCodes(int beginAccountCode, int endAccountCode,
            LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    @Query("SELECT da FROM DocumentAccountLine da" + " where da.account.id like ?1 and da.isDeleted=false and "
            + "(debitAmount + creditAmount) between ?2 and ?3 and documentAccount.documentDate between ?4 and ?5 ")
    Page<DocumentAccountLine> findByAccountIdInCurrentDatesAndAccountCodesAndAmounts(Long accountId,
            BigDecimal beginAmount, BigDecimal endAmount, LocalDateTime startDate, LocalDateTime endDate,
            Pageable pageable);

    @Query("SELECT da FROM DocumentAccountLine da" + " where da.account.id like ?1 and da.isDeleted=false and "
            + " da.letter IS NOT NULL and "
            + "(debitAmount + creditAmount) between ?2 and ?3 and documentAccount.documentDate between ?4 and ?5 ")
    Page<DocumentAccountLine> findByAccountIdInCurrentDatesAndAccountCodesAndAmountsLettring(Long accountId,
            BigDecimal beginAmount, BigDecimal endAmount, LocalDateTime startDate, LocalDateTime endDate,
            Pageable pageable);

    @Query("SELECT da FROM DocumentAccountLine da" + " where da.account.id like ?1 and da.isDeleted=false and "
            + " da.letter IS NULL and "
            + "(debitAmount + creditAmount) between ?2 and ?3 and documentAccount.documentDate between ?4 and ?5 ")
    Page<DocumentAccountLine> findByAccountIdInCurrentDatesAndAccountCodesAndAmountsNotLettring(Long accountId,
            BigDecimal beginAmount, BigDecimal endAmount, LocalDateTime startDate, LocalDateTime endDate,
            Pageable pageable);

    @Query("SELECT da FROM DocumentAccountLine da" + " where da.account.id like ?1 and da.isDeleted=false and "
            + "(debitAmount + creditAmount) >= ?2 and " + "documentAccount.documentDate between ?3 and ?4 ")
    Page<DocumentAccountLine> findByAccountIdInCurrentDatesAndAccountCodesWithoutEndAmount(Long accountId,
            BigDecimal beginAmount, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    @Query("SELECT da FROM DocumentAccountLine da" + " where da.account.id like ?1 and da.isDeleted=false and "
            + " da.letter IS NULL and " + "(debitAmount + creditAmount) >= ?2 and "
            + "documentAccount.documentDate between ?3 and ?4 ")
    Page<DocumentAccountLine> findByAccountIdInCurrentDatesAndAccountCodesWithoutEndAmountNotLettring(Long accountId,
            BigDecimal beginAmount, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    @Query("SELECT da FROM DocumentAccountLine da" + " where da.account.id like ?1 and da.isDeleted=false and "
            + " da.letter IS NOT NULL and " + "(debitAmount + creditAmount) >= ?2 and "
            + "documentAccount.documentDate between ?3 and ?4 ")
    Page<DocumentAccountLine> findByAccountIdInCurrentDatesAndAccountCodesWithoutEndAmountLettring(Long accountId,
            BigDecimal beginAmount, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    @Query("SELECT distinct reference FROM DocumentAccountLine where isDeleted=false and account.code like ?1"
            + " and reference is not null and reference <> '' "
            + "and letter IS NULL and documentAccount.documentDate between ?2 and ?3 order by reference")
    List<String> getDistinctReferenceByAccountAndLetterIsNull(Integer code, LocalDateTime startDate,
            LocalDateTime endDate);

    @Query("SELECT distinct account.code FROM DocumentAccountLine where letter IS NOT NULL"
            + " and isDeleted=false and account.code between ?1 and ?2 and account.literable=true"
            + " and documentAccount.documentDate between ?3 and ?4 order by account.code")
    Page<Integer> getLiterableAccountHavingLetteredLines(int beginAccountCode, int endAccountCode,
            LocalDateTime startDate, LocalDateTime endDate, Pageable pageableOfAccounts);

    @Query("SELECT distinct account.code FROM DocumentAccountLine where letter IS NULL"
            + " and isDeleted=false and account.code between ?1 and ?2 and account.literable=true"
            + " and documentAccount.documentDate between ?3 and ?4 order by account.code")
    Page<Integer> getLiterableAccountHavingNotLetteredLines(int beginAccountCode, int endAccountCode,
            LocalDateTime startDate, LocalDateTime endDate, Pageable pageableOfAccounts);

    @Query("SELECT da FROM DocumentAccountLine da" + " where da.account.code like ?1 and da.isDeleted=false"
            + " and da.letter IS NULL and documentAccount.documentDate between ?2 and ?3 ")
    Page<DocumentAccountLine> findByAccountCodeAndLetterIsNull(Integer code, LocalDateTime startDate,
            LocalDateTime endDate, Pageable pageableOfDocumentAccountLines);

    @Query("SELECT da FROM DocumentAccountLine da" + " where da.account.code like ?1 and da.isDeleted=false"
            + " and da.letter IS NOT NULL and documentAccount.documentDate between ?2 and ?3 ")
    Page<DocumentAccountLine> findByAccountCodeAndLetterIsNotNull(Integer code, LocalDateTime startDate,
            LocalDateTime endDate, Pageable pageableOfDocumentAccountLines);

    @Query("SELECT da FROM DocumentAccountLine da"
            + " where da.account.code like ?1 and da.isDeleted=false and da.letter IS NULL and da.reference=?4"
            + " and documentAccount.documentDate between ?2 and ?3 order by da.documentAccount.documentDate desc")
    List<DocumentAccountLine> findByAccountCodeAndReferenceAndLetterIsNull(Integer code, LocalDateTime startDate,
            LocalDateTime endDate, String reference);

    @Query("SELECT NEW fr.sparkit.accounting.dto.AccountBalanceDto(account.id, account.code, account.label,"
            + " sum(debitAmount),sum(creditAmount), account.literable) FROM DocumentAccountLine where isDeleted=false"
            + " and account.code in ?1 and documentAccount.documentDate"
            + " between ?2 and ?3 and (debitAmount + creditAmount) between ?4 and ?5"
            + " GROUP BY account.id, account.code, account.label, account.literable order by account.code")
    List<AccountBalanceDto> getGeneralLedgerAccountInDatesAndAmounts(List<Long> codes, LocalDateTime startDate,
            LocalDateTime endDate, BigDecimal beginAmount, BigDecimal endAmount);

    @Query("SELECT NEW fr.sparkit.accounting.dto.AccountBalanceDto(account.id, account.code, account.label,"
            + " sum(debitAmount),sum(creditAmount),account.literable) FROM DocumentAccountLine where isDeleted=false"
            + " and account.code in ?1 and documentAccount.documentDate between ?2 and ?3"
            + " and (debitAmount + creditAmount) >= ?4 GROUP BY account.id, account.code, account.label, account.literable "
            + "order by account.code")
    List<AccountBalanceDto> getGeneralLedgerAccountInDatesWithoutEndAmount(List<Long> codes, LocalDateTime startDate,
            LocalDateTime endDate, BigDecimal beginAmount);

    @Query("SELECT NEW fr.sparkit.accounting.dto.TrialBalanceAccountAmountDto(documentAccount.indexOfStatus, "
            + "sum(debitAmount),sum(creditAmount)) FROM DocumentAccountLine "
            + "where isDeleted=false and account.id=?1 and documentAccount.documentDate between ?2 and ?3 group by "
            + "documentAccount.indexOfStatus")
    List<TrialBalanceAccountAmountDto> getTrialBalanceAccountAmountsInDates(Long id, LocalDateTime startDate,
            LocalDateTime endDate);

    @Query("SELECT NEW fr.sparkit.accounting.dto.TrialBalanceAccountAmountDto(documentAccount.indexOfStatus, "
            + "sum(debitAmount),sum(creditAmount)) FROM DocumentAccountLine where isDeleted=false"
            + " and documentAccount.documentDate between ?1 and ?2 and account.code like CONCAT(?5,'%') "
            + "and account.code between ?3 and ?4" + " group by documentAccount.indexOfStatus")
    List<TrialBalanceAccountAmountDto> getTrialBalanceAccountAmountsInClassAndDatesAndAccountCodes(
            LocalDateTime startDate, LocalDateTime endDate, int beginAccountCode, int endAccountCode,
            String classNumber);

    @Query("SELECT NEW fr.sparkit.accounting.dto.TrialBalanceAccountAmountDto(documentAccount.indexOfStatus, "
            + "sum(debitAmount),sum(creditAmount)) FROM DocumentAccountLine where isDeleted=false"
            + " and documentAccount.documentDate between ?1 and ?2 and account.code between ?3 and ?4"
            + " group by documentAccount.indexOfStatus")
    List<TrialBalanceAccountAmountDto> getTrialBalanceAccountAmountsInDatesAndAccountCodes(LocalDateTime startDate,
            LocalDateTime endDate, int beginAccountCode, int endAccountCode);

    @Query("SELECT NEW fr.sparkit.accounting.dto.InitialTrialBalanceDto(account.id,sum(debitAmount),sum(creditAmount))"
            + " FROM DocumentAccountLine where isDeleted=false and account.code in ?1"
            + " and documentAccount.documentDate between ?2 and ?3 and documentAccount.indexOfStatus = 2"
            + " GROUP BY account.id")
    List<InitialTrialBalanceDto> initialTrialBalance(List<Long> codes, LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT NEW fr.sparkit.accounting.dto.AccountBalanceDto(account.id, account.code, account.label,"
            + " sum(debitAmount),sum(creditAmount), account.literable) FROM DocumentAccountLine where isDeleted=false"
            + " and account.code in ?1 and documentAccount.documentDate"
            + " between ?2 and ?3 and documentAccount.indexOfStatus != 2"
            + " GROUP BY account.id, account.code, account.label, account.literable" + " order by account.code")
    List<AccountBalanceDto> currentTrialBalance(List<Long> codes, LocalDateTime startDate, LocalDateTime endDate);

    @Query(value = "SELECT dal From DocumentAccountLine dal WHERE isDeleted=false AND debitAmount!=0"
            + " AND Letter IS NULL ORDER BY AccountId")
    List<DocumentAccountLine> findByLetterIsNullWithDebit();

    DocumentAccountLine findFirstLetterByLetterNotNullAndIsDeletedFalseOrderByLetterDesc();

    DocumentAccountLine findFirstLetterByLetterAndIsDeletedFalse(String letter);

    List<DocumentAccountLine> findByAccountIdAndDebitAmountAndLetterIsNullAndIsDeletedFalse(Long accountId,
            BigDecimal debitAmount);

    @Query(value = "SELECT dal FROM DocumentAccountLine dal WHERE Letter IS NULL AND isDeleted=false")
    List<DocumentAccountLine> getNotLetteredLines();

    @Query(value = "SELECT dal FROM DocumentAccountLine dal WHERE Letter IS NOT NULL AND Letter NOT LIKE '-%'"
            + " AND isDeleted=false ORDER BY Letter")
    List<DocumentAccountLine> getLetteredLines();

    @Query("SELECT NEW fr.sparkit.accounting.dto.AccountBalanceDto("
            + "account.id, account.code, account.label, sum(da.debitAmount), sum(da.creditAmount), account.literable)"
            + " from Account account, DocumentAccountLine da where account.id=da.account.id and"
            + " account.code like CONCAT(?1,'%') and da.documentAccount.documentDate between ?2 and ?3"
            + " and da.documentAccount.isDeleted=false and da.isDeleted=false and account.isDeleted=false"
            + " group by account.id, account.code, account.label, account.literable order by account.code")
    List<AccountBalanceDto> getAccountAnnexe(String planCode, LocalDateTime startDate, LocalDateTime endDate);

    @Query(value = "SELECT DAL_LETTER from T_DOCUMENT_ACCOUNT_LINE WHERE DAL_IS_DELETED=0"
            + " AND DAL_LETTER IS NOT NULL GROUP by DAL_LETTER ORDER BY DAL_LETTER ASC", nativeQuery = true)
    List<String> getAllLetter();

    @Query("SELECT dal FROM DocumentAccountLine dal where dal.isDeleted=false and dal.account.id = ?1"
            + " and dal.documentAccount.documentDate between ?2 and ?3 and documentAccount.journal.reconcilable=true"
            + " and dal.isClose=false ORDER BY dal.documentLineDate DESC")
    List<DocumentAccountLine> getReconcilableDocumentAccountLineNotCloseInBetween(Long accountId,
            LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT dal FROM DocumentAccountLine dal "
            + "where dal.isDeleted=false and dal.account.id = ?1 and dal.documentAccount.documentDate "
            + "between ?2 and ?3 and documentAccount.journal.reconcilable=true and dal.isClose=false and"
            + " dal.id NOT IN ?4 ORDER BY dal.documentLineDate DESC")
    List<DocumentAccountLine> getReconcilableDocumentAccountLineNotCloseInBetween(Long accountId,
            LocalDateTime startDate, LocalDateTime endDate, List<Long> documentAccountLineAffectedId);

    @Query("SELECT dal FROM DocumentAccountLine dal where dal.isDeleted=false and dal.account.id = ?1 "
            + "and dal.documentAccount.documentDate between ?2 and ?3 and documentAccount.journal.reconcilable=true"
            + " and dal.isClose=false")
    List<DocumentAccountLine> getInitialReconcilableDocumentAccountLineNotCloseInBetween(Long accountId,
            LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT dal FROM DocumentAccountLine dal where" + " isDeleted=false and documentAccount.id in ?1")
    List<DocumentAccountLine> getDocumentLineDtosByDocumentIds(List<Long> documentIds);

    @Query("SELECT dal FROM DocumentAccountLine dal where dal.isDeleted=false and dal.account.id Like ?1"
            + " and dal.isClose=true and dal.reconciliationDate between ?2 and ?3")
    List<DocumentAccountLine> getCloseDocumentAccountLineInBetween(Long accountId, LocalDate startDate,
            LocalDate endDate);

    @Query("SELECT dal FROM DocumentAccountLine dal where dal.isDeleted=false and dal.account.id Like ?1 "
            + "and dal.isClose=true and dal.id NOT IN ?4 and dal.reconciliationDate between ?2 and ?3")
    List<DocumentAccountLine> getCloseDocumentAccountLineInBetween(Long accountId, LocalDate startDate,
            LocalDate endDate, List<Long> documentAccountLineReleased);

    @Query("SELECT dal FROM DocumentAccountLine dal "
            + "where dal.isDeleted=false and dal.account.id Like ?1 and dal.isClose=true"
            + " and dal.reconciliationDate between ?2 and ?3")
    List<DocumentAccountLine> getInitialCloseDocumentAccountLineInBetween(Long accountId, LocalDate startDate,
            LocalDate endDate);

    @Transactional
    @Modifying
    @Query("UPDATE DocumentAccountLine SET isClose=true, reconciliationDate=?2 where id in ?1")
    void setDocumentAccountLineIsClose(List<Long> ids, LocalDate reconciliationDate);

    @Transactional
    @Modifying
    @Query("UPDATE DocumentAccountLine SET isClose=false, reconciliationDate= NULL where id in ?1")
    void setDocumentAccountLineIsNotClose(List<Long> ids);

    @Query("SELECT sum(debitAmount) FROM DocumentAccountLine " + "where isDeleted=false and documentAccount.id=?1")
    Optional<BigDecimal> totalDebitAmountDocumentAccount(Long documentAccountId);

    @Query("SELECT sum(creditAmount) FROM DocumentAccountLine " + "where isDeleted=false and documentAccount.id=?1")
    Optional<BigDecimal> totalCreditAmountDocumentAccount(Long documentAccountId);

    @Query(value = "SELECT SUM(DAL_DEBIT_AMOUNT)-SUM(DAL_CREDIT_AMOUNT) FROM ( "
            + "SELECT TOP (?1) T_DOCUMENT_ACCOUNT_LINE.DAL_DEBIT_AMOUNT, T_DOCUMENT_ACCOUNT_LINE.DAL_CREDIT_AMOUNT"
            + " FROM T_DOCUMENT_ACCOUNT_LINE, T_ACCOUNT, T_DOCUMENT_ACCOUNT"
            + " where T_DOCUMENT_ACCOUNT_LINE.DAL_IS_DELETED=0 and T_DOCUMENT_ACCOUNT_LINE.DAL_ACCOUNT_ID = T_ACCOUNT.AC_ID"
            + " and T_ACCOUNT.AC_ID=?2 and T_DOCUMENT_ACCOUNT_LINE.DAL_DOCUMENT_ACCOUNT_ID = T_DOCUMENT_ACCOUNT.DA_ID"
            + " and T_DOCUMENT_ACCOUNT.DA_DOCUMENT_DATE between ?3 and ?4 and"
            + " (T_DOCUMENT_ACCOUNT_LINE.DAL_DEBIT_AMOUNT + T_DOCUMENT_ACCOUNT_LINE.DAL_CREDIT_AMOUNT) between ?5 and ?6"
            + " order by CONVERT (varchar(8), T_DOCUMENT_ACCOUNT.DA_DOCUMENT_DATE, 112) desc,"
            + " T_DOCUMENT_ACCOUNT.DA_CODE_DOCUMENT desc, T_DOCUMENT_ACCOUNT_LINE.DAL_ID desc ) as balanceLastPage", nativeQuery = true)
    Optional<BigDecimal> balanceLastPage(int numberOfElement, Long accountId, LocalDateTime startDate,
            LocalDateTime endDate, BigDecimal beginAmount, BigDecimal endAmount);

    @Query(value = "SELECT SUM(DAL_DEBIT_AMOUNT)-SUM(DAL_CREDIT_AMOUNT) FROM ( "
            + "SELECT TOP (?1) T_DOCUMENT_ACCOUNT_LINE.DAL_DEBIT_AMOUNT, T_DOCUMENT_ACCOUNT_LINE.DAL_CREDIT_AMOUNT"
            + " FROM T_DOCUMENT_ACCOUNT_LINE, T_ACCOUNT, T_DOCUMENT_ACCOUNT  where T_DOCUMENT_ACCOUNT_LINE.DAL_IS_DELETED=0"
            + " and T_DOCUMENT_ACCOUNT_LINE.DAL_ACCOUNT_ID = T_ACCOUNT.AC_ID and T_ACCOUNT.AC_ID=?2"
            + " and T_DOCUMENT_ACCOUNT_LINE.DAL_DOCUMENT_ACCOUNT_ID = T_DOCUMENT_ACCOUNT.DA_ID"
            + " and T_DOCUMENT_ACCOUNT.DA_DOCUMENT_DATE between ?3 and ?4"
            + " and (T_DOCUMENT_ACCOUNT_LINE.DAL_DEBIT_AMOUNT + T_DOCUMENT_ACCOUNT_LINE.DAL_CREDIT_AMOUNT) >= ?5 "
            + " order by CONVERT (varchar(8), T_DOCUMENT_ACCOUNT.DA_DOCUMENT_DATE, 112) desc,"
            + " T_DOCUMENT_ACCOUNT.DA_CODE_DOCUMENT desc, T_DOCUMENT_ACCOUNT_LINE.DAL_ID desc ) as balanceLastPage", nativeQuery = true)
    Optional<BigDecimal> balanceLastPage(int numberOfElement, Long accountId, LocalDateTime startDate,
            LocalDateTime endDate, BigDecimal beginAmount);

    @Query("SELECT dal FROM DocumentAccountLine dal "
            + "where dal.isDeleted=false and dal.account.code between ?1 and ?2 and "
            + "dal.documentAccount.documentDate between ?3 and ?4 order by dal.account.code asc, "
            + "CONVERT (varchar(8), dal.documentAccount.documentDate, 112) desc,"
            + " dal.documentAccount.codeDocument desc, dal.id desc")
    List<DocumentAccountLine> findAllByAccountCodeAndDates(int beginAccountCode, int endAccountCode,
            LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT count(id) FROM DocumentAccountLine where isDeleted=false and isClose=true and documentAccount.id=?1")
    Long getTotalReconcilableLinesByDocumentAccountId(Long id);

    @Query("SELECT NEW fr.sparkit.accounting.dto.CentralizingJournalDetailsByMonthDto(dal.account.code / ?4,"
            + " sum(dal.debitAmount) , sum(dal.creditAmount) ) FROM DocumentAccountLine dal where dal.isDeleted=false"
            + " and dal.documentAccount.journal.id = ?1 and dal.documentAccount.documentDate between ?2 and ?3"
            + " and CONVERT(varchar(150),dal.account.plan.code) NOT LIKE CONCAT(?5,'%')"
            + " and CONVERT(varchar(150),dal.account.plan.code) NOT LIKE CONCAT(?6,'%') GROUP BY dal.account.code")
    List<CentralizingJournalDetailsByMonthDto> getCentralizingJournalDetailsDto(Long journalId, LocalDateTime startDate,
            LocalDateTime endDate, int breakingAccount, String customerAccount, String supplierAccount);

    @Query("SELECT NEW fr.sparkit.accounting.dto.CentralizingJournalDetailsByMonthDto(dal.account.code / ?4,"
            + " sum(dal.debitAmount) , sum(dal.creditAmount) ) FROM DocumentAccountLine dal where dal.isDeleted=false"
            + " and dal.documentAccount.journal.id = ?1 and dal.documentAccount.documentDate between ?2 and ?3"
            + " and CONVERT(varchar(150),dal.account.plan.code) LIKE CONCAT(?5,'%') GROUP BY dal.account.code")
    List<CentralizingJournalDetailsByMonthDto> getCentralizingJournalTiersDetailsDto(Long journalId,
            LocalDateTime startDate, LocalDateTime endDate, int breakingAccount, String tierAccount);

    @Query("SELECT dal FROM DocumentAccountLine dal where dal.isDeleted=false"
            + " and dal.documentAccount.journal.id = ?1 and dal.documentAccount.documentDate between ?2 and ?3")
    List<DocumentAccountLine> findDocumentAccountLineByJournalAndDocumentDateBetween(Long journalId,
            LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT NEW fr.sparkit.accounting.dto.AuxiliaryJournalDto(j.id, j.code,"
            + " j.label, sum(dal.debitAmount), sum(dal.creditAmount)) FROM DocumentAccountLine dal, "
            + " Journal j where dal.documentAccount.journal.id = j.id  AND dal.isDeleted=false and"
            + " dal.documentAccount.journal.id = ?3 and"
            + " dal.documentAccount.documentDate between ?1 and ?2 GROUP BY j.id, j.code, j.label")
    Optional<AuxiliaryJournalDto> findAuxiliaryJournalDto(LocalDateTime startDate, LocalDateTime endDate,
            Long journalId);

    @Query("SELECT NEW fr.sparkit.accounting.dto.AuxiliaryJournalDetailsDto(dal.id, dal.documentAccount.documentDate,"
            + " dal.documentAccount.codeDocument, dal.documentLineDate, dal.account.code,"
            + " dal.label, dal.debitAmount, dal.creditAmount, dal.documentAccount.id) FROM DocumentAccountLine dal"
            + " where dal.isDeleted=false and dal.documentAccount.journal.id = ?1"
            + " and dal.documentAccount.documentDate between ?2 and ?3")
    Page<AuxiliaryJournalDetailsDto> getByIdAuxiliaryJournalDetailsPage(Long journalId, LocalDateTime startDate,
            LocalDateTime endDate, Pageable pageable);

    @Query("SELECT dal FROM DocumentAccountLine dal where dal.isDeleted=false"
            + " and dal.documentAccount.journal.id = ?1 and dal.documentAccount.documentDate between ?2 and ?3")
    List<DocumentAccountLine> findAuxiliaryJournalDetails(Long journalId, LocalDateTime startDate,
            LocalDateTime endDate);

    @Query("SELECT distinct account.code FROM DocumentAccountLine where isDeleted=false"
            + " and account.code between :beginAccountCode and :endAccountCode and "
            + "documentAccount.documentDate between :startDate and :endDate"
            + " and (debitAmount + creditAmount) between :beginAmount and :endAmount"
            + " and ((:accountType IS true and account.literable=true)"
            + " or (:accountType IS false and account.literable=false) or (:accountType IS NULL))"
            + " group by account.code order by account.code")
    Page<Long> getAllAccountsInCurrentDatesAndAccountCodesAndAmounts(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate, @Param("beginAccountCode") int beginAccountCode,
            @Param("endAccountCode") int endAccountCode, @Param("beginAmount") BigDecimal beginAmount,
            @Param("endAmount") BigDecimal endAmount, @Param("accountType") Boolean accountType, Pageable pageable);

    @Query("SELECT distinct account.code FROM DocumentAccountLine where isDeleted=false"
            + " and account.code between :beginAccountCode and :endAccountCode"
            + " and documentAccount.documentDate between :startDate and :endDate"
            + " and (debitAmount + creditAmount) >= :beginAmount"
            + " and ((:accountType IS true and account.literable=true)"
            + " or (:accountType IS false and account.literable=false) or (:accountType IS NULL))"
            + " group by account.code order by account.code")
    Page<Long> getAllAccountsInCurrentDatesAndAccountCodesWithoutEndAmount(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate, @Param("beginAccountCode") int beginAccountCode,
            @Param("endAccountCode") int endAccountCode, @Param("beginAmount") BigDecimal beginAmount,
            @Param("accountType") Boolean accountType, Pageable pageable);

    @Query("SELECT NEW fr.sparkit.accounting.dto.GeneralLedgerAmountDto(sum(debitAmount),sum(creditAmount))"
            + " FROM DocumentAccountLine where "
            + "isDeleted=false and account.code between :beginAccountCode and :endAccountCode and "
            + "letter IS NOT NULL and documentAccount.documentDate between :startDate and :endDate"
            + " and (debitAmount + creditAmount) between :beginAmount and :endAmount"
            + " and ((:accountType IS true and account.literable=true)"
            + " or (:accountType IS false and account.literable=false) or (:accountType IS NULL)) ")
    GeneralLedgerAmountDto getGeneralLedgerTotalAmountsInDatesAndAccountCodesAndAmountsLetteredLines(
            @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate,
            @Param("beginAccountCode") int beginAccountCode, @Param("endAccountCode") int endAccountCode,
            @Param("beginAmount") BigDecimal beginAmount, @Param("endAmount") BigDecimal endAmount,
            @Param("accountType") Boolean accountType);

    @Query("SELECT NEW fr.sparkit.accounting.dto.GeneralLedgerAmountDto(sum(debitAmount),sum(creditAmount))"
            + " FROM DocumentAccountLine where "
            + "isDeleted=false and account.code between :beginAccountCode and :endAccountCode and "
            + "letter IS NULL and documentAccount.documentDate between :startDate and :endDate"
            + " and (debitAmount + creditAmount) between :beginAmount and :endAmount"
            + " and ((:accountType IS true and account.literable=true)"
            + " or (:accountType IS false and account.literable=false) or (:accountType IS NULL)) ")
    GeneralLedgerAmountDto getGeneralLedgerTotalAmountsInDatesAndAccountCodesAndAmountsNotLetteredLines(
            @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate,
            @Param("beginAccountCode") int beginAccountCode, @Param("endAccountCode") int endAccountCode,
            @Param("beginAmount") BigDecimal beginAmount, @Param("endAmount") BigDecimal endAmount,
            @Param("accountType") Boolean accountType);

    @Query("SELECT NEW fr.sparkit.accounting.dto.GeneralLedgerAmountDto(sum(debitAmount),sum(creditAmount))"
            + " FROM DocumentAccountLine where "
            + "isDeleted=false and account.code between :beginAccountCode and :endAccountCode and "
            + "documentAccount.documentDate between :startDate and :endDate"
            + " and (debitAmount + creditAmount) between :beginAmount and :endAmount"
            + " and ((:accountType IS true and account.literable=true)"
            + " or (:accountType IS false and account.literable=false) or (:accountType IS NULL)) ")
    GeneralLedgerAmountDto getGeneralLedgerTotalAmountsInDatesAndAccountCodesAndAmounts(
            @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate,
            @Param("beginAccountCode") int beginAccountCode, @Param("endAccountCode") int endAccountCode,
            @Param("beginAmount") BigDecimal beginAmount, @Param("endAmount") BigDecimal endAmount,
            @Param("accountType") Boolean accountType);

    @Query("SELECT NEW fr.sparkit.accounting.dto.GeneralLedgerAmountDto(sum(debitAmount),sum(creditAmount))"
            + " FROM DocumentAccountLine where isDeleted=false and letter IS NOT NULL and account.code between :beginAccountCode"
            + " and :endAccountCode and documentAccount.documentDate between :startDate and :endDate"
            + " and  (debitAmount + creditAmount) >= :beginAmount and ((:accountType IS true and account.literable=true)"
            + " or (:accountType IS false and account.literable=false) or (:accountType IS NULL)) ")
    GeneralLedgerAmountDto getGeneralLedgerTotalAmountsInDatesAndAccountCodesWithoutEndAmountLetteredLines(
            @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate,
            @Param("beginAccountCode") int beginAccountCode, @Param("endAccountCode") int endAccountCode,
            @Param("beginAmount") BigDecimal beginAmount, @Param("accountType") Boolean accountType);

    @Query("SELECT NEW fr.sparkit.accounting.dto.GeneralLedgerAmountDto(sum(debitAmount),sum(creditAmount))"
            + " FROM DocumentAccountLine where isDeleted=false and letter IS NULL and account.code between :beginAccountCode"
            + " and :endAccountCode and documentAccount.documentDate between :startDate and :endDate"
            + " and  (debitAmount + creditAmount) >= :beginAmount and ((:accountType IS true and account.literable=true)"
            + " or (:accountType IS false and account.literable=false) or (:accountType IS NULL)) ")
    GeneralLedgerAmountDto getGeneralLedgerTotalAmountsInDatesAndAccountCodesWithoutEndAmountNotLetteredLines(
            @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate,
            @Param("beginAccountCode") int beginAccountCode, @Param("endAccountCode") int endAccountCode,
            @Param("beginAmount") BigDecimal beginAmount, @Param("accountType") Boolean accountType);

    @Query("SELECT NEW fr.sparkit.accounting.dto.GeneralLedgerAmountDto(sum(debitAmount),sum(creditAmount))"
            + " FROM DocumentAccountLine where isDeleted=false and account.code between :beginAccountCode"
            + " and :endAccountCode and documentAccount.documentDate between :startDate and :endDate"
            + " and  (debitAmount + creditAmount) >= :beginAmount and ((:accountType IS true and account.literable=true)"
            + " or (:accountType IS false and account.literable=false) or (:accountType IS NULL)) ")
    GeneralLedgerAmountDto getGeneralLedgerTotalAmountsInDatesAndAccountCodesWithoutEndAmount(
            @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate,
            @Param("beginAccountCode") int beginAccountCode, @Param("endAccountCode") int endAccountCode,
            @Param("beginAmount") BigDecimal beginAmount, @Param("accountType") Boolean accountType);

    @Query("SELECT dal FROM DocumentAccountLine dal where "
            + "dal.isDeleted=false and dal.letter IS NOT NULL and "
            + " dal.account.code between :beginAccountCode and :endAccountCode and "
            + "dal.documentAccount.documentDate between :startDate and :endDate"
            + " and (debitAmount + creditAmount) between :beginAmount and :endAmount"
            + " and ((:accountType IS true and account.literable=true)"
            + " or (:accountType IS false and account.literable=false) or (:accountType IS NULL)) ")
    List<DocumentAccountLine> findAllDocumentAccountLinesInDatesAndAccountCodesAndAmountsLettering(
            @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate,
            @Param("beginAccountCode") int beginAccountCode, @Param("endAccountCode") int endAccountCode,
            @Param("beginAmount") BigDecimal beginAmount, @Param("endAmount") BigDecimal endAmount,
            @Param("accountType") Boolean accountType, Sort sortField);

    @Query("SELECT dal FROM DocumentAccountLine dal where " + "dal.isDeleted=false and dal.letter IS  NULL "
            + " and dal.account.code between :beginAccountCode and :endAccountCode and "
            + "dal.documentAccount.documentDate between :startDate and :endDate"
            + " and (debitAmount + creditAmount) between :beginAmount and :endAmount"
            + " and ((:accountType IS true and account.literable=true)"
            + " or (:accountType IS false and account.literable=false) or (:accountType IS NULL)) ")
    List<DocumentAccountLine> findAllDocumentAccountLinesInDatesAndAccountCodesAndAmountsNotLettering(
            @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate,
            @Param("beginAccountCode") int beginAccountCode, @Param("endAccountCode") int endAccountCode,
            @Param("beginAmount") BigDecimal beginAmount, @Param("endAmount") BigDecimal endAmount,
            @Param("accountType") Boolean accountType, Sort sortField);

    @Query("SELECT dal FROM DocumentAccountLine dal where "
            + "dal.isDeleted=false and dal.account.code between :beginAccountCode and :endAccountCode and "
            + "dal.documentAccount.documentDate between :startDate and :endDate"
            + " and (debitAmount + creditAmount) between :beginAmount and :endAmount"
            + " and ((:accountType IS true and account.literable=true)"
            + " or (:accountType IS false and account.literable=false) or (:accountType IS NULL)) ")
    List<DocumentAccountLine> findAllDocumentAccountLinesInDatesAndAccountCodesAndAmounts(
            @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate,
            @Param("beginAccountCode") int beginAccountCode, @Param("endAccountCode") int endAccountCode,
            @Param("beginAmount") BigDecimal beginAmount, @Param("endAmount") BigDecimal endAmount,
            @Param("accountType") Boolean accountType, Sort sortField);

    @Query("SELECT dal FROM DocumentAccountLine dal where dal.isDeleted=false"
            + " and dal.letter IS NOT NULL and dal.account.code between :beginAccountCode and :endAccountCode and "
            + "dal.documentAccount.documentDate between :startDate and :endDate"
            + " and (debitAmount + creditAmount) >= :beginAmount and ((:accountType IS true and account.literable=true)"
            + " or (:accountType IS false and account.literable=false) or (:accountType IS NULL))")
    List<DocumentAccountLine> findAllDocumentAccountLinesInDatesAndAccountCodesWithoutEndAmountLettering(
            @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate,
            @Param("beginAccountCode") int beginAccountCode, @Param("endAccountCode") int endAccountCode,
            @Param("beginAmount") BigDecimal beginAmount, @Param("accountType") Boolean accountType, Sort sortField);

    @Query("SELECT dal FROM DocumentAccountLine dal where dal.isDeleted=false"
            + " and dal.letter IS NULL and dal.account.code between :beginAccountCode and :endAccountCode and "
            + "dal.documentAccount.documentDate between :startDate and :endDate"
            + " and (debitAmount + creditAmount) >= :beginAmount and ((:accountType IS true and account.literable=true)"
            + " or (:accountType IS false and account.literable=false) or (:accountType IS NULL))")
    List<DocumentAccountLine> findAllDocumentAccountLinesInDatesAndAccountCodesWithoutEndAmountNotLettering(
            @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate,
            @Param("beginAccountCode") int beginAccountCode, @Param("endAccountCode") int endAccountCode,
            @Param("beginAmount") BigDecimal beginAmount, @Param("accountType") Boolean accountType, Sort sortField);

    @Query("SELECT dal FROM DocumentAccountLine dal where dal.isDeleted=false"
            + " and dal.account.code between :beginAccountCode and :endAccountCode and "
            + "dal.documentAccount.documentDate between :startDate and :endDate"
            + " and (debitAmount + creditAmount) >= :beginAmount and ((:accountType IS true and account.literable=true)"
            + " or (:accountType IS false and account.literable=false) or (:accountType IS NULL))")
    List<DocumentAccountLine> findAllDocumentAccountLinesInDatesAndAccountCodesWithoutEndAmount(
            @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate,
            @Param("beginAccountCode") int beginAccountCode, @Param("endAccountCode") int endAccountCode,
            @Param("beginAmount") BigDecimal beginAmount, @Param("accountType") Boolean accountType, Sort sortField);

    @Query("SELECT dal FROM DocumentAccountLine dal where dal.isDeleted=false and dal.account.code between ?1 and ?2"
            + " and dal.documentAccount.documentDate between ?3 and ?4 order by dal.account.code asc,"
            + " CONVERT (varchar(8), dal.documentAccount.documentDate, 112) desc,"
            + " dal.documentAccount.codeDocument desc, dal.id desc")
    List<DocumentAccountLine> findAllDocumentAccountLinesInDatesAndAccountCodes(int beginAccountCode,
            int endAccountCode, LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT NEW fr.sparkit.accounting.dto.DocumentAccountingDto(documentAccount.id, sum(debitAmount),"
            + " sum(creditAmount)) FROM DocumentAccountLine where isDeleted=false and documentAccount.id IN ?1"
            + " group by documentAccount.id")
    List<DocumentAccountingDto> getDocumentAccountingDtoByDocumentIds(List<Long> documentIds);

    @Query("SELECT dal FROM DocumentAccountLine dal "
            + "where dal.isDeleted=false and dal.documentAccount.isDeleted=false and dal.isClose=true and"
            + " dal.documentAccount.fiscalYear.id=?1 and dal.documentAccount.journal.id = ?2")
    List<DocumentAccountLine> findReconcilableLinesUsingJournal(Long currentFiscalYearId, Long journalId);

    @Query(value = "SELECT distinct da.*, ABS(da.DAL_DEBIT_AMOUNT - da.DAL_CREDIT_AMOUNT) FROM T_DOCUMENT_ACCOUNT_LINE da "
            + " left join T_DOCUMENT_ACCOUNT doc on da.DAL_DOCUMENT_ACCOUNT_ID = doc.DA_ID "
            + " left join T_ACCOUNT acc on da.DAL_ACCOUNT_ID = acc.AC_ID "
            + " left join T_DOCUMENT_ACCOUNT_LINE line1 on ((da.DAL_CREDIT_AMOUNT = line1.DAL_DEBIT_AMOUNT and da.DAL_CREDIT_AMOUNT > 0) "
            + " or (line1.DAL_CREDIT_AMOUNT = da.DAL_DEBIT_AMOUNT and da.DAL_DEBIT_AMOUNT > 0)) and line1.DAL_ACCOUNT_ID = acc.AC_ID "
            + " where da.DAL_IS_DELETED=0 and acc.AC_CODE like ?1"
            + " and da.DAL_LETTER IS NULL and doc.DA_DOCUMENT_DATE between ?2 and ?3 order by(ABS(da.DAL_DEBIT_AMOUNT - da.DAL_CREDIT_AMOUNT)) ", countQuery = "SELECT count(distinct(da.DAL_ID)) FROM T_DOCUMENT_ACCOUNT_LINE da "
                    + " left join T_DOCUMENT_ACCOUNT doc on da.DAL_DOCUMENT_ACCOUNT_ID = doc.DA_ID "
                    + " left join T_ACCOUNT acc on da.DAL_ACCOUNT_ID = acc.AC_ID "
                    + " left join T_DOCUMENT_ACCOUNT_LINE line1 on ((da.DAL_CREDIT_AMOUNT = line1.DAL_DEBIT_AMOUNT and da.DAL_CREDIT_AMOUNT > 0) "
                    + " or (line1.DAL_CREDIT_AMOUNT = da.DAL_DEBIT_AMOUNT and da.DAL_DEBIT_AMOUNT > 0)) and line1.DAL_ACCOUNT_ID = acc.AC_ID "
                    + " where da.DAL_IS_DELETED=0 and acc.AC_CODE like ?1"
                    + " and da.DAL_LETTER IS NULL and doc.DA_DOCUMENT_DATE between ?2 and ?3 ", nativeQuery = true)
    Page<DocumentAccountLine> findByAccountCodeAndLetterIsNullAndSameAmount(Integer code, LocalDateTime startDate,
            LocalDateTime endDate, Pageable pageableOfDocumentAccountLines);

    @Query(value = "SELECT distinct da.*, ABS(da.DAL_DEBIT_AMOUNT - da.DAL_CREDIT_AMOUNT) FROM T_DOCUMENT_ACCOUNT_LINE da "
            + " left join T_DOCUMENT_ACCOUNT doc on da.DAL_DOCUMENT_ACCOUNT_ID = doc.DA_ID "
            + " left join T_ACCOUNT acc on da.DAL_ACCOUNT_ID = acc.AC_ID "
            + " left join T_DOCUMENT_ACCOUNT_LINE line1 on ((da.DAL_CREDIT_AMOUNT = line1.DAL_DEBIT_AMOUNT and da.DAL_CREDIT_AMOUNT > 0) "
            + " or (line1.DAL_CREDIT_AMOUNT = da.DAL_DEBIT_AMOUNT and da.DAL_DEBIT_AMOUNT > 0)) and line1.DAL_ACCOUNT_ID = acc.AC_ID "
            + " where da.DAL_IS_DELETED=0 and acc.AC_CODE like ?1"
            + " and da.DAL_LETTER IS NOT NULL and doc.DA_DOCUMENT_DATE between ?2 and ?3 order by(ABS(da.DAL_DEBIT_AMOUNT - da.DAL_CREDIT_AMOUNT))", countQuery = "SELECT count(distinct(da.DAL_ID)) FROM T_DOCUMENT_ACCOUNT_LINE da "
                    + " left join T_DOCUMENT_ACCOUNT doc on da.DAL_DOCUMENT_ACCOUNT_ID = doc.DA_ID "
                    + " left join T_ACCOUNT acc on da.DAL_ACCOUNT_ID = acc.AC_ID "
                    + " left join T_DOCUMENT_ACCOUNT_LINE line1 on ((da.DAL_CREDIT_AMOUNT = line1.DAL_DEBIT_AMOUNT and da.DAL_CREDIT_AMOUNT > 0) "
                    + " or (line1.DAL_CREDIT_AMOUNT = da.DAL_DEBIT_AMOUNT and da.DAL_DEBIT_AMOUNT > 0)) and line1.DAL_ACCOUNT_ID = acc.AC_ID "
                    + " where da.DAL_IS_DELETED=0 and acc.AC_CODE like ?1"
                    + " and da.DAL_LETTER IS NOT NULL and doc.DA_DOCUMENT_DATE between ?2 and ?3 ", nativeQuery = true)
    Page<DocumentAccountLine> findByAccountCodeAndLetterIsNotNullAndSameAmount(Integer code, LocalDateTime startDate,
            LocalDateTime endDate, Pageable pageableOfDocumentAccountLines);

    @Query("SELECT da FROM DocumentAccountLine da"
            + " where da.account.code like ?1 and da.isDeleted=false and da.letter IS NULL and da.reference is not null and da.reference <> ''"
            + " and documentAccount.documentDate between ?2 and ?3 ")
    List<DocumentAccountLine> findByAccountCodeAndReferenceNotEmptyAndLetterIsNull(Integer code,
            LocalDateTime startDate, LocalDateTime endDate);

    @Query(value = "SELECT  da.*  FROM T_DOCUMENT_ACCOUNT_LINE da "
            + " left join T_DOCUMENT_ACCOUNT doc on da.DAL_DOCUMENT_ACCOUNT_ID = doc.DA_ID "
            + " left join T_ACCOUNT acc on da.DAL_ACCOUNT_ID = acc.AC_ID "
            + " where da.DAL_IS_DELETED=0 and acc.AC_CODE like :code"
            + " and da.DAL_LETTER IS NULL and doc.DA_DOCUMENT_DATE between :startDate and :endDate order by  "
            + " CASE WHEN da.DAL_ID in :ids THEN 0 ELSE 1 END asc , da.DAL_REFERENCE desc OFFSET :offset ROWS FETCH NEXT :limit  ROWS ONLY ", nativeQuery = true)
    List<DocumentAccountLine> findByAccountCodeAndLetterIsNullForLettring(@Param("ids") List<Long> ids,
            @Param("code") Integer code,
            @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate,
            @Param("offset") int offset, @Param("limit") int limit);

    @Query("SELECT count(da.id) FROM DocumentAccountLine da" + " where da.account.code like ?1 and da.isDeleted=false"
            + " and da.letter IS NULL and documentAccount.documentDate between ?2 and ?3 ")
    Long findByAccountCodeAndLetterIsNullForLettring(Integer code, LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT distinct id FROM DocumentAccountLine where isDeleted=false" + " and account.id = ?1")
    List<Long> getIdsByDocumentAccountId(Long documentAccountId);

    List<DocumentAccountLine> findByDocumentAccountId(Long documentAccountId);

    List<DocumentAccountLine> findAllByIdIn(List<Long> ids);

    List<DocumentAccountLine> findByDocumentAccountIdIn(List<Long> documentAccountIds);
}
