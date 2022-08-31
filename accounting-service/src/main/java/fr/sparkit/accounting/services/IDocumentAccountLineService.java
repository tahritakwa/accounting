package fr.sparkit.accounting.services;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.sparkit.accounting.dto.AuxiliaryJournalDetailsDto;
import fr.sparkit.accounting.dto.AuxiliaryJournalDto;
import fr.sparkit.accounting.dto.CentralizingJournalDetailsByMonthDto;
import fr.sparkit.accounting.dto.CloseDocumentAccountLineDto;
import fr.sparkit.accounting.dto.DocumentAccountLineDto;
import fr.sparkit.accounting.dto.ReconciliationDocumentAccountLinePageDto;
import fr.sparkit.accounting.entities.DocumentAccount;
import fr.sparkit.accounting.entities.DocumentAccountLine;
import fr.sparkit.accounting.entities.Journal;

public interface IDocumentAccountLineService extends IGenericService<DocumentAccountLine, Long> {

    List<DocumentAccountLine> findByLetterIsNull();

    List<DocumentAccountLine> findByLetterIsNullWithDebit();

    String findLastLetteringCode();

    List<DocumentAccountLine> findByAccountIdAndDebitAmountAndLetterIsNullAndIsDeletedFalse(Long accountId,
            BigDecimal debitAmount);

    List<DocumentAccountLine> findByDocumentAccountId(Long accountId);

    List<DocumentAccountLine> getNotLetteredLines();

    List<DocumentAccountLine> getLetteredLines();

    List<CloseDocumentAccountLineDto> getReconcilableDocumentAccountLineInBetween(Long accountId,
            LocalDateTime startDate, LocalDateTime endDate);

    ReconciliationDocumentAccountLinePageDto getReconcilableDocumentAccountLineInBetween(Long accountId,
            LocalDateTime startDate, LocalDateTime endDate,
            List<CloseDocumentAccountLineDto> documentAccountLineAffected, Pageable pageable);

    List<CloseDocumentAccountLineDto> getCloseDocumentAccountLineInBetween(Long accountId, LocalDate startDate,
            LocalDate endDate);

    List<CloseDocumentAccountLineDto> getAllReconcilableDocumentAccountLineInBetween(Long accountId,
            LocalDateTime startDate, LocalDateTime endDate, List<CloseDocumentAccountLineDto> documentAccountLineAffected);


    List<DocumentAccountLine> getCloseDocumentAccountLineInBetweenDate(Long accountId, LocalDate startDate,
            LocalDate endDate, List<Long> documentAccountLineReleased);

    List<DocumentAccountLine> getCloseDocumentAccountLineInBetweenDate(Long accountId, LocalDate startDate,
            LocalDate endDate, List<Long> documentAccountLineReleased, Pageable pageable);

    void setDocumentAccountLineIsClose(List<Long> ids, LocalDate reconciliationDate);

    List<DocumentAccountLine> findLinesWithNoLetterForBalancedAccountsInFiscalYear(LocalDateTime startDate,
            LocalDateTime endDate);

    List<DocumentAccountLine> findLinesWithNoLetterForRevenueAndExpensesAccountsInFiscalYear(LocalDateTime startDate,
            LocalDateTime endDate);

    void setDocumentaccountLineIsNotClose(List<Long> ids);

    void save(List<DocumentAccountLineDto> documentAccountLines, DocumentAccount documentAccount);

    Optional<BigDecimal> totalDebitAmountDocumentAccount(Long documentAccountId);

    Optional<BigDecimal> totalCreditAmountDocumentAccount(Long documentAccountId);

    List<DocumentAccountLine> getDocumentLineDtosByDocumentIds(List<Long> documentIds);

    boolean isDocumentAccountLineValuesAddedToRow(DocumentAccountLineDto documentAccountLine, Row row,
            List<Field> excelHeaderFields, List<String> acceptedHeaders);

    boolean isLineMonetaryValuesValid(BigDecimal debit, BigDecimal credit);

    Long getTotalReconcilableLinesByDocumentAccountId(Long id);

    Long getCountLetteredLinesByDocumentAccountId(Long id);

    List<CentralizingJournalDetailsByMonthDto> getCentralizingJournalDetailsDto(Long journalId, LocalDateTime startDate,
            LocalDateTime endDate, int breakingAccount, String customerAccountCode, String supplierAccountCode);

    List<CentralizingJournalDetailsByMonthDto> getCentralizingJournalTiersDetailsDto(Long journalId, LocalDateTime startDate,
            LocalDateTime endDate, int breakingAccount, String tierAccountCode);

    List<DocumentAccountLine> findDocumentAccountLineByJournalAndDocumentDateBetween(Long journalId,
            LocalDateTime startDate, LocalDateTime endDate);

    List<AuxiliaryJournalDto> getAuxiliaryJournalDtos(LocalDateTime startDate, LocalDateTime endDate,
            List<Journal> journals);

    Page<AuxiliaryJournalDetailsDto> getByIdAuxiliaryJournalDetailsPage(Long id, LocalDateTime startDate,
            LocalDateTime endDate, Pageable pageable);

    List<AuxiliaryJournalDetailsDto> getByIdAuxiliaryJournalDetails(Long id, LocalDateTime startDate,
            LocalDateTime endDate);

    void addTotalToClosedDocumentAccountLinesList(List<CloseDocumentAccountLineDto> closeDocumentAccountLines);

    boolean isMonetaryValueInCellValid(Cell cell);

    List<DocumentAccountLine> findReconcilableLinesUsingJournal(Long currentFiscalYearId, Long journalId);

    List<DocumentAccountLine> saveDocumentAccountLines(List<DocumentAccountLine> documentAccountLines);
}
