package fr.sparkit.accounting.convertor;

import java.util.List;

import fr.sparkit.accounting.dto.BankReconciliationStatementDto;
import fr.sparkit.accounting.dto.CloseDocumentAccountLineDto;
import fr.sparkit.accounting.entities.Account;
import fr.sparkit.accounting.entities.BankReconciliationStatement;
import fr.sparkit.accounting.entities.FiscalYear;

public final class BankReconciliationStatementConvertor {

    private BankReconciliationStatementConvertor() {
        super();
    }

    public static BankReconciliationStatement dtoToModel(BankReconciliationStatementDto bankReconciliationStatementDto,
            Account account, FiscalYear fiscalYear) {

        return new BankReconciliationStatement(bankReconciliationStatementDto.getId(), account, fiscalYear,
                bankReconciliationStatementDto.getCloseMonth(), bankReconciliationStatementDto.getInitialAmount(),
                bankReconciliationStatementDto.getFinalAmount(), false, null);
    }

    public static BankReconciliationStatementDto modelToDto(BankReconciliationStatement bankReconciliationStatement,
            List<CloseDocumentAccountLineDto> closeDocumentAccountLines) {

        return new BankReconciliationStatementDto(bankReconciliationStatement.getId(),
                bankReconciliationStatement.getCloseMonth(), bankReconciliationStatement.getFiscalYear().getId(),
                bankReconciliationStatement.getFiscalYear().getName(), bankReconciliationStatement.getAccount().getId(),
                bankReconciliationStatement.getAccount().getLabel(), bankReconciliationStatement.getAccount().getCode(),
                bankReconciliationStatement.getInitialAmount(), bankReconciliationStatement.getFinalAmount(),
                closeDocumentAccountLines);
    }
}
