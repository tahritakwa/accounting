package fr.sparkit.accounting.services;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.sparkit.accounting.dto.CentralizingJournalByMonthReportLineDto;
import fr.sparkit.accounting.dto.CentralizingJournalDetailsByMonthDto;
import fr.sparkit.accounting.dto.CentralizingJournalDetailsDto;
import fr.sparkit.accounting.dto.CentralizingJournalDto;
import fr.sparkit.accounting.dto.CentralizingJournalReportLineDto;
import fr.sparkit.accounting.dto.FileUploadDto;
import fr.sparkit.accounting.dto.Filter;
import fr.sparkit.accounting.dto.JournalDto;
import fr.sparkit.accounting.entities.Journal;

public interface IJournalService extends IGenericService<Journal, Long> {

    JournalDto findById(Long id);

    JournalDto save(JournalDto journalDto);

    JournalDto update(JournalDto journalDto);

    boolean isDeleteJournal(Long id);

    Journal findJournalByLabel(String label);

    Journal findByCode(String code);

    boolean existsById(Long journalANewId);

    CentralizingJournalDto findCentralizingJournalPage(int page, int size, LocalDateTime startDate,
            LocalDateTime endDate, List<Long> journalIds, int breakingAccount, int breakingCustomerAccount,
            int breakingProviderAccount);

    List<CentralizingJournalDetailsDto> findCentralizingJournalDetails(Long journalId, LocalDateTime startDate,
            LocalDateTime endDate, int breakingAccount, int breakingCustomerAccount, int breakingProviderAccount);

    Page<CentralizingJournalDetailsByMonthDto> findCentralizingJournalDetailsByMonthPage(int page, int size,
            Long journalId, LocalDateTime startDate, LocalDateTime endDate, int breakingAccount,
            int breakingCustomerAccount, int breakingSupplierAccount, String month);

    List<CentralizingJournalReportLineDto> generateCentralizingJournalTelerikReportLines(List<Long> journalIds,
            LocalDateTime startDate, LocalDateTime endDate, int breakingAccount, int breakingCustomerAccount,
            int breakingProviderAccount);

    List<CentralizingJournalByMonthReportLineDto> generateCentralizingJournalByMonthReportLines(List<Long> journalIds,
            LocalDateTime startDate, LocalDateTime endDate, int breakingAccount, int breakingCustomerAccount,
            int breakingSupplierAccount);

    List<Journal> findByIds(List<Long> journalIds);

    Page<JournalDto> filterJournal(List<Filter> filters, Pageable pageable);

    byte[] exportJournalsExcelModel();

    FileUploadDto loadJournalsExcelData(FileUploadDto fileUploadDto);

    byte[] exportJournalsAsExcelFile();

}
