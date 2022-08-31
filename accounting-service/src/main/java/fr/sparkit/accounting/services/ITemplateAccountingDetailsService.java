package fr.sparkit.accounting.services;

import java.lang.reflect.Field;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;

import fr.sparkit.accounting.dto.TemplateAccountingDetailsDto;
import fr.sparkit.accounting.entities.TemplateAccounting;
import fr.sparkit.accounting.entities.TemplateAccountingDetails;

public interface ITemplateAccountingDetailsService extends IGenericService<TemplateAccountingDetails, Long> {

    List<TemplateAccountingDetailsDto> getAllTemplateAccountingDetails();

    TemplateAccountingDetailsDto getTemplateAccountingDetailsById(Long id);

    void save(TemplateAccountingDetailsDto templateAccountingDetail, TemplateAccounting templateAccounting);

    List<TemplateAccountingDetails> findByTemplateAccountingIdAndIsDeletedFalse(Long id);

    boolean isAccountingTemplateDetailValuesAddedToRow(TemplateAccountingDetailsDto accountingTemplateDetail, Row row,
            List<Field> excelHeaderFields, List<String> acceptedHeaders);
}
