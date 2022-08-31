package fr.sparkit.accounting.services.impl;

import static fr.sparkit.accounting.util.CalculationUtil.getFormattedBigDecimalValueOrEmptyStringIfZero;

import java.math.BigDecimal;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import static fr.sparkit.accounting.constants.AccountingConstants.*;
import fr.sparkit.accounting.dto.AnnexeDetailsDto;
import fr.sparkit.accounting.dto.AnnexeReportDto;
import fr.sparkit.accounting.services.IInsertIntoReportLineService;
import fr.sparkit.accounting.services.utils.TraductionServiceUtil;

@Service("totalLine")
public class InsertTotalIntoReportLineService implements IInsertIntoReportLineService {

    @Override
    public void insertIntoReport(AnnexeDetailsDto annexeDetailsDto, List<AnnexeReportDto> annexeBalanceSheetReport) {
        String totalDebitValue = StringUtils.EMPTY;
        String totalCreditValue = StringUtils.EMPTY;
        if (annexeDetailsDto.getTotalAmount().compareTo(BigDecimal.ZERO) < 0) {
            totalDebitValue = getFormattedBigDecimalValueOrEmptyStringIfZero(annexeDetailsDto.getTotalAmount().abs());
        } else if (annexeDetailsDto.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {

            totalCreditValue = getFormattedBigDecimalValueOrEmptyStringIfZero(annexeDetailsDto.getTotalAmount().abs());
        }
        AnnexeReportDto annexeReportDto = new AnnexeReportDto(EMPTY_STRING, EMPTY_STRING, EMPTY_STRING,
                TraductionServiceUtil.getI18nResourceBundle().getString(TOTAL_BALANCE_PARAM), totalDebitValue,
                totalCreditValue);
        annexeBalanceSheetReport.add(annexeReportDto);
    }

}
