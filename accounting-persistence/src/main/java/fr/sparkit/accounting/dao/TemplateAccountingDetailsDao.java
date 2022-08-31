package fr.sparkit.accounting.dao;

import java.util.List;

import org.springframework.stereotype.Repository;

import fr.sparkit.accounting.entities.TemplateAccountingDetails;

@Repository
public interface TemplateAccountingDetailsDao extends BaseRepository<TemplateAccountingDetails, Long> {

    List<TemplateAccountingDetails> findBytemplateAccountingIdAndIsDeletedFalse(Long templateAccountingId);

}
