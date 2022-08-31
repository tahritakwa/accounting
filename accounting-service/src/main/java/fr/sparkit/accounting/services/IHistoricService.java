package fr.sparkit.accounting.services;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.sparkit.accounting.dto.HistoricDtoPage;
import fr.sparkit.accounting.dto.HistoricSearchFieldDto;
import fr.sparkit.accounting.dto.ReportLineDto;
import fr.sparkit.accounting.dto.SortDto;
import fr.sparkit.accounting.entities.Historic;

public interface IHistoricService extends IGenericService<Historic, Long> {

    HistoricDtoPage getHistoricDocumentAccount(Long entityId, String startDate, String endDate,
            String searchValue, Pageable pageable, SortDto sortDto);

    Page<Historic> getHistoricByEntity(String entityName, Long entityId, String startDate, String endDate,
            String searchValue, Pageable pageable, SortDto sortDto);

    List<HistoricSearchFieldDto> getDocumentAccountHistorySearchFields();

    HistoricDtoPage gethistoricDocumentAccountLine(Long entityId, Pageable pageable);

    HistoricDtoPage getHistoricReportLine(Long entityId, Pageable pageable);

    List<ReportLineDto> getReportLineFromHistoric(String reportType);

    HistoricDtoPage getLettringHistoric(Long accountId, Pageable pageable);

    HistoricDtoPage getReconciliationHistoric(Long accountId, Pageable pageable);

    HistoricDtoPage getFiscalYearHistoric(Pageable pageable);

}
