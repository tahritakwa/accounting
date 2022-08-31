package fr.sparkit.accounting.restcontroller;

import java.time.LocalDateTime;
import java.util.List;

import javax.validation.Valid;

import fr.sparkit.accounting.auditing.HasRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.sparkit.accounting.constants.AccountingConstants;
import fr.sparkit.accounting.dto.LiterableDocumentAccountLineDto;
import fr.sparkit.accounting.dto.LiterableDocumentAccountLinePageDto;
import fr.sparkit.accounting.services.ILetteringService;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/accounting/lettering")
public class LetteringController {

    private final ILetteringService letteringService;

    @Autowired
    public LetteringController(ILetteringService letteringService) {
        this.letteringService = letteringService;
    }

    @GetMapping(value = "/document-account-lines-for-literable-account")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "VIEW_ACCOUNTING_LETTERING" })
    public LiterableDocumentAccountLinePageDto findDocumentAccountLinesForLiterableAccount(
            @RequestParam int accountPage, @RequestParam int literableLinePageSize, @RequestParam int literableLinePage,
            @RequestParam Boolean havingLetteredLines, @RequestParam String beginAccountCode,
            @RequestParam String endAccountCode, @RequestParam Boolean sameAmount,
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime endDate,
            @RequestParam String field, @RequestParam String direction) {
        return letteringService.findDocumentAccountLinesForLiterableAccount(accountPage, literableLinePageSize,
                literableLinePage, havingLetteredLines, beginAccountCode, endAccountCode, startDate, endDate,
                sameAmount, field, direction);
    }

    @GetMapping(value = "/auto-generate-letter-to-literable-document-account-line")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "PERFORM_LETTERING" })
    public LiterableDocumentAccountLinePageDto autoGenerateLetterToLiterableDocumentAccountLine(
            @RequestParam int accountPage, @RequestParam int literableLinePageSize, @RequestParam int literableLinePage,
            @RequestParam String beginAccountCode, @RequestParam String endAccountCode,
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime endDate) {
        return letteringService.autoLiterateDocumentAccountLinesWithOrder(accountPage, literableLinePageSize,
                literableLinePage, beginAccountCode, endAccountCode, startDate, endDate);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "PERFORM_LETTERING" })
    public List<LiterableDocumentAccountLineDto> saveLettersToSelectedLiterableDocumentAccountLine(
            @RequestBody @Valid List<LiterableDocumentAccountLineDto> selectedLiterableDocumentAccountLine) {
        return letteringService.saveLettersToSelectedLiterableDocumentAccountLine(selectedLiterableDocumentAccountLine);
    }

    @PostMapping(value = "/remove-letter-from-deselected-document-account-line")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "PERFORM_DELETTERING" })
    public List<LiterableDocumentAccountLineDto> removeLettersFromDeselectedDocumentAccountLine(
            @RequestBody List<LiterableDocumentAccountLineDto> deselectedDocumentAccountLine) {
        return letteringService.removeLettersFromDeselectedDocumentAccountLine(deselectedDocumentAccountLine);
    }

    @GetMapping(value = "/generate-letter-code")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "PERFORM_LETTERING" })
    public LiterableDocumentAccountLineDto generateFirstNotUsedLetter() {
        String letter = letteringService.generateFirstUnusedLetter();
        return new LiterableDocumentAccountLineDto(letter);
    }

}
