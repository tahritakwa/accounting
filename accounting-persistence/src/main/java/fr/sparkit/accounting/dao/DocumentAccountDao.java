package fr.sparkit.accounting.dao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import fr.sparkit.accounting.entities.DocumentAccount;

@Repository
public interface DocumentAccountDao extends BaseRepository<DocumentAccount, Long> {

    DocumentAccount findFirstByDocumentDateBetweenAndIsDeletedFalseOrderByCodeDocumentDesc(LocalDateTime dayStart,
            LocalDateTime dayEnd);

    Optional<DocumentAccount> findByCodeDocumentAndFiscalYearIdAndIsDeletedFalse(String codeDocument,
            Long fiscalYearId);

    List<DocumentAccount> findByJournalIdAndDocumentDateBetweenAndIsDeletedFalse(Long journalId,
            LocalDateTime startDate, LocalDateTime endDate);

    List<DocumentAccount> findByFiscalYearIdAndIsDeletedFalseOrderByDocumentDateDescCodeDocumentDesc(Long fiscalYearId);

    @Query("SELECT dal FROM DocumentAccount dal where isDeleted=false AND indexOfStatus = 2 and fiscalYear.id = ?1")
    DocumentAccount findByFiscalYearIdAndIsCreatedByConcludingCurrentFiscalYear(Long fiscalYearId);

    boolean existsByIdAndIsDeletedFalse(Long documentAccountId);

    @Query("SELECT id FROM DocumentAccount dal where isDeleted=false AND indexOfStatus = 3 and fiscalYear.id = ?1")
    List<Long> getListOfDocumentAccountGeneratedFromAmortization(Long currentFiscalYearId);

    @Query("SELECT count(id) FROM DocumentAccount where isDeleted=false and fiscalYear.id=?1")
    long findNumberOfDocumentsInCurrentFiscalYear(Long currentFiscalYearId);
}
