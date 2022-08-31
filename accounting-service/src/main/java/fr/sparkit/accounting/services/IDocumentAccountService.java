package fr.sparkit.accounting.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.springframework.data.domain.Pageable;

import fr.sparkit.accounting.dto.AccountDto;
import fr.sparkit.accounting.dto.BillDto;
import fr.sparkit.accounting.dto.DocumentAccountLineDto;
import fr.sparkit.accounting.dto.DocumentAccountingDto;
import fr.sparkit.accounting.dto.DocumentPageDto;
import fr.sparkit.accounting.dto.FileUploadDto;
import fr.sparkit.accounting.dto.Filter;
import fr.sparkit.accounting.dto.RegulationDto;
import fr.sparkit.accounting.entities.DocumentAccount;
import fr.sparkit.accounting.entities.DocumentAccountLine;

public interface IDocumentAccountService extends IGenericService<DocumentAccount, Long> {

    DocumentAccount saveDocumentAccount(DocumentAccountingDto documentAccountingDto,
            boolean isComingFromMovingToNextFiscalYear);

    DocumentAccountingDto getDocumentAccount(Long id);

    boolean existsById(Long documentAccountId);

    boolean deleteDocumentAccount(Long id);

    boolean isValidDocumentAccount(DocumentAccountingDto documentAccountingDto);

    BigDecimal calculateTotalDebitAmountDocument(DocumentAccountingDto documentAccountingDto);

    BigDecimal calculateTotalCreditAmountDocument(DocumentAccountingDto documentAccountingDto);

    boolean isNewDocumentAccount(DocumentAccountingDto documentAccountingDto);

    boolean documentAccountIsEmpty(DocumentAccountingDto documentAccountingDto);

    void saveDocumentAccountLines(List<DocumentAccountLineDto> documentAccountLines, DocumentAccount documentAccount);

    void deleteUnsavedDocumentAccountLines(DocumentAccountingDto documentAccountingDto);

    boolean totalDebitAmountIsEqualToTotalCreditAmount(DocumentAccountingDto documentAccountingDto);

    boolean documentAccountHasValidLines(DocumentAccountingDto documentAccountingDto);

    DocumentAccountLine getPurchasesTaxStampDocumentLine(BillDto billDto, AccountDto purchasesAccountDto);

    DocumentAccountLine getSalesTaxStampDocumentLine(BillDto billDto, AccountDto salesAccountDto);

    List<DocumentAccount> findByJournal(Long journalId, LocalDateTime startDate, LocalDateTime endDate);

    String getCodeDocument(LocalDateTime documentDate);

    AccountDto getCofferDocumentLine(RegulationDto regulationDto);

    List<DocumentAccount> findAllDocumentsInFiscalYear(Long fiscalYearId);

    DocumentAccount findJournalANewDocumentForFiscalYear(Long id);

    void deleteJournalANewDocumentForFiscalYear(Long id);

    byte[] exportDocumentAccountExcelModel();

    FileUploadDto loadDocumentAccountsExcelData(FileUploadDto fileUploadDto);

    boolean isMonetaryValueNegativeOrScaleInvalid(Cell cell, BigDecimal credit);

    boolean isDocumentDateInCellValid(Cell cell);

    byte[] exportDocumentAccountsAsExcelFile();

    boolean hasDocumentAccountLetteredLines(DocumentAccount documentAccount);

    DocumentPageDto filterDocumentAccount(List<Filter> filters, Pageable pageable);

    DocumentAccountingDto generateDocumentAccountFromAmortization(Long fiscalYearId, Long dotationAmortizationAccount,
            Long journalId, Boolean isDetailedGeneration, String contentType, String user, String authorization);

    boolean isDocumentAccountGeneratedFromAmortization(Long fiscalYearId);

    DocumentAccount checkIfCanDelete(Long id);

    boolean deleteDocument(Long id, DocumentAccount documentAccountToBeDeleted);

    public List<DocumentAccountLineDto> getFiltredDALines(List<DocumentAccountLineDto> documentAccountLines);

    }
