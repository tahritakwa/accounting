package fr.sparkit.accounting.convertor;

import static fr.sparkit.accounting.util.CalculationUtil.getFormattedBigDecimalValue;

import java.util.ArrayList;
import java.util.List;

import fr.sparkit.accounting.dto.CloseDocumentAccountLineDto;
import fr.sparkit.accounting.dto.PrintableCloseDocumentAccountLineDto;

public final class CloseDocumentAccountLineConvertor {

    private CloseDocumentAccountLineConvertor() {
        super();
    }

    public static PrintableCloseDocumentAccountLineDto closeDocumentAccountLineDtoToPrintable(
            CloseDocumentAccountLineDto closeDocumentAccountLineDto) {
        return new PrintableCloseDocumentAccountLineDto(closeDocumentAccountLineDto.getLabel(),
                closeDocumentAccountLineDto.getReference(),
                getFormattedBigDecimalValue(closeDocumentAccountLineDto.getDebitAmount()),
                getFormattedBigDecimalValue(closeDocumentAccountLineDto.getCreditAmount()),
                closeDocumentAccountLineDto.getJournalLabel(), closeDocumentAccountLineDto.getDocumentDate(),
                closeDocumentAccountLineDto.getCodeDocument());

    }

    public static List<PrintableCloseDocumentAccountLineDto> closeDocumentAccountLinesDtoToPrintables(
            Iterable<CloseDocumentAccountLineDto> closeDocumentAccountLineDtos) {
        List<PrintableCloseDocumentAccountLineDto> bankReconcialiationReportLineDtos = new ArrayList<>();
        closeDocumentAccountLineDtos.forEach((CloseDocumentAccountLineDto line) -> {
            PrintableCloseDocumentAccountLineDto bankReconcialiationReportLineDto = closeDocumentAccountLineDtoToPrintable(
                    line);
            bankReconcialiationReportLineDtos.add(bankReconcialiationReportLineDto);
        });
        return bankReconcialiationReportLineDtos;
    }
}
