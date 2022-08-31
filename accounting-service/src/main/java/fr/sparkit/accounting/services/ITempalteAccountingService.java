package fr.sparkit.accounting.services;

import java.util.List;

import org.springframework.data.domain.Pageable;

import fr.sparkit.accounting.dto.FileUploadDto;
import fr.sparkit.accounting.dto.Filter;
import fr.sparkit.accounting.dto.TemplateAccountingDto;
import fr.sparkit.accounting.dto.TemplatePageDto;
import fr.sparkit.accounting.entities.TemplateAccounting;

public interface ITempalteAccountingService extends IGenericService<TemplateAccounting, Long> {

    List<TemplateAccountingDto> getAllTemplateAccounting();

    TemplateAccounting saveTemplateAccounting(TemplateAccountingDto templateAccountingDto);

    TemplateAccountingDto getTemplateAccountingById(Long id);

    boolean deleteTemplateAccounting(Long id);

    List<TemplateAccounting> getTemplateAccountingByJournal(Long id);

    TemplatePageDto filterTemplateAccounting(List<Filter> filters, Pageable pageable);

    byte[] exportAccountingTemplatesExcelModel();

    FileUploadDto loadAccountingTemplatesExcelData(FileUploadDto fileUploadDto);

    byte[] exportAccountingTemplatesAsExcelFile();
}
