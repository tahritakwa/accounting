package fr.sparkit.accounting.services.utils;

import static fr.sparkit.accounting.constants.AccountingConstants.*;
import static fr.sparkit.accounting.util.CalculationUtil.getFormattedBigDecimalValue;

import java.math.BigDecimal;
import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;

import fr.sparkit.accounting.constants.NumberConstant;
import fr.sparkit.accounting.dto.CentralizingJournalDetailsByMonthDto;
import fr.sparkit.accounting.dto.CentralizingJournalReportLineDto;
import fr.sparkit.accounting.dto.JournalDto;

public final class JournalServiceUtil {

    private JournalServiceUtil() {
        super();
    }

    public static Long calculateEndPage(int listSize, Pageable pageable, Long start) {
        Long end;
        if (start + pageable.getPageSize() > listSize) {
            end = (long) listSize;
        } else {
            end = start + pageable.getPageSize();
        }
        return end;
    }

    public static void addCentralizingJournalReportTotalAmountLine(
            Collection<CentralizingJournalReportLineDto> centralizingJournalReportDtos, BigDecimal totalDebitAmount,
            BigDecimal totalCreditAmount) {
        centralizingJournalReportDtos.add(new CentralizingJournalReportLineDto(EMPTY_STRING, EMPTY_STRING, EMPTY_STRING,
                TraductionServiceUtil.getI18nResourceBundle().getString(TOTAL_PARAM),
                getFormattedBigDecimalValue(totalDebitAmount), getFormattedBigDecimalValue(totalCreditAmount)));
    }

    public static void addCentralizingJournalReportLines(
            Iterable<CentralizingJournalDetailsByMonthDto> centralizingJournalDetailsByMonthDtos,
            Collection<CentralizingJournalReportLineDto> centralizingJournalReportDtos, JournalDto journalDto) {
        centralizingJournalReportDtos.add(new CentralizingJournalReportLineDto(
                AccountingServiceUtil.getConcatinationCodeWithLabel(journalDto.getCode(), journalDto.getLabel()),
                EMPTY_STRING, EMPTY_STRING, EMPTY_STRING, EMPTY_STRING, EMPTY_STRING));
        for (CentralizingJournalDetailsByMonthDto centralizingJournalDetailsByMonthDto : centralizingJournalDetailsByMonthDtos) {
            String totalDebitAmount = StringUtils.EMPTY;
            String totalCreditAmount = StringUtils.EMPTY;
            if (centralizingJournalDetailsByMonthDto.getDebitAmount().compareTo(BigDecimal.ZERO) > NumberConstant.ZERO) {
                totalDebitAmount = getFormattedBigDecimalValue(centralizingJournalDetailsByMonthDto.getDebitAmount());
            }
            if (centralizingJournalDetailsByMonthDto.getCreditAmount().compareTo(BigDecimal.ZERO) > NumberConstant.ZERO) {
                totalCreditAmount = getFormattedBigDecimalValue(centralizingJournalDetailsByMonthDto.getCreditAmount());
            }
            centralizingJournalReportDtos.add(new CentralizingJournalReportLineDto(EMPTY_STRING, EMPTY_STRING,
                    centralizingJournalDetailsByMonthDto.getPlanCode().toString(),
                    centralizingJournalDetailsByMonthDto.getPlanLabel(), totalDebitAmount, totalCreditAmount));
        }
        centralizingJournalReportDtos.add(new CentralizingJournalReportLineDto(EMPTY_STRING, EMPTY_STRING, EMPTY_STRING,
                TraductionServiceUtil.getI18nResourceBundle().getString(SUB_TOTAL_PARAM),
                getFormattedBigDecimalValue(journalDto.getJournalDebitAmount()),
                getFormattedBigDecimalValue(journalDto.getJournalCreditAmount())));
    }

    public static int getDivisionCode(int breakingTierAccount) {
        StringBuilder breakingCode = new StringBuilder().append(NumberConstant.ONE);
        for (int i = 0; i < NumberConstant.EIGHT - breakingTierAccount; i++) {
            breakingCode.append('0');
        }
        return Integer.valueOf(breakingCode.toString());
    }

    public static BigDecimal calculateJournalCreditAmount(
            Collection<CentralizingJournalDetailsByMonthDto> centralizingJournalDetailsByMonthDtos) {
        return centralizingJournalDetailsByMonthDtos.stream().map(CentralizingJournalDetailsByMonthDto::getCreditAmount)
                .filter((BigDecimal debitAmount) -> debitAmount.compareTo(BigDecimal.ZERO) == NumberConstant.ONE)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static BigDecimal calculateJournalDebitAmount(
            Collection<CentralizingJournalDetailsByMonthDto> centralizingJournalDetailsByMonthDtos) {
        return centralizingJournalDetailsByMonthDtos.stream().map(CentralizingJournalDetailsByMonthDto::getDebitAmount)
                .filter((BigDecimal debitAmount) -> debitAmount.compareTo(BigDecimal.ZERO) == NumberConstant.ONE)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}
