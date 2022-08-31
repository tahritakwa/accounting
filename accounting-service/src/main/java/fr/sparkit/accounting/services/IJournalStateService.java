package fr.sparkit.accounting.services;

import java.time.LocalDateTime;
import java.util.List;

import fr.sparkit.accounting.dto.JournalStateReportLineDto;
import org.springframework.data.domain.Page;

import fr.sparkit.accounting.dto.JournalStateDetailsDto;
import fr.sparkit.accounting.dto.JournalStateDto;
import fr.sparkit.accounting.entities.DocumentAccount;
import org.springframework.data.domain.Pageable;

public interface IJournalStateService {
    Page<JournalStateDto> findJournalsState(int page, int size, LocalDateTime startDate, LocalDateTime endDate);

    Page<JournalStateDetailsDto> findJournalStateDetails(Long journalId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    void addToJournalStateJournalDetails(DocumentAccount documentAccount,
            List<JournalStateDetailsDto> journalStateDetailsDtos, LocalDateTime startDate, LocalDateTime endDate);

    void testDotnetUrl(String contentType, String user, String authorization, String dotnetRessource);

    List<JournalStateReportLineDto> findJournalsStateReport(LocalDateTime startDate, LocalDateTime endDate);

}
