package fr.sparkit.accounting.convertor;

import static fr.sparkit.accounting.auditing.MoneySerializer.THREE;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import fr.sparkit.accounting.dto.CloseDocumentAccountLineDto;
import fr.sparkit.accounting.dto.DocumentAccountLineDto;
import fr.sparkit.accounting.entities.Account;
import fr.sparkit.accounting.entities.DocumentAccount;
import fr.sparkit.accounting.entities.DocumentAccountLine;

public final class DocumentAccountingLineConvertor {

    private DocumentAccountingLineConvertor() {
        super();
    }

    public static DocumentAccountLine dtoToModel(DocumentAccountLineDto documentAccountLineDto) {
        return new DocumentAccountLine(documentAccountLineDto.getDocumentLineDate(), documentAccountLineDto.getLabel(),
                documentAccountLineDto.getReference(), documentAccountLineDto.getDebitAmount(),
                documentAccountLineDto.getCreditAmount(), documentAccountLineDto.isClose(),
                documentAccountLineDto.getLetter(), documentAccountLineDto.getReconciliationDate());
    }

    public static DocumentAccountLineDto modelToDto(DocumentAccountLine documentAccountLine) {
        return new DocumentAccountLineDto(documentAccountLine.getId(), documentAccountLine.getDocumentLineDate(),
                documentAccountLine.getLabel(), documentAccountLine.getReference(),
                documentAccountLine.getDebitAmount().setScale(THREE, RoundingMode.HALF_UP),
                documentAccountLine.getCreditAmount().setScale(THREE, RoundingMode.HALF_UP),
                documentAccountLine.getAccount().getId(), documentAccountLine.isClose(),
                documentAccountLine.getLetter(), documentAccountLine.getReconciliationDate());
    }

    public static List<DocumentAccountLine> dtosToModels(Collection<DocumentAccountLineDto> dtos) {
        return dtos.stream().filter(Objects::nonNull).map(DocumentAccountingLineConvertor::dtoToModel)
                .collect(Collectors.toList());
    }

    public static List<DocumentAccountLineDto> modelsToDtos(Collection<DocumentAccountLine> models) {
        return models.stream().filter(Objects::nonNull).map(DocumentAccountingLineConvertor::modelToDto)
                .collect(Collectors.toList());
    }

    public static CloseDocumentAccountLineDto documentAccountLineToClosedocumentAccountLineDto(
            DocumentAccountLine documentAccountLines) {
        return new CloseDocumentAccountLineDto(documentAccountLines.getId(), documentAccountLines.getLabel(),
                documentAccountLines.getReference(), documentAccountLines.getDebitAmount(),
                documentAccountLines.getCreditAmount(),
                documentAccountLines.getDocumentAccount().getJournal().getLabel(), documentAccountLines.isClose(),
                documentAccountLines.getDocumentAccount().getId(), documentAccountLines.getDocumentLineDate(),
                documentAccountLines.getDocumentAccount().getCodeDocument());
    }

    public static List<CloseDocumentAccountLineDto> documentAccountLinesToCloseDocumentAccountLineDtos(
            Collection<DocumentAccountLine> documentAccountLines) {
        return documentAccountLines.stream().filter(Objects::nonNull)
                .map(DocumentAccountingLineConvertor::documentAccountLineToClosedocumentAccountLineDto)
                .collect(Collectors.toList());
    }

    public static DocumentAccountLine closeDocumentAccountLineDtosToDocumentAccountLine(
            CloseDocumentAccountLineDto closeDocumentAccountLineDto, DocumentAccount documentAccount, Account account) {
        return new DocumentAccountLine(closeDocumentAccountLineDto.getId(),
                closeDocumentAccountLineDto.getDocumentDate(), closeDocumentAccountLineDto.getLabel(),
                closeDocumentAccountLineDto.getReference(), closeDocumentAccountLineDto.getDebitAmount(),
                closeDocumentAccountLineDto.getCreditAmount(), null, documentAccount,
                closeDocumentAccountLineDto.isClose(), LocalDate.now(), account, false, null);
    }

}
