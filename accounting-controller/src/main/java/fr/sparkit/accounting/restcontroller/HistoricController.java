package fr.sparkit.accounting.restcontroller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.sparkit.accounting.dto.HistoricDtoPage;
import fr.sparkit.accounting.dto.HistoricSearchFieldDto;
import fr.sparkit.accounting.dto.ReportLineDto;
import fr.sparkit.accounting.dto.SortDto;
import fr.sparkit.accounting.services.IHistoricService;

@RestController
@RequestMapping(value = { "/api/accounting/history" })
public class HistoricController {

    private final IHistoricService historicService;

    @Autowired
    public HistoricController(IHistoricService historicService) {
        this.historicService = historicService;
    }

    @GetMapping(value = "/historic-by-document-account-id")
    public HistoricDtoPage getDocumentAccountHistorics(@RequestParam Long entityId,
            @RequestParam(required = false, defaultValue = "") String startDate,
            @RequestParam(required = false, defaultValue = "") String endDate,
            @RequestParam(required = false, defaultValue = "") String searchValue, Pageable pageable, SortDto sortDto) {
        return historicService.getHistoricDocumentAccount(entityId, startDate, endDate, searchValue, pageable, sortDto);
    }

    @GetMapping(value = "/historic-document-account-search-fields")
    public List<HistoricSearchFieldDto> getHistorySearchFields() {
        return historicService.getDocumentAccountHistorySearchFields();
    }

    @GetMapping(value = "/historic-line-by-document-account-id")
    public HistoricDtoPage getDocumentAccountLineHistorics(@RequestParam Long entityId, Pageable pageable) {
        return historicService.gethistoricDocumentAccountLine(entityId, pageable);
    }

    @GetMapping(value = "/historic-line-report-type")
    public List<ReportLineDto> getReportLineFromHistoric(@RequestParam String reportType) {
        return historicService.getReportLineFromHistoric(reportType);
    }

    @GetMapping(value = "/historic-line-by-reportLine-id")
    public HistoricDtoPage getHistoricReportLine(@RequestParam Long entityId, Pageable pageable) {
        return historicService.getHistoricReportLine(entityId, pageable);
    }

    @GetMapping(value = "/historic-lettring")
    public HistoricDtoPage getLettringHistoric(@RequestParam Long accountId, Pageable pageable) {
        return historicService.getLettringHistoric(accountId, pageable);
    }

    @GetMapping(value = "/historic-reconciliation")
    public HistoricDtoPage getReconciliationHistoric(@RequestParam Long accountId, Pageable pageable) {
        return historicService.getReconciliationHistoric(accountId, pageable);
    }

    @GetMapping(value = "/historic-fiscal-year")
    public HistoricDtoPage getFiscalYearHistoric(Pageable pageable) {
        return historicService.getFiscalYearHistoric(pageable);
    }
}
