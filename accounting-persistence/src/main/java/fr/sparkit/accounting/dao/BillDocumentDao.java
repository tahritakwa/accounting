package fr.sparkit.accounting.dao;

import java.util.Optional;

import fr.sparkit.accounting.entities.BillDocument;

public interface BillDocumentDao extends BaseRepository<BillDocument, Long> {

    Optional<BillDocument> findByDocumentAccountIdAndIsDeletedFalse(Long idDocument);

    boolean existsByIdBillAndIsDeletedFalse(Long idBill);

    Optional<BillDocument> findByIdBillAndIsDeletedFalse(Long id);
}
