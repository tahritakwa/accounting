package fr.sparkit.accounting.services;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.sparkit.accounting.dto.AuxiliaryJournalDetailsDto;
import fr.sparkit.accounting.dto.AuxiliaryJournalLineDto;
import fr.sparkit.accounting.dto.StateOfAuxiliaryJournalPage;

public interface IStateOfAuxiliaryJournalService {

    StateOfAuxiliaryJournalPage findAuxiliaryJournals(LocalDateTime startDate, LocalDateTime endDate,
            List<Long> journalIds, Pageable pageable);

    Page<AuxiliaryJournalDetailsDto> findAuxiliaryJournalDetails(Long id, LocalDateTime startDate,
            LocalDateTime endDate, Pageable pageable);

    List<AuxiliaryJournalLineDto> generateAuxiliaryJournalsTelerikReport(LocalDateTime startDate, LocalDateTime endDate,
            List<Long> journalIds);

}
