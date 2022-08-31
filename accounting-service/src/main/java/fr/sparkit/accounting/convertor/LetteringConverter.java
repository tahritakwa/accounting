package fr.sparkit.accounting.convertor;

import java.math.BigDecimal;

import fr.sparkit.accounting.dto.LiterableDocumentAccountLineDto;
import fr.sparkit.accounting.entities.Account;
import fr.sparkit.accounting.entities.DocumentAccount;
import fr.sparkit.accounting.entities.DocumentAccountLine;
import fr.sparkit.accounting.entities.Journal;

public final class LetteringConverter {

    private LetteringConverter() {
        super();
    }

    public static LiterableDocumentAccountLineDto documentAccountingLineToLetteringDto(
            DocumentAccountLine documentAccountLine, DocumentAccount documentAccount, Account account,
            Journal journal) {
        BigDecimal debit = documentAccountLine.getDebitAmount();
        BigDecimal credit = documentAccountLine.getCreditAmount();
        BigDecimal balance = debit.subtract(credit);
        return new LiterableDocumentAccountLineDto(documentAccountLine.getId(),
                account.getCode() + " " + account.getLabel(), documentAccount.getDocumentDate(),
                documentAccount.getCodeDocument(), journal.getLabel(), documentAccountLine.getReference(), debit,
                credit, balance, documentAccountLine.getLetter(), documentAccount.getId(),
                documentAccountLine.getLabel(), account.getId());
    }

}
