package fr.sparkit.accounting.services.utils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import fr.sparkit.accounting.constants.NumberConstant;
import fr.sparkit.accounting.convertor.AccountConvertor;
import fr.sparkit.accounting.convertor.DocumentAccountingLineConvertor;
import fr.sparkit.accounting.dto.AccountDto;
import fr.sparkit.accounting.dto.BillDetailsDto;
import fr.sparkit.accounting.dto.BillDto;
import fr.sparkit.accounting.dto.DocumentAccountingDto;
import fr.sparkit.accounting.dto.RegulationDto;
import fr.sparkit.accounting.entities.Account;
import fr.sparkit.accounting.entities.DocumentAccountLine;
import fr.sparkit.accounting.enumuration.DocumentAccountStatus;
import fr.sparkit.accounting.util.errors.ApiErrors;

@Service
public final class ImportDocumentUtil {

    public static final char ZERO_CHAR = '0';

    private ImportDocumentUtil() {
    }

    public static Map<BillDetailsDto, BigDecimal> getBillDetailsDtosBySumAmount(BillDto billDto) {
        return billDto.getBillDetails().stream().filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(),
                        Collectors.reducing(BigDecimal.ZERO, BillDetailsDto::getPretaxAmount, BigDecimal::add)));
    }

    public static DocumentAccountingDto billDetailsDtoToDocumentAccountingDto(BillDto billDto,
            List<DocumentAccountLine> documentAccountLines) {
        return new DocumentAccountingDto(billDto.getCodeDocument(), billDto.getDocumentDate(), billDto.getIdJournal(),
                DocumentAccountStatus.BY_IMPORT_DOCUMENT_IS_CREATED.getIndex(),
                DocumentAccountingLineConvertor.modelsToDtos(documentAccountLines));
    }

    public static DocumentAccountingDto regulationDtoToDocumentAccountingDto(RegulationDto regulationDto,
            List<DocumentAccountLine> documentAccountLines) {
        return new DocumentAccountingDto(regulationDto.getCodeSettlement(), regulationDto.getSettlementDate(),
                regulationDto.getIdJournal(), DocumentAccountStatus.BY_IMPORT_SETTLEMENT_IS_CREATED.getIndex(),
                DocumentAccountingLineConvertor.modelsToDtos(documentAccountLines));
    }

    public static List<BillDetailsDto> getAllBillDetailsDtos(BillDto billDto) {
        return billDto.getBillDetails().stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    public static BigDecimal groupBillDetailsByAccount(BillDto billDto) {
        AtomicReference<BigDecimal> amountHT = new AtomicReference<>();
        amountHT.set(BigDecimal.ZERO);
        billDto.getBillDetails().forEach(
                (BillDetailsDto billDetailsDto) -> amountHT.set(amountHT.get().add(billDetailsDto.getPretaxAmount())));
        return amountHT.get();
    }

    public static List<DocumentAccountLine> groupBillDetailsByAccount(String codeDocument, String tierName,
            Account taxAccount, String documentType, LocalDateTime documentDate, BigDecimal amount) {
        List<DocumentAccountLine> documentAccountLines = new ArrayList<>();
        switch (documentType) {
        case ApiErrors.Accounting.INVOICE_SALES:
        case ApiErrors.Accounting.PURCHASES_ASSETS:
            if (amount.compareTo(BigDecimal.ZERO) == NumberConstant.ONE) {
                documentAccountLines.add(new DocumentAccountLine(documentDate, tierName, codeDocument, BigDecimal.ZERO,
                        amount, taxAccount));
            }
            break;
        case ApiErrors.Accounting.SALES_ASSETS:
        case ApiErrors.Accounting.INVOICE_PURCHASES:
            if (amount.compareTo(BigDecimal.ZERO) == NumberConstant.ONE) {
                documentAccountLines.add(new DocumentAccountLine(documentDate, tierName, codeDocument, amount,
                        BigDecimal.ZERO, taxAccount));
            }
            break;
        default:
        }
        return documentAccountLines;
    }

    public static void balancedDocument(BillDto billDto, Collection<DocumentAccountLine> documentAccountLines,
            BillDetailsDto billDetailsDto, Account taxAccount, Account hTaxaccount) {
        documentAccountLines.addAll(ImportDocumentUtil.groupBillDetailsByAccount(billDto.getCodeDocument(),
                billDto.getTierName(), taxAccount, billDto.getDocumentType(), billDto.getDocumentDate(),
                billDetailsDto.getVatAmount()));
        documentAccountLines.addAll(ImportDocumentUtil.groupBillDetailsByAccount(billDto.getCodeDocument(),
                billDto.getTierName(), hTaxaccount, billDto.getDocumentType(), billDto.getDocumentDate(),
                billDetailsDto.getPretaxAmount()));

    }

    public static StringBuilder generateAccountCode(int chartAccountCode) {
        StringBuilder taxAccountCode = new StringBuilder().append(chartAccountCode);
        for (int i = taxAccountCode.length(); i < NumberConstant.EIGHT; i++) {
            taxAccountCode.append(ZERO_CHAR);
        }
        return taxAccountCode;
    }

    public static DocumentAccountLine getSalesDiscountDocumentLine(BillDto billDto, AccountDto salesAccountDto) {
        return new DocumentAccountLine(billDto.getDocumentDate(), billDto.getTierName(), billDto.getCodeDocument(),
                billDto.getRistourn(), BigDecimal.ZERO, AccountConvertor.dtoToModel(salesAccountDto, null));
    }

    public static DocumentAccountLine getPurchaseDiscountDocumentLine(BillDto billDto, AccountDto purchaseAccountDto) {
        return new DocumentAccountLine(billDto.getDocumentDate(), billDto.getTierName(), billDto.getCodeDocument(),
                BigDecimal.ZERO, billDto.getRistourn(), AccountConvertor.dtoToModel(purchaseAccountDto, null));
    }

}
