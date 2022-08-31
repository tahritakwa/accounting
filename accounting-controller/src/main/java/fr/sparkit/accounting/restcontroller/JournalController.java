package fr.sparkit.accounting.restcontroller;

import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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

import fr.sparkit.accounting.auditing.HasRoles;
import fr.sparkit.accounting.constants.AccountingConstants;
import fr.sparkit.accounting.convertor.UserPreferenceConvertor;
import fr.sparkit.accounting.dto.FileUploadDto;
import fr.sparkit.accounting.dto.Filter;
import fr.sparkit.accounting.dto.JournalDto;
import fr.sparkit.accounting.dto.UserPreferenceDto;
import fr.sparkit.accounting.entities.Journal;
import fr.sparkit.accounting.entities.UserPreference;
import fr.sparkit.accounting.services.IJournalService;
import fr.sparkit.accounting.services.IUserPreferenceService;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/accounting/journal")
public class JournalController {

    private final IJournalService journalService;
    private final IUserPreferenceService userPreferenceService;

    @Autowired
    public JournalController(IJournalService journalService, IUserPreferenceService userPreferenceService) {
        this.journalService = journalService;
        this.userPreferenceService = userPreferenceService;
    }

    @GetMapping(value = "/journals")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "ACCOUNTING", "VIEW_JOURNALS" })
    public List<Journal> findAll() {
        return journalService.findAll();
    }

    @GetMapping(value = "/{id}")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "ACCOUNTING", "VIEW_JOURNALS" })
    public JournalDto getJournal(@PathVariable Long id) {
        return journalService.findById(id);
    }

    @DeleteMapping(value = "/{id}")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "DELETE_JOURNALS" })
    public boolean deleteJournal(@PathVariable Long id) {
        return journalService.isDeleteJournal(id);
    }

    @PostMapping()
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "ADD_JOURNALS" })
    public JournalDto save(@RequestBody @Valid JournalDto journalDto) {
        return journalService.save(journalDto);
    }

    @PutMapping(value = "/{id}")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "UPDATE_JOURNALS" })
    public JournalDto updateJournal(@RequestBody @Valid JournalDto journalDto) {
        return journalService.update(journalDto);
    }

    @GetMapping(value = "/by-user/{id}")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "ACCOUNTING", "VIEW_JOURNALS" })
    public UserPreferenceDto getByUser(@PathVariable Long id) {
        return UserPreferenceConvertor.modelToDto(userPreferenceService.findUserPreference());
    }

    @PostMapping(value = "by-user")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "ACCOUNTING_SETTINGS" })
    public UserPreference saveOrUpdateByUser(@RequestBody UserPreferenceDto userPreferenceDto) {
        return userPreferenceService.saveOrUpdateByUser(userPreferenceDto);
    }

    @PostMapping(value = "/filter-journal")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "ACCOUNTING", "VIEW_JOURNALS" })
    public Page<JournalDto> filterJournal(@RequestBody List<Filter> filters, Pageable pageable) {
        return journalService.filterJournal(filters, pageable);
    }

    @GetMapping(value = "/excel-template")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "IMPORT_JOURNALS" })
    public ResponseEntity<byte[]> exportModel() {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add(HttpHeaders.CONTENT_TYPE, AccountingConstants.EXCEL_DOC_TYPE);
        return new ResponseEntity<>(journalService.exportJournalsExcelModel(), responseHeaders, HttpStatus.OK);
    }

    @PostMapping(value = "/import-journals")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "EXPORT_MODEL_JOURNALS" })
    public ResponseEntity<FileUploadDto> importJournalsFromExcelFile(@RequestBody FileUploadDto fileUploadDto) {
        return new ResponseEntity<>(journalService.loadJournalsExcelData(fileUploadDto), HttpStatus.OK);
    }

    @GetMapping(value = "/export-journals")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "EXPORT_JOURNALS" })
    public ResponseEntity<byte[]> exportJournalsAsExcelFile() {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add(HttpHeaders.CONTENT_TYPE, AccountingConstants.EXCEL_DOC_TYPE);
        return new ResponseEntity<>(journalService.exportJournalsAsExcelFile(), responseHeaders, HttpStatus.OK);
    }
}
