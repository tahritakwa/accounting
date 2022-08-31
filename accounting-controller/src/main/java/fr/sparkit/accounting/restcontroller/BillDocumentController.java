package fr.sparkit.accounting.restcontroller;

import java.util.List;

import fr.sparkit.accounting.auditing.HasRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import fr.sparkit.accounting.dto.BillSuccessDto;
import fr.sparkit.accounting.dto.DocumentsToImportDto;
import fr.sparkit.accounting.dto.ImportMultipleBillDto;
import fr.sparkit.accounting.dto.PaymentAccountDto;
import fr.sparkit.accounting.dto.SettlementsToImportDto;
import fr.sparkit.accounting.entities.DocumentAccount;
import fr.sparkit.accounting.entities.account.relations.PaymentAccount;
import fr.sparkit.accounting.services.IBillDocumentService;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/accounting/bill")
public class BillDocumentController {

    private final IBillDocumentService billDocumentService;

    @Autowired
    public BillDocumentController(IBillDocumentService billDocumentService) {
        this.billDocumentService = billDocumentService;
    }

    @DeleteMapping(value = "/{id}")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "IMPORT_ACCOUNTING_DOCUMENTS" })
    public BillSuccessDto deleteBill(@PathVariable Long id) {
        return billDocumentService.deleteBill(id);
    }

    @PostMapping(value = "/getDocuments")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "IMPORT_ACCOUNTING_DOCUMENTS" })
    public List<DocumentAccount> findByBillId(@RequestBody List<Long> documentIds) {
        return billDocumentService.findDocuments(documentIds);
    }

    @PostMapping(value = "/importDocuments")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "IMPORT_ACCOUNTING_DOCUMENTS" })
    public ImportMultipleBillDto importBill(@RequestBody DocumentsToImportDto documentsToImport,
            @RequestParam String contentType, @RequestHeader("User") String user, @RequestHeader("Authorization") String authorization) {
        return billDocumentService.importMultipleBill(documentsToImport, contentType, user, authorization);
    }

    @PostMapping(value = "/importRegulations")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "IMPORT_ACCOUNTING_DOCUMENTS" })
    public List<BillSuccessDto> importRegulations(@RequestBody SettlementsToImportDto settlementsToImportDtos,
            @RequestParam String contentType, @RequestParam String user, @RequestParam String authorization) {
        return billDocumentService.importMultipleRegulation(settlementsToImportDtos, contentType, user, authorization);
    }

    @PostMapping(value = "/import-tax")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "IMPORT_ACCOUNTING_DOCUMENTS" })
    public PaymentAccount importTaxs(@RequestBody PaymentAccountDto paymentAccountDto) {
        return billDocumentService.importTaxs(paymentAccountDto);
    }

    @GetMapping(value = "/tax")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "IMPORT_ACCOUNTING_DOCUMENTS" })
    public PaymentAccountDto getPaymentAccount(@RequestParam Long taxId) {
        return billDocumentService.findPaymentAccountsByTaxId(taxId);
    }

    @GetMapping(value = "/replace-settlement")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "IMPORT_ACCOUNTING_DOCUMENTS" })
    public Boolean replaceSettlements(@RequestParam Long settlementId) {
        return billDocumentService.replaceSettlements(settlementId);
    }
}
