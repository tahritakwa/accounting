package fr.sparkit.accounting.services.impl;

import static fr.sparkit.accounting.constants.AccountingConstants.LOG_ENTITY_CREATED;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import fr.sparkit.accounting.constants.NumberConstant;
import fr.sparkit.accounting.convertor.BankReconciliationStatementConvertor;
import fr.sparkit.accounting.convertor.DocumentAccountingLineConvertor;
import fr.sparkit.accounting.dao.BankReconciliationStatementDao;
import fr.sparkit.accounting.dao.FiscalYearDao;
import fr.sparkit.accounting.dto.BankReconciliationPageDto;
import fr.sparkit.accounting.dto.BankReconciliationStatementDto;
import fr.sparkit.accounting.dto.CloseDocumentAccountLineDto;
import fr.sparkit.accounting.dto.FiscalYearDto;
import fr.sparkit.accounting.entities.Account;
import fr.sparkit.accounting.entities.BankReconciliationStatement;
import fr.sparkit.accounting.entities.DocumentAccountLine;
import fr.sparkit.accounting.entities.FiscalYear;
import fr.sparkit.accounting.enumuration.FiscalYearClosingState;
import fr.sparkit.accounting.services.IAccountService;
import fr.sparkit.accounting.services.IBankReconciliationStatementService;
import fr.sparkit.accounting.services.IDocumentAccountLineService;
import fr.sparkit.accounting.services.IFiscalYearService;
import fr.sparkit.accounting.services.utils.BankReconciliationStatementUtil;
import fr.sparkit.accounting.util.errors.ApiErrors;
import fr.sparkit.accounting.util.errors.ErrorsResponse;
import fr.sparkit.accounting.util.http.HttpCustomException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BankReconciliationStatementService extends GenericService<BankReconciliationStatement, Long>
        implements IBankReconciliationStatementService {

    private final IAccountService accountService;
    private final IDocumentAccountLineService documentAccountLineService;
    private final IFiscalYearService fiscalYearService;
    private final FiscalYearDao fiscalYearDao;
    private final BankReconciliationStatementDao bankReconciliationStatementDao;

    @Autowired
    public BankReconciliationStatementService(IAccountService accountService,
            IDocumentAccountLineService documentAccountLineService, IFiscalYearService fiscalYearService,
            FiscalYearDao fiscalYearDao, BankReconciliationStatementDao bankReconciliationStatementDao) {
        super();
        this.fiscalYearDao = fiscalYearDao;
        this.bankReconciliationStatementDao = bankReconciliationStatementDao;
        this.accountService = accountService;
        this.documentAccountLineService = documentAccountLineService;
        this.fiscalYearService = fiscalYearService;
    }

    @Override
    public BankReconciliationStatement saveOrUpdateBankReconciliationStatement(
            BankReconciliationStatementDto bankReconciliationStatementDto) {
        List<BankReconciliationStatement> nextBankReconciliationStatements = new ArrayList<>();
        FiscalYearDto fiscalYear = fiscalYearService.findById(bankReconciliationStatementDto.getFiscalYearId());
        Optional<FiscalYear> nextfiscalYearOpt = Optional.empty();
        if (FiscalYearClosingState.OPEN.getValue() == fiscalYear.getClosingState()
                || FiscalYearClosingState.PARTIALLY_CLOSED.getValue() == fiscalYear.getClosingState()) {

            Optional<BankReconciliationStatement> currentBankReconciliationStatementOpt = bankReconciliationStatementDao
                    .findByFiscalYearIdAndAccountIdAndCloseMonthAndIsDeletedFalse(
                            bankReconciliationStatementDto.getFiscalYearId(),
                            bankReconciliationStatementDto.getAccountId(),
                            bankReconciliationStatementDto.getCloseMonth());
            BigDecimal finalAmountDifference = BigDecimal.ZERO;
            if (currentBankReconciliationStatementOpt.isPresent()) {
                finalAmountDifference = bankReconciliationStatementDto.getFinalAmount()
                        .subtract(currentBankReconciliationStatementOpt.get().getFinalAmount());
            } else {
                finalAmountDifference = bankReconciliationStatementDto.getFinalAmount()
                        .subtract(bankReconciliationStatementDto.getInitialAmount());
            }
            BankReconciliationStatement savedBankReconciliationStatement = saveBankReconciliationStatement(
                    bankReconciliationStatementDto);
            int closeMonth = bankReconciliationStatementDto.getCloseMonth();
            do {
                Long fiscalYearId = fiscalYear.getId();
                if (nextfiscalYearOpt.isPresent()) {
                    fiscalYearId = nextfiscalYearOpt.get().getId();
                }
                while (closeMonth < NumberConstant.TWELVE) {
                    closeMonth++;
                    Optional<BankReconciliationStatement> bankReconciliationStatementOpt = bankReconciliationStatementDao
                            .findByFiscalYearIdAndAccountIdAndCloseMonthAndIsDeletedFalse(fiscalYearId,
                                    bankReconciliationStatementDto.getAccountId(), closeMonth);
                    if (bankReconciliationStatementOpt.isPresent()) {
                        BankReconciliationStatement nextBankReconciliationStatement = bankReconciliationStatementOpt
                                .get();
                        nextBankReconciliationStatement.setInitialAmount(
                                nextBankReconciliationStatement.getInitialAmount().add(finalAmountDifference));
                        nextBankReconciliationStatement.setFinalAmount(
                                nextBankReconciliationStatement.getFinalAmount().add(finalAmountDifference));
                        nextBankReconciliationStatements.add(nextBankReconciliationStatement);
                        save(nextBankReconciliationStatements);
                    }
                }
                nextfiscalYearOpt = fiscalYearService.findNextFiscalYear(fiscalYearId);
                closeMonth = NumberConstant.ONE;
            } while (nextfiscalYearOpt.isPresent());
            return savedBankReconciliationStatement;
        } else {
            throw new HttpCustomException(ApiErrors.Accounting.FISCAL_YEAR_NOT_OPENED_OPERATION_NOT_ALLOWED,
                    new ErrorsResponse().error(fiscalYear.getName()));
        }

    }

    @Override
    public BankReconciliationStatement saveBankReconciliationStatement(
            BankReconciliationStatementDto bankReconciliationStatementDto) {
        Account account = accountService.findOne(bankReconciliationStatementDto.getAccountId());
        FiscalYear fiscalYear = fiscalYearDao.findOne(bankReconciliationStatementDto.getFiscalYearId());
        List<Long> documentAccountLineToCloseId = bankReconciliationStatementDto.getDocumentAccountLinesAffected()
                .stream().map(CloseDocumentAccountLineDto::getId).collect(Collectors.toList());
        List<Long> documentAccountLineToReleaseId = bankReconciliationStatementDto.getDocumentAccountLinesReleased()
                .stream().map(CloseDocumentAccountLineDto::getId).collect(Collectors.toList());
        if (!documentAccountLineToReleaseId.isEmpty()) {
            documentAccountLineService.setDocumentaccountLineIsNotClose(documentAccountLineToReleaseId);
        }

        if (!documentAccountLineToCloseId.isEmpty()) {
            closeDocumentAccountLine(bankReconciliationStatementDto, documentAccountLineToCloseId, fiscalYear);
        }
        BankReconciliationStatement savedBankReconciliationStatement = saveAndFlush(
                BankReconciliationStatementConvertor.dtoToModel(bankReconciliationStatementDto, account, fiscalYear));
        log.info(LOG_ENTITY_CREATED, savedBankReconciliationStatement);
        return savedBankReconciliationStatement;
    }

    private void closeDocumentAccountLine(BankReconciliationStatementDto bankReconciliationStatementDto,
            List<Long> documentAccountLineToCloseId, FiscalYear fiscalYear) {
        LocalDate date = BankReconciliationStatementUtil.getCloseMonth(fiscalYear.getStartDate(),
                bankReconciliationStatementDto.getCloseMonth());
        LocalDate endDate = date.withDayOfMonth(date.getMonth().length(date.isLeapYear()));
        if (LocalDate.now().isAfter(endDate)) {
            documentAccountLineService.setDocumentAccountLineIsClose(documentAccountLineToCloseId, endDate);
        } else {
            documentAccountLineService.setDocumentAccountLineIsClose(documentAccountLineToCloseId, LocalDate.now());
        }
    }

    @Override
    public Optional<BigDecimal> getFinalAmountReconciliationBank(Long accountId, int closeMonth, Long fiscalYearId) {
        return bankReconciliationStatementDao.getFinalAmount(closeMonth, accountId, fiscalYearId);
    }

    @Override
    public BankReconciliationPageDto getBankReconciliationStatement(Long accountId, Long fiscalYearId,
            Integer closeMonth, List<CloseDocumentAccountLineDto> documentAccountLineReleased, Pageable pageable) {
        Long total = 0L;
        Optional<BankReconciliationStatement> bankReconciliationStatement = bankReconciliationStatementDao
                .findByFiscalYearIdAndAccountIdAndCloseMonthAndIsDeletedFalse(fiscalYearId, accountId, closeMonth);
        if (bankReconciliationStatement.isPresent()) {
            LocalDateTime fiscalYearFirstDate = bankReconciliationStatement.get().getFiscalYear().getStartDate();
            LocalDate date = BankReconciliationStatementUtil.getCloseMonth(fiscalYearFirstDate,
                    bankReconciliationStatement.get().getCloseMonth());
            LocalDate startdate = date.withDayOfMonth(NumberConstant.ONE);
            LocalDate endDate = date.withDayOfMonth(date.getMonth().length(date.isLeapYear()));
            List<CloseDocumentAccountLineDto> closeDocumentAccountLines = new ArrayList<>();
            List<Long> documentAccountLineReleasedId = documentAccountLineReleased.stream()
                    .map(CloseDocumentAccountLineDto::getId).collect(Collectors.toList());
            List<DocumentAccountLine> documentAccountLines = documentAccountLineService
                    .getCloseDocumentAccountLineInBetweenDate(accountId, startdate, endDate,
                            documentAccountLineReleasedId, pageable);

            int beginIndex = Math.toIntExact(pageable.getOffset());
            int endIndex = beginIndex + (Math.toIntExact(pageable.getPageSize())
                    - (documentAccountLineReleased.size() % Math.toIntExact(pageable.getPageSize())));
            if (endIndex > documentAccountLines.size()) {
                endIndex = documentAccountLines.size();
            }
            if ((!documentAccountLines.isEmpty() && beginIndex < endIndex)
                    && (beginIndex < documentAccountLines.size())) {
                closeDocumentAccountLines = DocumentAccountingLineConvertor
                        .documentAccountLinesToCloseDocumentAccountLineDtos(
                                documentAccountLines.subList(beginIndex, endIndex));
            }
            total = (long) documentAccountLines.size();

            BigDecimal totalDebit = documentAccountLines.stream().map(DocumentAccountLine::getDebitAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalCredit = documentAccountLines.stream().map(DocumentAccountLine::getCreditAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BankReconciliationStatementDto bankReconciliationStatementDtos = BankReconciliationStatementConvertor
                    .modelToDto(bankReconciliationStatement.get(), closeDocumentAccountLines);
            return new BankReconciliationPageDto(bankReconciliationStatementDtos, total, totalDebit, totalCredit);
        } else {
            Optional<BigDecimal> finalAmount = Optional.empty();
            Optional<FiscalYear> previousFiscalYearOpt = Optional.empty();
            int initialCloseMonth = closeMonth - NumberConstant.ONE;
            do {
                if (initialCloseMonth >= NumberConstant.ONE) {
                    while (initialCloseMonth >= 1 && !finalAmount.isPresent()) {
                        finalAmount = getFinalAmountReconciliationBank(accountId, initialCloseMonth, fiscalYearId);
                        initialCloseMonth = initialCloseMonth - NumberConstant.ONE;
                    }
                }
                previousFiscalYearOpt = fiscalYearService.findPreviousFiscalYear(fiscalYearId);
                if (previousFiscalYearOpt.isPresent()) {
                    initialCloseMonth = NumberConstant.TWELVE;
                    fiscalYearId = previousFiscalYearOpt.get().getId();
                }
            } while (!finalAmount.isPresent() && previousFiscalYearOpt.isPresent());
            return new BankReconciliationPageDto(new BankReconciliationStatementDto(closeMonth, fiscalYearId,
                    finalAmount.orElse(BigDecimal.ZERO), finalAmount.orElse(BigDecimal.ZERO)), total, BigDecimal.ZERO,
                    BigDecimal.ZERO);
        }
    }

    @Override
    public List<CloseDocumentAccountLineDto> getAllBankReconciliationStatement(Long accountId, Long fiscalYearId,
            Integer closeMonth, List<CloseDocumentAccountLineDto> documentAccountLineReleased) {
        Optional<BankReconciliationStatement> bankReconciliationStatement = bankReconciliationStatementDao
                .findByFiscalYearIdAndAccountIdAndCloseMonthAndIsDeletedFalse(fiscalYearId, accountId, closeMonth);
        List<CloseDocumentAccountLineDto> closeDocumentAccountLines = new ArrayList<>();
        if (bankReconciliationStatement.isPresent()) {
            LocalDateTime fiscalYearFirstDate = bankReconciliationStatement.get().getFiscalYear().getStartDate();
            LocalDate date = BankReconciliationStatementUtil.getCloseMonth(fiscalYearFirstDate,
                    bankReconciliationStatement.get().getCloseMonth());
            LocalDate startdate = date.withDayOfMonth(NumberConstant.ONE);
            LocalDate endDate = date.withDayOfMonth(date.getMonth().length(date.isLeapYear()));
            List<Long> documentAccountLineReleasedId = documentAccountLineReleased.stream()
                    .map(CloseDocumentAccountLineDto::getId).collect(Collectors.toList());
            closeDocumentAccountLines = DocumentAccountingLineConvertor
                    .documentAccountLinesToCloseDocumentAccountLineDtos(
                            documentAccountLineService.getCloseDocumentAccountLineInBetweenDate(accountId, startdate,
                                    endDate, documentAccountLineReleasedId));
        }
        return closeDocumentAccountLines;
    }

    @Override
    public List<CloseDocumentAccountLineDto> generateBankReconciliationReport(Long fiscalYearId, Long accountId,
            int closeMonth) {
        Optional<BankReconciliationStatement> bankReconciliationStatementOpt = bankReconciliationStatementDao
                .findByFiscalYearIdAndAccountIdAndCloseMonthAndIsDeletedFalse(fiscalYearId, accountId, closeMonth);
        if (bankReconciliationStatementOpt.isPresent()) {
            return getReportLines(accountId, bankReconciliationStatementOpt.get());
        } else {
            throw new HttpCustomException(ApiErrors.Accounting.MONTH_NOT_RECONCILED);
        }

    }

    private List<CloseDocumentAccountLineDto> getReportLines(Long accountId,
            BankReconciliationStatement bankReconciliationStatement) {
        List<CloseDocumentAccountLineDto> closeDocumentAccountLines = addClosedLineToReport(accountId,
                bankReconciliationStatement);
        documentAccountLineService.addTotalToClosedDocumentAccountLinesList(closeDocumentAccountLines);
        return closeDocumentAccountLines;
    }

    private List<CloseDocumentAccountLineDto> addClosedLineToReport(Long accountId,
            BankReconciliationStatement bankReconciliationStatement) {
        LocalDate firstDateOfClosedMonth = BankReconciliationStatementUtil.getCloseMonth(
                bankReconciliationStatement.getFiscalYear().getStartDate(),
                bankReconciliationStatement.getCloseMonth());
        return documentAccountLineService.getCloseDocumentAccountLineInBetween(accountId,
                firstDateOfClosedMonth.withDayOfMonth(NumberConstant.ONE), firstDateOfClosedMonth
                        .withDayOfMonth(firstDateOfClosedMonth.getMonth().length(firstDateOfClosedMonth.isLeapYear())));
    }
}
