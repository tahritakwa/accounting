package fr.sparkit.accounting.restcontroller;

import java.time.LocalDateTime;
import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import fr.sparkit.accounting.auditing.HasRoles;
import fr.sparkit.accounting.constants.AccountingConstants;
import fr.sparkit.accounting.dto.CloseDocumentAccountLineDto;
import fr.sparkit.accounting.dto.DocumentAccountingDto;
import fr.sparkit.accounting.dto.DocumentPageDto;
import fr.sparkit.accounting.dto.FileDto;
import fr.sparkit.accounting.dto.FileUploadDto;
import fr.sparkit.accounting.dto.Filter;
import fr.sparkit.accounting.dto.ReconciliationDocumentAccountLinePageDto;
import fr.sparkit.accounting.entities.DocumentAccount;
import fr.sparkit.accounting.entities.DocumentAccountAttachement;
import fr.sparkit.accounting.services.IDocumentAccountLineService;
import fr.sparkit.accounting.services.IDocumentAccountService;
import fr.sparkit.accounting.services.IStorageService;
import fr.sparkit.accounting.services.impl.DocumentAccountService;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/accounting/document")
public class DocumentAccountController {

    private final IDocumentAccountService documentAccountService;
    private final IDocumentAccountLineService documentAccountLineService;
    private final IStorageService storageService;

    @Autowired
    public DocumentAccountController(DocumentAccountService documentAccountService,
            IDocumentAccountLineService documentAccountLineService,
            @Qualifier("DocumentAccountAttachmentService") IStorageService storageService) {
        this.documentAccountService = documentAccountService;
        this.documentAccountLineService = documentAccountLineService;
        this.storageService = storageService;
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(value = { "/{id}", "" })
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "ADD_DOCUMENTS_ACCOUNTS" })
    public DocumentAccount save(@RequestBody @Valid DocumentAccountingDto documentAccountingDto) {
        return documentAccountService.saveDocumentAccount(documentAccountingDto, false);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PutMapping(value = { "/{id}", "" })
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "UPDATE_DOCUMENTS_ACCOUNTS" })
    public DocumentAccount update(@RequestBody @Valid DocumentAccountingDto documentAccountingDto) {
        return documentAccountService.saveDocumentAccount(documentAccountingDto, false);
    }

    @DeleteMapping(value = "/{id}")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "DELETE_DOCUMENTS_ACCOUNTS" })
    public boolean deleteDocumentAccount(@RequestBody @PathVariable Long id) {
        return documentAccountService.deleteDocumentAccount(id);
    }

    @GetMapping(value = "/{id}")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "VIEW_DOCUMENTS_ACCOUNTS" })
    public DocumentAccountingDto getDocumentAccount(@PathVariable Long id) {
        return documentAccountService.getDocumentAccount(id);
    }

    @GetMapping(value = "/documents-account")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "VIEW_DOCUMENTS_ACCOUNTS" })
    public List<DocumentAccount> getAllDocumentAccount() {
        return documentAccountService.findAll();
    }

    @GetMapping(value = "/code-documents")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "VIEW_DOCUMENTS_ACCOUNTS", "ADD_DOCUMENTS_ACCOUNTS" })
    public DocumentAccountingDto getCodeDocument(
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime documentDate) {
        String codeDocument = documentAccountService.getCodeDocument(documentDate);
        return new DocumentAccountingDto(codeDocument);
    }

    @GetMapping(value = "/excel-template")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "EXPORT_MODEL_DOCUMENTS_ACCOUNTS" })
    public ResponseEntity<byte[]> exportModel() {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add(HttpHeaders.CONTENT_TYPE, AccountingConstants.EXCEL_DOC_TYPE);
        return new ResponseEntity<>(documentAccountService.exportDocumentAccountExcelModel(), responseHeaders,
                HttpStatus.OK);
    }

    @PostMapping(value = "/import-documents")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "IMPORT_DOCUMENTS_ACCOUNTS" })
    public ResponseEntity<FileUploadDto> importDocumentAccountsFromExcelFile(@RequestBody FileUploadDto fileUploadDto) {
        return new ResponseEntity<>(documentAccountService.loadDocumentAccountsExcelData(fileUploadDto), HttpStatus.OK);
    }

    @GetMapping(value = "/export-documents")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "EXPORT_DOCUMENTS_ACCOUNTS" })
    public ResponseEntity<byte[]> exportDocumentsAsExcelFile() {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add(HttpHeaders.CONTENT_TYPE, AccountingConstants.EXCEL_DOC_TYPE);
        return new ResponseEntity<>(documentAccountService.exportDocumentAccountsAsExcelFile(), responseHeaders,
                HttpStatus.OK);
    }

    @PostMapping(value = "/close-reconcilable-document-account-line")
    public ReconciliationDocumentAccountLinePageDto documentAccountLineInBetween(@RequestParam Long accountId,
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime endDate,
            @RequestBody List<CloseDocumentAccountLineDto> documentAccountLineAffected, Pageable pageable) {
        return documentAccountLineService.getReconcilableDocumentAccountLineInBetween(accountId, startDate, endDate,
                documentAccountLineAffected, pageable);
    }

    @PostMapping(value = "/close-reconcilable-document-account-all-line")
    public List<CloseDocumentAccountLineDto> allDocumentAccountLineInBetween(@RequestParam Long accountId,
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime endDate,
            @RequestBody List<CloseDocumentAccountLineDto> documentAccountLineAffected) {
        return documentAccountLineService.getAllReconcilableDocumentAccountLineInBetween(accountId, startDate, endDate,
                documentAccountLineAffected);
    }

    @PostMapping(value = "/upload-document-account-attachement")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "ADD_FILE_ATTACHMENT_TO_DOCUMENTS_ACCOUNTS" })
    public void uploadDocumentAccountAttachements(@RequestBody List<FileDto> fileDto) {
        storageService.store(fileDto);
    }

    @PostMapping(value = "/load-document-account-attachement")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "VIEW_DOCUMENTS_ACCOUNTS" })
    public Resource loadDocumentAccountAttachements(@RequestBody String fileName) {
        return storageService.loadFile(fileName);
    }

    @GetMapping(value = "/document-account-attachement/{id}")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "VIEW_DOCUMENTS_ACCOUNTS" })
    public List<DocumentAccountAttachement> getDocumentAccountAttachements(@PathVariable Long id) {
        return storageService.getDocumentAccountAttachements(id);
    }

    @DeleteMapping(value = "/document-account-attachement")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "DELETE_FILE_ATTACHMENT_TO_DOCUMENTS_ACCOUNTS" })
    public boolean deleteDocumentAccountAttachements(@RequestParam List<Long> filesIds) {
        return storageService.deleteAttachementFiles(filesIds);
    }

    @PostMapping(value = "/filter-document-account")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "VIEW_DOCUMENTS_ACCOUNTS", "ADD_DOCUMENTS_ACCOUNTS", "IMPORT_DOCUMENTS_ACCOUNTS",
            "EXPORT_DOCUMENTS_ACCOUNTS", "EXPORT_MODEL_DOCUMENTS_ACCOUNTS",
            "ADD_FILE_ATTACHMENT_TO_DOCUMENTS_ACCOUNTS" })
    public DocumentPageDto filterDocumentAccount(@RequestBody List<Filter> filters, Pageable pageable) {
        return documentAccountService.filterDocumentAccount(filters, pageable);
    }

    @PostMapping(value = "/generate-document-account-from-amortization")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "GENERATE_DOCUMENT_FROM_ACTIVES" })
    public DocumentAccountingDto generateDocumentFromAmortization(@RequestParam Long fiscalYearId,
            @RequestParam Boolean isDetailedGeneration, @RequestParam Long dotationAmortizationAccount,
            @RequestParam Long journalId,
            @RequestHeader(value = "Content-Type") String contentType, @RequestHeader(value = "User") String user,
            @RequestHeader("Authorization") String authorization) {
        return documentAccountService.generateDocumentAccountFromAmortization(fiscalYearId, dotationAmortizationAccount,
                journalId, isDetailedGeneration, contentType, user, authorization);
    }

    @GetMapping(value = "/is-document-account-generated-from-amortization/{fiscalYearId}")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "VIEW_DOCUMENTS_ACCOUNTS" })
    public boolean isDocumentAccountGeneratedFromAmortization(@PathVariable Long fiscalYearId) {
        return documentAccountService.isDocumentAccountGeneratedFromAmortization(fiscalYearId);
    }
}
