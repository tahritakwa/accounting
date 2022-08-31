package fr.sparkit.accounting.dao;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import fr.sparkit.accounting.entities.TemplateAccounting;

@Repository
public interface TemplateAccountingDao extends BaseRepository<TemplateAccounting, Long> {

    Page<TemplateAccounting> findByLabelContainsIgnoreCaseAndIsDeletedFalseOrJournalLabelContainsIgnoreCaseAndIsDeletedFalse(
            String label, String journalLabel, Pageable pageable);

    List<TemplateAccounting> findByJournalIdAndIsDeletedFalse(Long journalId);

    TemplateAccounting findByLabelAndIsDeletedFalse(String label);

}
