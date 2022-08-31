package fr.sparkit.accounting.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import fr.sparkit.accounting.dto.BillDto;
import fr.sparkit.accounting.dto.BillSuccessDto;
import fr.sparkit.accounting.dto.DocumentsToImportDto;
import fr.sparkit.accounting.dto.ImportMultipleBillDto;
import fr.sparkit.accounting.dto.PaymentAccountDto;
import fr.sparkit.accounting.dto.SettlementsToImportDto;
import fr.sparkit.accounting.entities.BillDocument;
import fr.sparkit.accounting.entities.DocumentAccount;
import fr.sparkit.accounting.entities.account.relations.PaymentAccount;

public interface IBillDocumentService extends IGenericService<BillDocument, Long> {

    BillSuccessDto importBill(BillDto billDto, String contentType, String user, String authorization,
            Long fiscalYearId, List<BillDto> billFailedDtos, List<String> listBillImported,
            List<Integer> httpErrorCodes, List<LocalDateTime> billIdNotInCurrentFiscalYear);

    boolean existsByBillId(Long idBill);

    BillSuccessDto deleteBill(Long documentId);

    List<DocumentAccount> findDocuments(List<Long> documentIds);

    ImportMultipleBillDto importMultipleBill(DocumentsToImportDto documentsToImport, String contentType,
            String user, String authorization);

    List<BillSuccessDto> importMultipleRegulation(SettlementsToImportDto settlementsToImportDtos,
            String contentType, String user, String authorization);

    PaymentAccount importTaxs(PaymentAccountDto paymentAccountDto);

    PaymentAccountDto findPaymentAccountsByTaxId(Long taxId);

    Optional<BillDocument> findByDocumentAccountId(Long idDocument);

    Boolean replaceSettlements(Long settlementId);

}
