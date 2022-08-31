package fr.sparkit.accounting.services.impl;

import static fr.sparkit.accounting.util.CalculationUtil.getFormattedBigDecimalValueOrEmptyStringIfZero;

import java.math.BigDecimal;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import fr.sparkit.accounting.constants.AccountingConstants;
import fr.sparkit.accounting.dto.AccountBalanceDto;
import fr.sparkit.accounting.dto.AnnexeDetailsDto;
import fr.sparkit.accounting.dto.AnnexeReportDto;
import fr.sparkit.accounting.services.IInsertIntoReportLineService;

@Service("accountLines")
public class InsertAccountIntoReportLineService implements IInsertIntoReportLineService {

    @Override
    public void insertIntoReport(AnnexeDetailsDto annexeDetailsDto, List<AnnexeReportDto> annexeBalanceSheetReport) {
        if (!annexeDetailsDto.getAccounts().isEmpty()) {
            for (AccountBalanceDto accountDto : annexeDetailsDto.getAccounts()) {
                String creditAmount = StringUtils.EMPTY;
                String debitAmount = StringUtils.EMPTY;
                if (accountDto.getTotalCurrentCredit().compareTo(BigDecimal.ZERO) != 0) {
                    creditAmount = getFormattedBigDecimalValueOrEmptyStringIfZero(
                            accountDto.getTotalCurrentCredit().abs());
                }
                if (accountDto.getTotalCurrentDebit().compareTo(BigDecimal.ZERO) != 0) {
                    debitAmount = getFormattedBigDecimalValueOrEmptyStringIfZero(
                            accountDto.getTotalCurrentDebit().abs());
                }
                annexeBalanceSheetReport.add(new AnnexeReportDto(AccountingConstants.EMPTY_STRING,
                        AccountingConstants.EMPTY_STRING, accountDto.getAccountCode().toString(),
                        accountDto.getAccountLabel(), debitAmount, creditAmount));
            }
        }
    }

}
