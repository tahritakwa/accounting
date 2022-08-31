package fr.sparkit.accounting.services;

import org.springframework.stereotype.Service;

import fr.sparkit.accounting.dto.BillDto;
import fr.sparkit.accounting.dto.RegulationDto;
import fr.sparkit.accounting.entities.DocumentAccount;

@Service
public interface IDocumentImporter {

    DocumentAccount importDocument(BillDto billDto);

    DocumentAccount importRegulation(RegulationDto regulationDto, Long bankAccoutId, Long cofferAccountId, boolean isMultipleRegulationImportation);

    DocumentAccount importCreditNote(BillDto billDto);

}
