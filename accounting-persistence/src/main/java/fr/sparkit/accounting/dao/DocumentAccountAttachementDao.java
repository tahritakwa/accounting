package fr.sparkit.accounting.dao;

import java.util.List;

import org.springframework.stereotype.Repository;

import fr.sparkit.accounting.entities.DocumentAccountAttachement;

@Repository
public interface DocumentAccountAttachementDao extends BaseRepository<DocumentAccountAttachement, Long> {

    List<DocumentAccountAttachement> findByDocumentAccountIdAndIsDeletedFalse(Long documentAccountId);

    DocumentAccountAttachement findByDocumentAccountIdAndAndFileNameAndIsDeletedFalse(Long documentAccountId,
            String fileName);

    List<DocumentAccountAttachement> findByIdIn(List<Long> ids);

}
