package fr.sparkit.accounting.services.impl;

import static fr.sparkit.accounting.constants.AccountingConstants.*;
import static fr.sparkit.accounting.util.CalculationUtil.getFormattedBigDecimalValueOrEmptyStringIfZero;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.sparkit.accounting.constants.AccountingConstants;
import fr.sparkit.accounting.dto.AuxiliaryJournalDetailsDto;
import fr.sparkit.accounting.dto.AuxiliaryJournalDto;
import fr.sparkit.accounting.dto.AuxiliaryJournalLineDto;
import fr.sparkit.accounting.dto.StateOfAuxiliaryJournalPage;
import fr.sparkit.accounting.entities.Journal;
import fr.sparkit.accounting.services.IDocumentAccountLineService;
import fr.sparkit.accounting.services.IJournalService;
import fr.sparkit.accounting.services.IStateOfAuxiliaryJournalService;
import fr.sparkit.accounting.services.utils.AccountingServiceUtil;
import fr.sparkit.accounting.services.utils.TraductionServiceUtil;

@Service
@Transactional
public class StateOfAuxiliaryJournalService implements IStateOfAuxiliaryJournalService {

    private final IDocumentAccountLineService documentAccountLineService;
    private final IJournalService journalService;

    @Autowired
    public StateOfAuxiliaryJournalService(IDocumentAccountLineService documentAccountLineService,
            IJournalService journalService) {
        super();
        this.documentAccountLineService = documentAccountLineService;
        this.journalService = journalService;
    }

    @Override
    public StateOfAuxiliaryJournalPage findAuxiliaryJournals(LocalDateTime startDate, LocalDateTime endDate,
            List<Long> journalIds, Pageable pageable) {

        AccountingServiceUtil.checkFilterOnDates(startDate, endDate);

        List<Journal> journals;
        if (journalIds.isEmpty()) {
            journals = journalService.findAll();
        } else {
            journals = journalService.findByIds(journalIds);
        }

        List<AuxiliaryJournalDto> auxiliaryJournalDtos = documentAccountLineService.getAuxiliaryJournalDtos(startDate,
                endDate, journals);

        int beginIndex = Math.toIntExact(pageable.getOffset());
        int endIndex = beginIndex + Math.toIntExact(pageable.getPageSize());

        if (beginIndex >= auxiliaryJournalDtos.size()) {
            beginIndex = 0;
            endIndex = 0;
        } else if (beginIndex + pageable.getPageSize() > auxiliaryJournalDtos.size()) {
            endIndex = auxiliaryJournalDtos.size();
        }

        BigDecimal totalAmountDebit = calculateTotalAmountDebitForAuxiliaryJournalState(auxiliaryJournalDtos);
        BigDecimal totalAmountCredit = calculateTotalAmountCreditForAuxiliaryJournalState(auxiliaryJournalDtos);

        return new StateOfAuxiliaryJournalPage(auxiliaryJournalDtos.subList(beginIndex, endIndex),
                totalAmountDebit.setScale(AccountingConstants.DEFAULT_SCALE_FOR_BIG_DECIMAL, RoundingMode.HALF_UP),
                totalAmountCredit.setScale(AccountingConstants.DEFAULT_SCALE_FOR_BIG_DECIMAL, RoundingMode.HALF_UP),
                auxiliaryJournalDtos.size());
    }

    @Override
    public Page<AuxiliaryJournalDetailsDto> findAuxiliaryJournalDetails(Long id, LocalDateTime startDate,
            LocalDateTime endDate, Pageable pageable) {

        AccountingServiceUtil.checkFilterOnDates(startDate, endDate);
        return documentAccountLineService.getByIdAuxiliaryJournalDetailsPage(id, startDate, endDate, pageable);
    }

    @Override
    public List<AuxiliaryJournalLineDto> generateAuxiliaryJournalsTelerikReport(LocalDateTime startDate,
            LocalDateTime endDate, List<Long> journalIds) {

        List<AuxiliaryJournalLineDto> auxiliaryJournalLineDtos = new ArrayList<>();

        List<Journal> journals;
        if (journalIds.isEmpty()) {
            journals = journalService.findAll();
        } else {
            journals = journalService.findByIds(journalIds);
        }

        List<AuxiliaryJournalDto> auxiliaryJournalDtos = documentAccountLineService.getAuxiliaryJournalDtos(startDate,
                endDate, journals);

        for (AuxiliaryJournalDto auxiliaryJournalDto : auxiliaryJournalDtos) {

            insertJournalNameAndCodeIntoLineIntoReport(auxiliaryJournalDto, auxiliaryJournalLineDtos);

            List<AuxiliaryJournalDetailsDto> listDocumentAccountDetails = documentAccountLineService
                    .getByIdAuxiliaryJournalDetails(auxiliaryJournalDto.getId(), startDate, endDate);

            insertAuxiliaryJournalDetailsIntoAuxiliaryJournalLineDto(auxiliaryJournalLineDtos,
                    listDocumentAccountDetails);
            insertAuxiliaryJournalIntoAuxiliaryJournalLineDto(auxiliaryJournalDto, auxiliaryJournalLineDtos);

        }
        insertTotalDebitAndCreditLineForAuxiliaryJournal(auxiliaryJournalDtos, auxiliaryJournalLineDtos);
        return auxiliaryJournalLineDtos;
    }

    public BigDecimal calculateTotalAmountDebitForAuxiliaryJournalState(
            Collection<AuxiliaryJournalDto> auxiliaryJournalDto) {
        return auxiliaryJournalDto.stream().map(AuxiliaryJournalDto::getTotalDebit).reduce(BigDecimal.ZERO,
                BigDecimal::add);
    }

    public BigDecimal calculateTotalAmountCreditForAuxiliaryJournalState(
            Collection<AuxiliaryJournalDto> auxiliaryJournalDto) {
        return auxiliaryJournalDto.stream().map(AuxiliaryJournalDto::getTotalCredit).reduce(BigDecimal.ZERO,
                BigDecimal::add);
    }

    public void insertAuxiliaryJournalIntoAuxiliaryJournalLineDto(AuxiliaryJournalDto auxiliaryJournalDto,
            Collection<AuxiliaryJournalLineDto> auxiliaryJournalLineDtos) {
        auxiliaryJournalLineDtos.add(
                new AuxiliaryJournalLineDto(TraductionServiceUtil.getI18nResourceBundle().getString(SUB_TOTAL_PARAM),
                        getFormattedBigDecimalValueOrEmptyStringIfZero(auxiliaryJournalDto.getTotalDebit()),
                        getFormattedBigDecimalValueOrEmptyStringIfZero(auxiliaryJournalDto.getTotalCredit())));

    }

    public void insertAuxiliaryJournalDetailsIntoAuxiliaryJournalLineDto(
            Collection<AuxiliaryJournalLineDto> auxiliaryJournalLineDtos,
            Iterable<AuxiliaryJournalDetailsDto> auxiliaryJournalDetailsDtos) {
        for (AuxiliaryJournalDetailsDto auxiliaryJournalDetailsDto : auxiliaryJournalDetailsDtos) {
            auxiliaryJournalLineDtos.add(new AuxiliaryJournalLineDto(
                    auxiliaryJournalDetailsDto.getDate().format(DateTimeFormatter.ofPattern(DD_MM_YYYY)),
                    auxiliaryJournalDetailsDto.getFolio(), auxiliaryJournalDetailsDto.getLabel(),
                    auxiliaryJournalDetailsDto.getDocumentLineDate().format(DateTimeFormatter.ofPattern(DD_MM_YYYY)),
                    Integer.toString(auxiliaryJournalDetailsDto.getAccountCode()),
                    getFormattedBigDecimalValueOrEmptyStringIfZero(auxiliaryJournalDetailsDto.getDebit()),
                    getFormattedBigDecimalValueOrEmptyStringIfZero(auxiliaryJournalDetailsDto.getCredit())));

        }
    }

    public void insertJournalNameAndCodeIntoLineIntoReport(AuxiliaryJournalDto auxiliaryJournalDto,
            Collection<AuxiliaryJournalLineDto> auxiliaryJournalLineDtos) {
        auxiliaryJournalLineDtos.add(new AuxiliaryJournalLineDto(AccountingServiceUtil
                .getConcatinationCodeWithLabel(auxiliaryJournalDto.getCode(), auxiliaryJournalDto.getLabel())));
    }

    public void insertTotalDebitAndCreditLineForAuxiliaryJournal(List<AuxiliaryJournalDto> auxiliaryJournalDtos,
            Collection<AuxiliaryJournalLineDto> auxiliaryJournalLineDtos) {
        BigDecimal totalAmountDebit = calculateTotalAmountDebitForAuxiliaryJournalState(auxiliaryJournalDtos);
        BigDecimal totalAmountCredit = calculateTotalAmountCreditForAuxiliaryJournalState(auxiliaryJournalDtos);
        auxiliaryJournalLineDtos
                .add(new AuxiliaryJournalLineDto(TraductionServiceUtil.getI18nResourceBundle().getString(TOTAL_PARAM),
                        getFormattedBigDecimalValueOrEmptyStringIfZero(totalAmountDebit),
                        getFormattedBigDecimalValueOrEmptyStringIfZero(totalAmountCredit)));
    }

}
