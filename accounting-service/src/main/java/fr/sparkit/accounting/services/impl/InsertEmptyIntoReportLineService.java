package fr.sparkit.accounting.services.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import fr.sparkit.accounting.constants.AccountingConstants;
import fr.sparkit.accounting.dto.AnnexeDetailsDto;
import fr.sparkit.accounting.dto.AnnexeReportDto;
import fr.sparkit.accounting.services.IInsertIntoReportLineService;

@Service("emptyLine")
public class InsertEmptyIntoReportLineService implements IInsertIntoReportLineService {

    @Override
    public void insertIntoReport(AnnexeDetailsDto annexeDetailsDto, List<AnnexeReportDto> annexeBalanceSheetReport) {
        annexeBalanceSheetReport.add(new AnnexeReportDto(AccountingConstants.EMPTY_STRING,
                AccountingConstants.EMPTY_STRING, AccountingConstants.EMPTY_STRING, AccountingConstants.EMPTY_STRING,
                AccountingConstants.EMPTY_STRING, AccountingConstants.EMPTY_STRING));
    }

}
