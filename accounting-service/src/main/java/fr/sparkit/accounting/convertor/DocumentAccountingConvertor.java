package fr.sparkit.accounting.convertor;

import java.math.BigDecimal;
import java.util.List;

import fr.sparkit.accounting.dto.DocumentAccountLineDto;
import fr.sparkit.accounting.dto.DocumentAccountingDto;
import fr.sparkit.accounting.entities.DocumentAccount;
import fr.sparkit.accounting.entities.Journal;

public final class DocumentAccountingConvertor {

    private DocumentAccountingConvertor() {
        super();
    }

    public static DocumentAccountingDto modelToDto(DocumentAccount documentAccount, BigDecimal totalDebitAmount,
            BigDecimal totalCreditAmount, List<DocumentAccountLineDto> documentAccountLines) {
        Journal journal = new Journal();
        if (documentAccount.getJournal() != null) {
            journal = documentAccount.getJournal();
        }
        return new DocumentAccountingDto(documentAccount.getId(), documentAccount.getDocumentDate(),
                documentAccount.getLabel(), documentAccount.getCodeDocument(), totalDebitAmount, totalCreditAmount,
                journal.getId(), journal.getLabel(), documentAccountLines, documentAccount.getFiscalYear().getId(),
                null, documentAccount.getIndexOfStatus());
    }

    public static DocumentAccount dtoToModel(DocumentAccountingDto documentAccountingDto, Journal journal) {
        return new DocumentAccount(documentAccountingDto.getCodeDocument(), documentAccountingDto.getDocumentDate(),
                documentAccountingDto.getLabel(), journal, documentAccountingDto.getIndexOfStatus());
    }

}
