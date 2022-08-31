package fr.sparkit.accounting.services;

import java.time.LocalDateTime;
import java.util.List;

import fr.sparkit.accounting.dto.LiterableDocumentAccountLineDto;
import fr.sparkit.accounting.dto.LiterableDocumentAccountLinePageDto;

public interface ILetteringService {

    LiterableDocumentAccountLinePageDto findDocumentAccountLinesForLiterableAccount(int accountPage,
            int literableLinePageSize, int literableLinePage, Boolean havingLetterdLines, String beginAccountCode,
            String endAccountCode, LocalDateTime startDate, LocalDateTime endDate, Boolean sameAmount, String field,
            String direction);

    List<LiterableDocumentAccountLineDto> saveLettersToSelectedLiterableDocumentAccountLine(
            List<LiterableDocumentAccountLineDto> selectedLiterableDocumentAccountLine);

    List<LiterableDocumentAccountLineDto> removeLettersFromDeselectedDocumentAccountLine(
            List<LiterableDocumentAccountLineDto> deselectedDocumentAccountLine);

    LiterableDocumentAccountLinePageDto autoLiterateDocumentAccountLines(int accountPage, int literableLinePageSize,
            int literableLinePage, String beginAccountCode, String endAccountCode, LocalDateTime startDate,
            LocalDateTime endDate, String field, String direction);

    String generateFirstUnusedLetter();

    LiterableDocumentAccountLinePageDto autoLiterateDocumentAccountLinesWithOrder(int accountPage,
            int literableLinePageSize, int literableLinePage, String beginAccountCode, String endAccountCode,
            LocalDateTime startDate, LocalDateTime endDate);

}
