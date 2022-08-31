package fr.sparkit.accounting.services.impl;

import static fr.sparkit.accounting.constants.AccountingConstants.CURRENT_FISCAL_YEAR_DOES_NOT_EXIST;
import static fr.sparkit.accounting.constants.AccountingConstants.FISCAL_YEAR_PREVIOUS_FISCAL_YEAR_HAS_NO_NON_LETTERED_DOCUMENT_ACCOUNT_LINES;
import static fr.sparkit.accounting.constants.AccountingConstants.JOURNAL_A_NEW_LINE_LABEL;
import static fr.sparkit.accounting.constants.AccountingConstants.JOURNAL_NEW_DOES_NOT_EXIST;
import static fr.sparkit.accounting.constants.AccountingConstants.LOG_ENTITY_CREATED;
import static fr.sparkit.accounting.constants.AccountingConstants.LOG_ENTITY_UPDATED;
import static fr.sparkit.accounting.constants.AccountingConstants.PASSING_INTO_FISCAL_YEARS;
import static fr.sparkit.accounting.constants.AccountingConstants.RESULT_ACCOUNT_DOES_NOT_EXIST;
import static fr.sparkit.accounting.constants.AccountingConstants.TARGET_FISCAL_YEAR_DOES_NOT_EXIST;
import static fr.sparkit.accounting.constants.AccountingConstants.TARGET_FISCAL_YEAR_NOT_AFTER_SELECTED_FISCAL_YEAR;
import static fr.sparkit.accounting.constants.AccountingConstants.TARGET_FISCAL_YEAR_NOT_OPEN;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import fr.sparkit.accounting.convertor.AccountConvertor;
import fr.sparkit.accounting.convertor.AmortizationTableConvertor;
import fr.sparkit.accounting.convertor.FiscalYearConvertor;
import fr.sparkit.accounting.convertor.JournalConverter;
import fr.sparkit.accounting.dto.AccountingConfigurationDto;
import fr.sparkit.accounting.dto.AmortizationTableDto;
import fr.sparkit.accounting.dto.CloseAndReopeningFiscalYearDto;
import fr.sparkit.accounting.dto.DepreciationAssetsDto;
import fr.sparkit.accounting.dto.FiscalYearDto;
import fr.sparkit.accounting.entities.Account;
import fr.sparkit.accounting.entities.AmortizationTable;
import fr.sparkit.accounting.entities.DepreciationAssets;
import fr.sparkit.accounting.entities.DocumentAccount;
import fr.sparkit.accounting.entities.DocumentAccountLine;
import fr.sparkit.accounting.entities.FiscalYear;
import fr.sparkit.accounting.enumuration.DocumentAccountStatus;
import fr.sparkit.accounting.enumuration.FiscalYearClosingState;
import fr.sparkit.accounting.services.IAccountingConfigurationService;
import fr.sparkit.accounting.services.IDocumentAccountLineService;
import fr.sparkit.accounting.services.IDocumentAccountService;
import fr.sparkit.accounting.services.IFiscalYearClosingAndReOpeningService;
import fr.sparkit.accounting.services.IFiscalYearService;
import fr.sparkit.accounting.util.errors.ApiErrors;
import fr.sparkit.accounting.util.http.HttpCustomException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FiscalYearClosingAndReOpeningService implements IFiscalYearClosingAndReOpeningService {

    private final AccountService accountService;
    private final JournalService journalService;
    private final IAccountingConfigurationService accountingConfigurationService;
    private final IFiscalYearService fiscalYearService;
    private final IDocumentAccountService documentAccountService;
    private final IDocumentAccountLineService documentAccountLineService;
    private final DepreciationAssetService depreciationAssetService;
    private final AmortizationTableService amortizationTableService;

    private static final String TOTAL_SUM_OF_CREDITS = "totalSumOfCredits";
    private static final String TOTAL_SUM_OF_DEBITS = "totalSumOfDebits";

    @Autowired
    public FiscalYearClosingAndReOpeningService(@Lazy IDocumentAccountService documentAccountService,
            AccountService accountService, JournalService journalService,
            @Lazy IAccountingConfigurationService accountingConfigurationService,
            @Lazy IFiscalYearService fiscalYearService, @Lazy DepreciationAssetService depreciationAssetService,
            @Lazy IDocumentAccountLineService documentAccountLineService,
            @Lazy AmortizationTableService amortizationTableService) {
        super();
        this.fiscalYearService = fiscalYearService;
        this.documentAccountService = documentAccountService;
        this.accountService = accountService;
        this.journalService = journalService;
        this.accountingConfigurationService = accountingConfigurationService;
        this.depreciationAssetService = depreciationAssetService;
        this.documentAccountLineService = documentAccountLineService;
        this.amortizationTableService = amortizationTableService;
    }

    @Override
    @CacheEvict(value = "FiscalYearCache", allEntries = true)
    public void closeAndReOpenFiscalYear(String contentType, String user, String authorization,
            CloseAndReopeningFiscalYearDto closeAndReopeningFiscalYearDto) {
        checkCloseAndReopeningFiscalYearValues(closeAndReopeningFiscalYearDto);
        FiscalYear targetFiscalYear = fiscalYearService.findOne(closeAndReopeningFiscalYearDto.getTargetFiscalYearId());
        if (targetFiscalYear.getClosingState() != FiscalYearClosingState.OPEN.getValue()) {
            log.error(TARGET_FISCAL_YEAR_NOT_OPEN);
            throw new HttpCustomException(ApiErrors.Accounting.TARGET_FISCAL_YEAR_IS_CLOSED);
        }
        FiscalYear currentFiscalYear = fiscalYearService.findOne(closeAndReopeningFiscalYearDto.getCurrentFiscalYear());
        fiscalYearService.checkPreviousFiscalYearsAreConcluded(currentFiscalYear);

        List<FiscalYear> fiscalYearsAfterCurrentFiscalYear = fiscalYearService
                .findAllFiscalYearsAfterDate(currentFiscalYear.getStartDate());
        fiscalYearsAfterCurrentFiscalYear.sort(Comparator.comparing(FiscalYear::getStartDate));
        if (!fiscalYearsAfterCurrentFiscalYear.contains(targetFiscalYear)) {
            log.error(TARGET_FISCAL_YEAR_NOT_AFTER_SELECTED_FISCAL_YEAR);
            throw new HttpCustomException(ApiErrors.Accounting.TARGET_FISCAL_YEAR_NOT_AFTER_SELECTED_FISCAL_YEAR);
        }
        for (FiscalYear fiscalYear : fiscalYearsAfterCurrentFiscalYear) {
            log.info(PASSING_INTO_FISCAL_YEARS, currentFiscalYear.getName(), fiscalYear.getName());
            closeAndReopeningFiscalYearDto.setCurrentFiscalYear(currentFiscalYear.getId());
            closeAndReopeningFiscalYearDto.setTargetFiscalYearId(fiscalYear.getId());
            performClosing(contentType, user, authorization, closeAndReopeningFiscalYearDto, fiscalYear,
                    currentFiscalYear);
            if (fiscalYear.equals(targetFiscalYear)) {
                break;
            }
            currentFiscalYear = fiscalYear;
        }
    }

    private void performClosing(String contentType, String user, String authorization,
            CloseAndReopeningFiscalYearDto closeAndReopeningFiscalYearDto, FiscalYear targetFiscalYear,
            FiscalYear currentFiscalYear) {
        transferDepreciation(currentFiscalYear, contentType, user, authorization,
                closeAndReopeningFiscalYearDto);

        if (closeAndReopeningFiscalYearDto.isTransferOfReports()) {
            fiscalYearService.generateAllAccountingReports(currentFiscalYear.getId(), user);
        }

        AccountingConfigurationDto accountingConfiguration = accountingConfigurationService.findLastConfig();

        Account resultAccount = accountService.findOne(closeAndReopeningFiscalYearDto.getResultAccount());
        if (closeAndReopeningFiscalYearDto.isPassEntryAccounting()) {
            passEntryAccounting(currentFiscalYear, targetFiscalYear, resultAccount,
                    closeAndReopeningFiscalYearDto.isLiterableAccounts(), accountingConfiguration);
        }

        currentFiscalYear.setClosingDate(currentFiscalYear.getEndDate());
        currentFiscalYear.setClosingState(FiscalYearClosingState.CONCLUDED.getValue());
        currentFiscalYear.setConclusionDate(LocalDateTime.now());
        fiscalYearService.saveAndFlush(currentFiscalYear);
        log.info(LOG_ENTITY_UPDATED, currentFiscalYear);
        setCurrentFiscal(targetFiscalYear);
    }

    private void checkCloseAndReopeningFiscalYearValues(CloseAndReopeningFiscalYearDto closeAndReopeningFiscalYearDto) {
        if (!fiscalYearService.existsById(closeAndReopeningFiscalYearDto.getTargetFiscalYearId())) {
            log.error(TARGET_FISCAL_YEAR_DOES_NOT_EXIST);
            throw new HttpCustomException(ApiErrors.Accounting.TARGET_FISCAL_YEAR_NON_EXISTENT);
        }
        if (!fiscalYearService.existsById(closeAndReopeningFiscalYearDto.getCurrentFiscalYear())) {
            log.error(CURRENT_FISCAL_YEAR_DOES_NOT_EXIST);
            throw new HttpCustomException(ApiErrors.Accounting.CURRENT_FISCAL_YEAR_NON_EXISTENT);
        }
        if (!accountService.existsById(closeAndReopeningFiscalYearDto.getResultAccount())) {
            log.error(RESULT_ACCOUNT_DOES_NOT_EXIST);
            throw new HttpCustomException(ApiErrors.Accounting.RESULT_ACCOUNT_NON_EXISTENT);
        }
        if (!journalService.existsById(closeAndReopeningFiscalYearDto.getJournalANewId())) {
            log.error(JOURNAL_NEW_DOES_NOT_EXIST);
            throw new HttpCustomException(ApiErrors.Accounting.JOURNAL_NO_JOURNAL_A_NEW);
        }
    }

    private void passEntryAccounting(FiscalYear currentFiscalYear, FiscalYear targetFiscalYear, Account resultAccount,
            boolean isLiterableAccounts, AccountingConfigurationDto accountingConfiguration) {
        discardRevenueAndExpenseAccounts(currentFiscalYear, resultAccount);
        stopJournalEntries(currentFiscalYear, accountingConfiguration);
        reopeningAccountsFromPreviousFiscal(currentFiscalYear, targetFiscalYear, isLiterableAccounts, resultAccount,
                accountingConfiguration);
    }

    private void transferDepreciation(FiscalYear currentFiscalYear, String contentType, String user,
            String authorization, CloseAndReopeningFiscalYearDto closeAndReopeningFiscalYear) {
        if (closeAndReopeningFiscalYear.isTransferOfDepreciationPeriod()) {
            transferOfDepreciationPeriod(currentFiscalYear, contentType, user, authorization);
        }
    }

    @Override
    public void discardRevenueAndExpenseAccounts(FiscalYear currentFiscalYear, Account resultAccount) {
        BigDecimal totalSumOfRevenue = BigDecimal.ZERO;
        BigDecimal totalSumOfExpenses = BigDecimal.ZERO;

        List<DocumentAccountLine> documentAccountLinesForRevenueAndExpensesAccounts = documentAccountLineService
                .findLinesWithNoLetterForRevenueAndExpensesAccountsInFiscalYear(currentFiscalYear.getStartDate(),
                        currentFiscalYear.getEndDate());

        Map<Account, List<DocumentAccountLine>> documentAccountLinesByAccount = documentAccountLinesForRevenueAndExpensesAccounts
                .stream().collect(Collectors.groupingBy(DocumentAccountLine::getAccount));
        for (Map.Entry<Account, List<DocumentAccountLine>> entry : documentAccountLinesByAccount.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                BigDecimal sumCredit = entry.getValue().stream().map(DocumentAccountLine::getCreditAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal sumDebit = entry.getValue().stream().map(DocumentAccountLine::getDebitAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                totalSumOfRevenue = totalSumOfRevenue.add(sumCredit);
                totalSumOfExpenses = totalSumOfExpenses.add(sumDebit);

                if (sumCredit.compareTo(sumDebit) > 0) {
                    entry.getKey().setDebitOpening(sumCredit.subtract(sumDebit));
                } else {
                    entry.getKey().setCreditOpening(sumDebit.subtract(sumCredit));
                }
                accountService.updateAccount(AccountConvertor.modelToDto(entry.getKey()));
            }
        }
        if (totalSumOfExpenses.compareTo(totalSumOfRevenue) > 0) {
            resultAccount.setCreditOpening(
                    resultAccount.getCreditOpening().add(totalSumOfExpenses.subtract(totalSumOfRevenue)));
        } else {
            resultAccount.setDebitOpening(
                    resultAccount.getDebitOpening().add(totalSumOfRevenue.subtract(totalSumOfExpenses)));
        }
        accountService.updateAccount(AccountConvertor.modelToDto(resultAccount));
    }

    @Override
    public void stopJournalEntries(FiscalYear currentFiscalYear, AccountingConfigurationDto accountingConfiguration) {
        List<DocumentAccountLine> documentAccountLinesForBalanceSheetAccounts = documentAccountLineService
                .findLinesWithNoLetterForBalancedAccountsInFiscalYear(currentFiscalYear.getStartDate(),
                        currentFiscalYear.getEndDate());
        Map<Account, List<DocumentAccountLine>> documentAccountLinesByAccount = documentAccountLinesForBalanceSheetAccounts
                .stream().collect(Collectors.groupingBy(DocumentAccountLine::getAccount));
        for (Map.Entry<Account, List<DocumentAccountLine>> entry : documentAccountLinesByAccount.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                BigDecimal sumCredit = entry.getValue().stream().map(DocumentAccountLine::getCreditAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal sumDebit = entry.getValue().stream().map(DocumentAccountLine::getDebitAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                if (sumCredit.compareTo(sumDebit) > 0) {
                    entry.getKey().setDebitOpening(entry.getKey().getDebitOpening().add(sumCredit.subtract(sumDebit)));
                } else {
                    entry.getKey()
                            .setCreditOpening(entry.getKey().getCreditOpening().add(sumDebit.subtract(sumCredit)));
                }
                accountService.updateAccount(AccountConvertor.modelToDto(entry.getKey()));
            }
        }
    }

    @Override
    public void reopeningAccountsFromPreviousFiscal(FiscalYear currentFiscalYear, FiscalYear targetFiscalYear,
            boolean literableAccounts, Account resultAccount, AccountingConfigurationDto configuration) {

        setCurrentFiscal(targetFiscalYear);
        DocumentAccount journalANewDocument = setUpJournalANewDocument(targetFiscalYear, configuration);
        List<DocumentAccountLine> documentAccountLinesToBeAddedInJournalANew = new ArrayList<>();
        Map<String, BigDecimal> journalANewTotals = setJournalANewLines(currentFiscalYear, targetFiscalYear,
                literableAccounts, documentAccountLinesToBeAddedInJournalANew, configuration, journalANewDocument);
        if (journalANewTotals.get(TOTAL_SUM_OF_CREDITS).compareTo(journalANewTotals.get(TOTAL_SUM_OF_DEBITS)) > 0) {
            DocumentAccountLine line = setUpDefaultLineForJournalANew(journalANewDocument, resultAccount,
                    targetFiscalYear, BigDecimal.ZERO,
                    journalANewTotals.get(TOTAL_SUM_OF_CREDITS).subtract(journalANewTotals.get(TOTAL_SUM_OF_DEBITS)));
            documentAccountLinesToBeAddedInJournalANew.add(line);
        } else if (journalANewTotals.get(TOTAL_SUM_OF_CREDITS)
                .compareTo(journalANewTotals.get(TOTAL_SUM_OF_DEBITS)) < 0) {
            DocumentAccountLine line = setUpDefaultLineForJournalANew(journalANewDocument, resultAccount,
                    targetFiscalYear,
                    journalANewTotals.get(TOTAL_SUM_OF_DEBITS).subtract(journalANewTotals.get(TOTAL_SUM_OF_CREDITS)),
                    BigDecimal.ZERO);
            documentAccountLinesToBeAddedInJournalANew.add(line);
        }
        journalANewDocument
                .setIndexOfStatus(DocumentAccountStatus.BY_CONCLUDING_CURRENT_FISCAL_YEAR_IS_CREATED.getIndex());
        documentAccountService.deleteJournalANewDocumentForFiscalYear(targetFiscalYear.getId());
        journalANewDocument
                .setCodeDocument(documentAccountService.getCodeDocument(journalANewDocument.getDocumentDate()));
        journalANewDocument.setId(documentAccountService.saveAndFlush(journalANewDocument).getId());
        log.info(LOG_ENTITY_CREATED, journalANewDocument);
        documentAccountLinesToBeAddedInJournalANew
                .sort(Comparator.comparing((DocumentAccountLine line) -> line.getAccount().getCode()).reversed());
        documentAccountLineService.save(documentAccountLinesToBeAddedInJournalANew);
        setCurrentFiscal(currentFiscalYear);
    }

    private Map<String, BigDecimal> setJournalANewLines(FiscalYear currentFiscalYear, FiscalYear targetFiscalYear,
            boolean literableAccounts, List<DocumentAccountLine> documentAccountLinesToBeAddedInJournalANew,
            AccountingConfigurationDto configuration, DocumentAccount journalANewDocument) {
        Map<String, BigDecimal> totals = new HashMap<>();
        totals.put(TOTAL_SUM_OF_CREDITS, BigDecimal.ZERO);
        totals.put(TOTAL_SUM_OF_DEBITS, BigDecimal.ZERO);
        List<DocumentAccountLine> documentAccountLinesForBalanceSheetAccounts = documentAccountLineService
                .findLinesWithNoLetterForBalancedAccountsInFiscalYear(currentFiscalYear.getStartDate(),
                        currentFiscalYear.getEndDate());
        Map<Account, List<DocumentAccountLine>> documentAccountLinesByAccount = documentAccountLinesForBalanceSheetAccounts
                .stream().collect(Collectors.groupingBy(DocumentAccountLine::getAccount));
        for (Map.Entry<Account, List<DocumentAccountLine>> entry : documentAccountLinesByAccount.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                if (literableAccounts) {
                    setUpJournalANewLinesForLetterableAccount(targetFiscalYear,
                            documentAccountLinesToBeAddedInJournalANew, totals, entry.getValue(), journalANewDocument);
                } else {
                    setUpJournalANewLineForNonLetterableAccount(targetFiscalYear,
                            documentAccountLinesToBeAddedInJournalANew, totals, entry.getKey(), entry.getValue(),
                            journalANewDocument);
                }
            }
        }
        if (documentAccountLinesToBeAddedInJournalANew.isEmpty()) {
            accountingConfigurationService.updateCurrentFiscalYear(currentFiscalYear.getId());
            log.error(FISCAL_YEAR_PREVIOUS_FISCAL_YEAR_HAS_NO_NON_LETTERED_DOCUMENT_ACCOUNT_LINES);
            throw new HttpCustomException(
                    ApiErrors.Accounting.FISCAL_YEAR_PREVIOUS_FISCAL_YEAR_HAS_NO_NON_LETTERED_DOCUMENT_ACCOUNT_LINES);
        }
        return totals;
    }

    public void setUpJournalANewLinesForLetterableAccount(FiscalYear targetFiscalYear,
            Collection<DocumentAccountLine> documentAccountLinesToBeAddedInJournalANew, Map<String, BigDecimal> totals,
            Iterable<DocumentAccountLine> accountLines, DocumentAccount journalANewDocument) {
        for (DocumentAccountLine documentAccountLine : accountLines) {
            totals.merge(TOTAL_SUM_OF_CREDITS, documentAccountLine.getCreditAmount(), BigDecimal::add);
            totals.merge(TOTAL_SUM_OF_DEBITS, documentAccountLine.getDebitAmount(), BigDecimal::add);
            documentAccountLine.setDocumentLineDate(targetFiscalYear.getStartDate().plusSeconds(1L));
            documentAccountLine.setId(null);
            documentAccountLine.setDocumentAccount(journalANewDocument);
            documentAccountLinesToBeAddedInJournalANew.add(documentAccountLine);
        }
    }

    private void setUpJournalANewLineForNonLetterableAccount(FiscalYear targetFiscalYear,
            List<DocumentAccountLine> documentAccountLinesToBeAddedInJournalANew, Map<String, BigDecimal> totals,
            Account account, List<DocumentAccountLine> accountLines, DocumentAccount journalANewDocument) {
        BigDecimal sumCredit = accountLines.stream().map(DocumentAccountLine::getCreditAmount).reduce(BigDecimal.ZERO,
                BigDecimal::add);
        BigDecimal sumDebit = accountLines.stream().map(DocumentAccountLine::getDebitAmount).reduce(BigDecimal.ZERO,
                BigDecimal::add);
        BigDecimal amount = sumCredit.subtract(sumDebit);
        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            totals.merge(TOTAL_SUM_OF_CREDITS, amount, BigDecimal::add);
            DocumentAccountLine documentAccountLine = setUpDefaultLineForJournalANew(journalANewDocument, account,
                    targetFiscalYear, amount, BigDecimal.ZERO);
            documentAccountLinesToBeAddedInJournalANew.add(documentAccountLine);
        } else if (amount.compareTo(BigDecimal.ZERO) < 0) {
            totals.merge(TOTAL_SUM_OF_DEBITS, amount.abs(), BigDecimal::add);
            DocumentAccountLine documentAccountLine = setUpDefaultLineForJournalANew(journalANewDocument, account,
                    targetFiscalYear, BigDecimal.ZERO, amount.abs());
            documentAccountLinesToBeAddedInJournalANew.add(documentAccountLine);
        } else {
            totals.merge(TOTAL_SUM_OF_CREDITS, sumCredit, BigDecimal::add);
            DocumentAccountLine documentAccountLine1 = setUpDefaultLineForJournalANew(journalANewDocument, account,
                    targetFiscalYear, sumCredit, BigDecimal.ZERO);
            documentAccountLinesToBeAddedInJournalANew.add(documentAccountLine1);

            totals.merge(TOTAL_SUM_OF_DEBITS, sumDebit, BigDecimal::add);
            DocumentAccountLine documentAccountLine2 = setUpDefaultLineForJournalANew(journalANewDocument, account,
                    targetFiscalYear, BigDecimal.ZERO, sumDebit);

            documentAccountLinesToBeAddedInJournalANew.add(documentAccountLine2);
        }
    }

    private DocumentAccount setUpJournalANewDocument(FiscalYear targetFiscalYear,
            AccountingConfigurationDto configuration) {
        DocumentAccount journalANewDocument = new DocumentAccount();
        journalANewDocument.setDocumentDate(targetFiscalYear.getStartDate().plusSeconds(1L));
        journalANewDocument.setCreationDocumentDate(LocalDateTime.now());
        journalANewDocument
                .setJournal(JournalConverter.dtoToModel(journalService.findById(configuration.getJournalANewId())));
        journalANewDocument.setFiscalYear(targetFiscalYear);
        journalANewDocument.setLabel("J.AN " + targetFiscalYear.getName());
        journalANewDocument
                .setIndexOfStatus(DocumentAccountStatus.BY_CONCLUDING_CURRENT_FISCAL_YEAR_IS_CREATED.getIndex());
        return journalANewDocument;
    }

    private void setCurrentFiscal(FiscalYear currentFiscalYear) {
        accountingConfigurationService.updateCurrentFiscalYear(currentFiscalYear.getId());
    }

    public DocumentAccountLine setUpDefaultLineForJournalANew(DocumentAccount journalANewDocument, Account account,
            FiscalYear targetFiscalYear, BigDecimal creditAmount, BigDecimal debitAmount) {
        DocumentAccountLine documentAccountLine = new DocumentAccountLine();
        documentAccountLine.setAccount(account);
        documentAccountLine.setDocumentAccount(journalANewDocument);
        documentAccountLine.setClose(false);
        documentAccountLine
                .setLabel(String.format(JOURNAL_A_NEW_LINE_LABEL, targetFiscalYear.getName(), account.getCode()));
        documentAccountLine.setReference("J.AN " + targetFiscalYear.getName());
        documentAccountLine.setCreditAmount(creditAmount);
        documentAccountLine.setDebitAmount(debitAmount);
        documentAccountLine.setDocumentLineDate(targetFiscalYear.getStartDate().plusSeconds(1L));
        return documentAccountLine;
    }

    @Override
    public void transferOfDepreciationPeriod(FiscalYear currentFiscalYear, String contentType,
            String user, String authorization) {
        List<DepreciationAssetsDto> depreciationAssetsDtos = depreciationAssetService.getAllDepreciations(user,
                contentType, authorization);
        FiscalYearDto fiscalYear = FiscalYearConvertor.modelToDto(currentFiscalYear);

        List<DepreciationAssetsDto> depricationAssetsList = depreciationAssetService
                .filtredImmobilization(depreciationAssetsDtos, fiscalYear);

        Map<Long, List<DepreciationAssetsDto>> depricationAssetsMap = depricationAssetsList.stream()
                .collect(Collectors.groupingBy(DepreciationAssetsDto::getIdImmobilizationAccount));

        for (Map.Entry<Long, List<DepreciationAssetsDto>> depricationAssets : depricationAssetsMap.entrySet()) {
            for (DepreciationAssetsDto depricationAssetDto : depricationAssets.getValue()) {
                AmortizationTableDto amortizationTableDto = amortizationTableService
                        .getDepreciationOfAsset(depricationAssetDto, fiscalYear, depricationAssetDto.getIdCategory());
                DepreciationAssets depreciationAssets = depreciationAssetService
                        .findByIdAssets(amortizationTableDto.getIdAssets());
                AmortizationTable amortizationTable = AmortizationTableConvertor.dtoToModel(amortizationTableDto,
                        depreciationAssets, FiscalYearConvertor.dtoToModel(fiscalYear));
                Optional<AmortizationTable> oldAmortization = amortizationTableService
                        .findAmortizationTable(fiscalYear.getId(), depreciationAssets.getId());
                if (oldAmortization.isPresent()) {
                    oldAmortization.get().setPreviousDepreciation(amortizationTableDto.getPreviousDepreciation());
                    oldAmortization.get().setAcquisitionValue(amortizationTableDto.getAcquisitionValue());
                    oldAmortization.get().setAnnuityExercise(amortizationTableDto.getAnnuityExercise());
                    oldAmortization.get().setVcn(amortizationTableDto.getVcn());
                    amortizationTableService.saveAndFlush(oldAmortization.get());
                    log.info(LOG_ENTITY_UPDATED, oldAmortization.get().toString());
                } else {
                    amortizationTable = amortizationTableService.saveAndFlush(amortizationTable);
                    log.info(LOG_ENTITY_CREATED, amortizationTable.toString());
                }

            }
        }
    }
}
