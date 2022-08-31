package fr.sparkit.accounting.restcontroller;

import java.util.List;

import javax.validation.Valid;

import fr.sparkit.accounting.auditing.HasRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.sparkit.accounting.constants.AccountingConstants;
import fr.sparkit.accounting.dto.FileUploadDto;
import fr.sparkit.accounting.dto.Filter;
import fr.sparkit.accounting.dto.TemplateAccountingDetailsDto;
import fr.sparkit.accounting.dto.TemplateAccountingDto;
import fr.sparkit.accounting.dto.TemplatePageDto;
import fr.sparkit.accounting.entities.TemplateAccounting;
import fr.sparkit.accounting.services.ITempalteAccountingService;
import fr.sparkit.accounting.services.ITemplateAccountingDetailsService;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/accounting/template-accounting")
public class TemplateAccountingController {

    private final ITempalteAccountingService templateAccountingService;

    private final ITemplateAccountingDetailsService templateAccountingDetailsService;

    @Autowired
    public TemplateAccountingController(ITempalteAccountingService templateAccountingService,
            ITemplateAccountingDetailsService templateAccountingDetailsService) {
        super();
        this.templateAccountingService = templateAccountingService;
        this.templateAccountingDetailsService = templateAccountingDetailsService;
    }

    @GetMapping(value = "/templates")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "VIEW_ACCOUNTING_TEMPLATE" })
    public List<TemplateAccountingDto> allTemplates() {
        return templateAccountingService.getAllTemplateAccounting();
    }

    @GetMapping(value = "/{id}")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "VIEW_ACCOUNTING_TEMPLATE" })
    public TemplateAccountingDto getTemplate(@PathVariable Long id) {
        return templateAccountingService.getTemplateAccountingById(id);
    }

    @PostMapping()
    @PutMapping(value = { "/{id}", "" })
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "ADD_ACCOUNTING_TEMPLATE", "UPDATE_ACCOUNTING_TEMPLATE" })
    public TemplateAccounting saveTemplate(@RequestBody @Valid TemplateAccountingDto templateAccountingDto) {

        return templateAccountingService.saveTemplateAccounting(templateAccountingDto);
    }

    @DeleteMapping(value = "/{id}")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "DELETE_ACCOUNTING_TEMPLATE", "UPDATE_ACCOUNTING_TEMPLATE" })
    public boolean deleteTemplate(@PathVariable Long id) {
        return templateAccountingService.deleteTemplateAccounting(id);
    }

    @GetMapping(value = "/template-lines")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "VIEW_ACCOUNTING_TEMPLATE" })
    public List<TemplateAccountingDetailsDto> allTemplateLines() {
        return templateAccountingDetailsService.getAllTemplateAccountingDetails();
    }

    @GetMapping(value = "/template-lines/{id}")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "VIEW_ACCOUNTING_TEMPLATE" })
    public TemplateAccountingDetailsDto getTemplateLines(@PathVariable Long id) {
        return templateAccountingDetailsService.getTemplateAccountingDetailsById(id);
    }

    @GetMapping(value = "/search-by-journal/{id}")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "ACCOUNTING","VIEW_ACCOUNTING_TEMPLATE" })
    public List<TemplateAccounting> getTemplateByJournal(@PathVariable Long id) {
        return templateAccountingService.getTemplateAccountingByJournal(id);
    }

    @PostMapping(value = "/filter-template-accounting")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "VIEW_ACCOUNTING_TEMPLATE" })
    public TemplatePageDto filterTemplateAccounting(@RequestBody List<Filter> filters, Pageable pageable) {
        return templateAccountingService.filterTemplateAccounting(filters, pageable);
    }

    @GetMapping(value = "/excel-template")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "EXPORT_MODEL_ACCOUNTING_TEMPLATE" })
    public ResponseEntity<byte[]> exportModel() {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add(HttpHeaders.CONTENT_TYPE, AccountingConstants.EXCEL_DOC_TYPE);
        return new ResponseEntity<>(templateAccountingService.exportAccountingTemplatesExcelModel(), responseHeaders,
                HttpStatus.OK);
    }

    @PostMapping(value = "/import-accounting-templates")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "IMPORT_ACCOUNTING_TEMPLATE" })
    public ResponseEntity<FileUploadDto> importAccountsFromExcelFile(@RequestBody FileUploadDto fileUploadDto) {
        return new ResponseEntity<>(templateAccountingService.loadAccountingTemplatesExcelData(fileUploadDto),
                HttpStatus.OK);
    }

    @GetMapping(value = "/export-accounting-templates")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "EXPORT_ACCOUNTING_TEMPLATE" })
    public ResponseEntity<byte[]> exportAccountsAsExcelFile() {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add(HttpHeaders.CONTENT_TYPE, AccountingConstants.EXCEL_DOC_TYPE);
        return new ResponseEntity<>(templateAccountingService.exportAccountingTemplatesAsExcelFile(), responseHeaders,
                HttpStatus.OK);
    }
}
