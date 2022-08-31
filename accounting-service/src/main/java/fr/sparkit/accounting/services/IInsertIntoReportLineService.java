package fr.sparkit.accounting.services;

import java.util.List;

import fr.sparkit.accounting.dto.AnnexeDetailsDto;
import fr.sparkit.accounting.dto.AnnexeReportDto;

public interface IInsertIntoReportLineService {

    void insertIntoReport(AnnexeDetailsDto annexeDetailsDto, List<AnnexeReportDto> annexeBalanceSheetReport);

}
