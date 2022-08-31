package fr.sparkit.accounting.services.utils;

import fr.sparkit.accounting.constants.AccountingConstants;
import fr.sparkit.accounting.dto.AccountDto;
import fr.sparkit.accounting.dto.ReportTemplateDefaultParameters;
import fr.sparkit.accounting.util.CalculationUtil;
import fr.sparkit.accounting.util.errors.ApiErrors;
import fr.sparkit.accounting.util.errors.ErrorsResponse;
import fr.sparkit.accounting.util.http.HttpCustomException;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Map;
import java.util.ResourceBundle;

import static fr.sparkit.accounting.constants.AccountingConstants.*;

@Slf4j
public final class ReportLineUtil {
    private ReportLineUtil() {
        super();
    }

    public static JRBeanCollectionDataSource getJRBeanCollection(Collection<?> beanCollection) {
        return new JRBeanCollectionDataSource(beanCollection);
    }

    public static JasperPrint compileJRXMLFile(Map<String, Object> parameters,
                                               JRBeanCollectionDataSource beanColDataSource, String path) throws JRException {
        JasperReport jasperReport = JasperCompileManager.compileReport(path);
        return JasperFillManager.fillReport(jasperReport, parameters, beanColDataSource);
    }

    public static void fillTheMapParametersWithGeneralInfos(LocalDateTime startDate, LocalDateTime endDate,
                                                            ReportTemplateDefaultParameters reportTemplateParams, Map<String, Object> parameters) {
        ResourceBundle i18nResourceBundle = TraductionServiceUtil.getI18nResourceBundle();
        parameters.put(FROM_PARAM, i18nResourceBundle.getString(FROM_PARAM));
        parameters.put(TO_PARAM, i18nResourceBundle.getString(TO_PARAM));
        parameters.put(AT_PARAM, i18nResourceBundle.getString(AT_PARAM));
        parameters.put(THE_PARAM, i18nResourceBundle.getString(THE_PARAM));
        parameters.put(FISCAL_NUMBER_PARAM, i18nResourceBundle.getString(FISCAL_NUMBER_PARAM));
        parameters.put(COMPANY_ADDRESS_PARAM, i18nResourceBundle.getString(COMPANY_ADDRESS_PARAM));
        parameters.put(COMPANY_PHONE_NUMBER_PARAM, i18nResourceBundle.getString(COMPANY_PHONE_NUMBER_PARAM));
        parameters.put(COMPANY_EMAIL_PARAM, i18nResourceBundle.getString(COMPANY_EMAIL_PARAM));
        parameters.put(COMPANY_WEB_SITE_PARAM, i18nResourceBundle.getString(COMPANY_WEB_SITE_PARAM));
        parameters.put(COMPANY_RC_PARAM, i18nResourceBundle.getString(COMPANY_RC_PARAM));
        parameters.put(STARK_URL_PARAM,i18nResourceBundle.getString(STARK_URL_PARAM));

        if (reportTemplateParams.isProvisionalEdition()) {
            parameters
                    .put(EDITION_PROVISOIRE_PARAM, i18nResourceBundle.getString(EDITION_PROVISOIRE_PARAM));
        } else {
            parameters.put(EDITION_PROVISOIRE_PARAM, EMPTY_STRING);
        }
        parameters.put(COMPANY_NAME_PARAM, reportTemplateParams.getCompanyName());

        parameters.put(FOOTER_PAGE_INFO_PARAM, reportTemplateParams.getCompanyAdressInfo());
        parameters.put(COMPANY_FN, reportTemplateParams.getMatriculeFisc());
        parameters.put(COMPANY_ADDRESS, reportTemplateParams.getCompanyAdressInfo());
        parameters.put(COMPANY_PHONE_NUMBER, reportTemplateParams.getTel());
        parameters.put(COMPANY_LOGO, reportTemplateParams.getLogoDataBase64());
        parameters.put(MAIL_INFO, reportTemplateParams.getMail());
        parameters.put(COMPANY_WEB_SITE, reportTemplateParams.getWebSite());
        parameters.put(COMPANY_RC, reportTemplateParams.getCommercialRegister());

        try {
            parameters.put(FIELD_IMAGE_PATH_NAME,
                    new ClassPathResource(LOGO + DOT_MARK + JPG_EXTENSION).getFile().getAbsolutePath());
        } catch (IOException e) {
            log.error(e.toString());
            throw new HttpCustomException(ApiErrors.Accounting.REPORT_TYPE_INVALID,
                    new ErrorsResponse().error(e));

        }

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(AccountingConstants.DD_MM_YYYY);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(AccountingConstants.HH_MM);

        if (startDate != null && endDate != null) {
            parameters.put(FIELD_NAME_START_DATE, startDate.format(dateFormatter));
            parameters.put(FIELD_NAME_END_DATE, endDate.format(dateFormatter));
        }
        parameters.put(FIELD_NAME_CURRENT_DATE, reportTemplateParams.getGenerationDate().format(dateFormatter));
        parameters.put(FIELD_NAME_CURRENT_TIME, reportTemplateParams.getGenerationDate().format(timeFormatter));
    }

    public static void fillTheMapParametersWithGeneralLedgerInfos(Map<String, Object> parameters) {
        ResourceBundle i18nResourceBundle = TraductionServiceUtil.getI18nResourceBundle();
        parameters.put(REPORT_NAME_PARAM, i18nResourceBundle.getString(GENERAL_LEGEDER));
        parameters.put(DATE_PARAM, i18nResourceBundle.getString(DATE_PARAM));
        parameters.put(JOURNAL_PARAM, i18nResourceBundle.getString(JOURNAL_PARAM));
        parameters.put(LABEL_PARAM, i18nResourceBundle.getString(LABEL_PARAM));
        parameters.put(DOCUMENT_CODE_PARAM, i18nResourceBundle.getString(DOCUMENT_CODE_PARAM));
        parameters.put(DEBIT_PARAM, i18nResourceBundle.getString(DEBIT_PARAM));
        parameters.put(CREDIT_PARAM, i18nResourceBundle.getString(CREDIT_PARAM));
        parameters.put(BALANCE_PARAM, i18nResourceBundle.getString(BALANCE_PARAM));
    }

    public static void fillTheMapParametersWithTrialBalanceInfos(Map<String, Object> parameters) {
        ResourceBundle i18nResourceBundle = TraductionServiceUtil.getI18nResourceBundle();
        parameters.put(REPORT_NAME_PARAM, i18nResourceBundle.getString(TRIAL_BALANCE));
        parameters.put(ACCOUNT_CODE_PARAM, i18nResourceBundle.getString(ACCOUNT_CODE_PARAM));
        parameters.put(ACCOUNT_LABEL_PARAM, i18nResourceBundle.getString(ACCOUNT_LABEL_PARAM));
        parameters.put(DEBIT_PARAM, i18nResourceBundle.getString(DEBIT_PARAM));
        parameters.put(CREDIT_PARAM, i18nResourceBundle.getString(CREDIT_PARAM));
        parameters.put(CUMULATIVE_PERIOD_PARAM, i18nResourceBundle.getString(CUMULATIVE_PERIOD_PARAM));
        parameters.put(BALANCE_PARAM, i18nResourceBundle.getString(BALANCE_PARAM));
    }

    public static void fillTheMapParametersWithCentralizingJournalInfos(Map<String, Object> parameters) {
        ResourceBundle i18nResourceBundle = TraductionServiceUtil.getI18nResourceBundle();
        parameters.put(REPORT_NAME_PARAM, i18nResourceBundle.getString(PDF_NAME_CENTRALIZING_JOURNAL));
        parameters.put(PLAN_CODE_PARAM, i18nResourceBundle.getString(PLAN_CODE_PARAM));
        parameters.put(PLAN_LABEL_PARAM, i18nResourceBundle.getString(PLAN_LABEL_PARAM));
        parameters.put(DEBIT_PARAM, i18nResourceBundle.getString(DEBIT_PARAM));
        parameters.put(CREDIT_PARAM, i18nResourceBundle.getString(CREDIT_PARAM));
        parameters.put("journalLabel", i18nResourceBundle.getString("journalLabel"));
        parameters.put("jle", i18nResourceBundle.getString("jle"));
        parameters.put("month", i18nResourceBundle.getString("month"));
    }

    public static void fillTheMapParametersWithStateOfJournal(Map<String, Object> parameters) {
        ResourceBundle i18nResourceBundle = TraductionServiceUtil.getI18nResourceBundle();
        parameters.put(REPORT_NAME_PARAM, i18nResourceBundle.getString(STATE_JOURNAL));
        parameters.put(CODE_PARAM, i18nResourceBundle.getString(CODE_PARAM));
        parameters.put(DATE_PARAM, i18nResourceBundle.getString(DATE_PARAM));
        parameters.put(LABEL_PARAM, i18nResourceBundle.getString(LABEL_PARAM));
        parameters.put(AMOUNT_PARAM, i18nResourceBundle.getString(AMOUNT_PARAM));
    }

    public static void fillTheMapParametersWithAuxiliaryJournalInfos(Map<String, Object> parameters) {
        ResourceBundle i18nResourceBundle = TraductionServiceUtil.getI18nResourceBundle();
        parameters.put(REPORT_NAME_PARAM, i18nResourceBundle.getString(AUXILIARY_JOURNAL));
        parameters.put(BILLING_DATE_PARAM, i18nResourceBundle.getString(BILLING_DATE_PARAM));
        parameters.put(DOCUMENT_CODE_PARAM, i18nResourceBundle.getString(DOCUMENT_CODE_PARAM));
        parameters.put(DOCUMENT_DATE_PARAM, i18nResourceBundle.getString(DOCUMENT_DATE_PARAM));
        parameters.put(ACCOUNT_CODE_PARAM, i18nResourceBundle.getString(ACCOUNT_CODE_PARAM));
        parameters.put(DOCUMENT_LABEL_PARAM, i18nResourceBundle.getString(DOCUMENT_LABEL_PARAM));
        parameters.put(DEBIT_PARAM, i18nResourceBundle.getString(DEBIT_PARAM));
        parameters.put(CREDIT_PARAM, i18nResourceBundle.getString(CREDIT_PARAM));
    }

    public static void fillTheMapParametersWithBalanceSheetParts(Map<String, Object> parameters,
                                                                 String balanceSheetPartsReportTitle) {
        ResourceBundle i18nResourceBundle = TraductionServiceUtil.getI18nResourceBundle();
        parameters.put(REPORT_NAME_PARAM, i18nResourceBundle.getString(balanceSheetPartsReportTitle));
        parameters.put(BALANCE_SHEET_PARAM, i18nResourceBundle.getString(BALANCE_SHEET_PARAM));
        parameters.put(ANNEX_PARAM, i18nResourceBundle.getString(ANNEX_PARAM));
        parameters
                .put(EQUITY_AND_LIABILITIES_PARAM, i18nResourceBundle.getString(EQUITY_AND_LIABILITIES_PARAM));
        parameters.put(BALANCE_SHEET_ASSETS_PARAM, i18nResourceBundle.getString(BALANCE_SHEET_ASSETS));
        parameters.put(NON_CURRENT_ASSET_PARAM, i18nResourceBundle.getString(NON_CURRENT_ASSET_PARAM));
        parameters.put(LIABILITIES_PARAM, i18nResourceBundle.getString(LIABILITIES_PARAM));
        parameters.put(CURRENT_LIABILITIES_PARAM, i18nResourceBundle.getString(CURRENT_LIABILITIES_PARAM));
        parameters.put(EQUITY_PARAM, i18nResourceBundle.getString(EQUITY_PARAM));
        parameters.put(TOTAL_EQUITY_AND_LIABILITIES_PARAM,
                i18nResourceBundle.getString(TOTAL_EQUITY_AND_LIABILITIES_PARAM));
        parameters.put(NON_CURRENT_LIABILITIES_PARAM,
                i18nResourceBundle.getString(NON_CURRENT_LIABILITIES_PARAM));
        parameters.put(CURRENT_ASSET_PARAM, i18nResourceBundle.getString(CURRENT_ASSET_PARAM));
        parameters.put(OTHER_CURRENT_ASSET_PARAM, i18nResourceBundle.getString(OTHER_CURRENT_ASSET_PARAM));
    }

    public static void fillTheMapParametersWithBilanAnnex(Map<String, Object> parameters) {
        ResourceBundle i18nResourceBundle = TraductionServiceUtil.getI18nResourceBundle();
        parameters.put(REPORT_NAME_PARAM, i18nResourceBundle.getString(BILAN_ANNEXE));
        parameters.put(ANNEX_PARAM, i18nResourceBundle.getString(ANNEX_PARAM));
        parameters.put(LABEL_PARAM, i18nResourceBundle.getString(LABEL_PARAM));
        parameters.put(ACCOUNT_CODE_PARAM, i18nResourceBundle.getString(ACCOUNT_CODE_PARAM));
        parameters.put(ACCOUNT_LABEL_PARAM, i18nResourceBundle.getString(ACCOUNT_LABEL_PARAM));
        parameters.put(DEBIT_PARAM, i18nResourceBundle.getString(DEBIT_PARAM));
        parameters.put(CREDIT_PARAM, i18nResourceBundle.getString(CREDIT_PARAM));
        parameters.put(TOTAL_BALANCE_PARAM, i18nResourceBundle.getString(TOTAL_BALANCE_PARAM));
    }

    public static void fillTheMapParametersWithStateOfIncomeAnnex(Map<String, Object> parameters) {
        ResourceBundle i18nResourceBundle = TraductionServiceUtil.getI18nResourceBundle();
        parameters.put(REPORT_NAME_PARAM, i18nResourceBundle.getString(STATE_OF_INCOME_ANNEX));
        parameters.put(ANNEX_PARAM, i18nResourceBundle.getString(ANNEX_PARAM));
        parameters.put(LABEL_PARAM, i18nResourceBundle.getString(LABEL_PARAM));
        parameters.put(ACCOUNT_CODE_PARAM, i18nResourceBundle.getString(ACCOUNT_CODE_PARAM));
        parameters.put(ACCOUNT_LABEL_PARAM, i18nResourceBundle.getString(ACCOUNT_LABEL_PARAM));
        parameters.put(DEBIT_PARAM, i18nResourceBundle.getString(DEBIT_PARAM));
        parameters.put(CREDIT_PARAM, i18nResourceBundle.getString(CREDIT_PARAM));
        parameters.put(TOTAL_BALANCE_PARAM, i18nResourceBundle.getString(TOTAL_BALANCE_PARAM));
    }

    public static void fillTheMapParametersWithCashFlowAuthorizedAnnex(Map<String, Object> parameters) {
        ResourceBundle i18nResourceBundle = TraductionServiceUtil.getI18nResourceBundle();
        parameters.put(REPORT_NAME_PARAM, i18nResourceBundle.getString(CASH_FLOW_AUTHORIZED_ANNEX));
        parameters.put(ANNEX_PARAM, i18nResourceBundle.getString(ANNEX_PARAM));
        parameters.put(LABEL_PARAM, i18nResourceBundle.getString(LABEL_PARAM));
        parameters.put(ACCOUNT_CODE_PARAM, i18nResourceBundle.getString(ACCOUNT_CODE_PARAM));
        parameters.put(ACCOUNT_LABEL_PARAM, i18nResourceBundle.getString(ACCOUNT_LABEL_PARAM));
        parameters.put(DEBIT_PARAM, i18nResourceBundle.getString(DEBIT_PARAM));
        parameters.put(CREDIT_PARAM, i18nResourceBundle.getString(CREDIT_PARAM));
        parameters.put(TOTAL_BALANCE_PARAM, i18nResourceBundle.getString(TOTAL_BALANCE_PARAM));
    }

    public static void fillTheMapParametersWithCashFlowAuthorized(Map<String, Object> parameters) {
        ResourceBundle i18nResourceBundle = TraductionServiceUtil.getI18nResourceBundle();
        parameters.put(REPORT_NAME_PARAM, i18nResourceBundle.getString(CASH_FLOW_AUTHORIZED));
        parameters.put(ANNEX_PARAM, i18nResourceBundle.getString(ANNEX_PARAM));
        parameters.put(CASH_FLOW_STATEMENT_PARAM, i18nResourceBundle.getString(CASH_FLOW_STATEMENT_PARAM));
    }

    public static void fillTheMapParametersWithIntermediaryBalance(Map<String, Object> parameters) {
        ResourceBundle i18nResourceBundle = TraductionServiceUtil.getI18nResourceBundle();
        parameters.put(REPORT_NAME_PARAM, i18nResourceBundle.getString(INTERMEDIARY_BALANCE_NAME));
        parameters.put(INDUSTRIAL_ACTIVITY_PARAM, i18nResourceBundle.getString(INDUSTRIAL_ACTIVITY_PARAM));
        parameters.put(COMMERCIAL_ACTIVITY_PARAM, i18nResourceBundle.getString(COMMERCIAL_ACTIVITY_PARAM));
    }

    public static void fillTheMapParametersWithReconciliationBankInfos(Map<String, Object> parameters,
                                                                       AccountDto accountdto, String period, String initialAmountCredit, String finalAmountCredit,
                                                                       String initialAmountDebit, String finalAmountDebit) {
        parameters.put("accountName", accountdto.getLabel());
        parameters.put("code", accountdto.getCode());
        parameters.put("period", period);
        parameters.put("initialAmountCredit",
                CalculationUtil.getFormattedBigDecimalValue(new BigDecimal(initialAmountCredit)));
        parameters.put("finalAmountCredit",
                CalculationUtil.getFormattedBigDecimalValue(new BigDecimal(finalAmountCredit)));
        parameters.put("initialAmountDebit",
                CalculationUtil.getFormattedBigDecimalValue(new BigDecimal(initialAmountDebit)));
        parameters.put("finalAmountDebit",
                CalculationUtil.getFormattedBigDecimalValue(new BigDecimal(finalAmountDebit)));

        ResourceBundle i18nResourceBundle = TraductionServiceUtil.getI18nResourceBundle();
        parameters.put(REPORT_NAME_PARAM, i18nResourceBundle.getString(RECONCILIATION_BANK));
        parameters.put(DOCUMENT_CODE_PARAM, i18nResourceBundle.getString(DOCUMENT_CODE_PARAM));
        parameters.put(DOCUMENT_DATE_PARAM, i18nResourceBundle.getString(DOCUMENT_DATE_PARAM));
        parameters.put(REFERENCE_PARAM, i18nResourceBundle.getString(REFERENCE_PARAM));
        parameters.put(LABEL_PARAM, i18nResourceBundle.getString(LABEL_PARAM));
        parameters.put(DEBIT_PARAM, i18nResourceBundle.getString(DEBIT_PARAM));
        parameters.put(CREDIT_PARAM, i18nResourceBundle.getString(CREDIT_PARAM));
        parameters.put(ACCOUNT_CODE_PARAM, i18nResourceBundle.getString(ACCOUNT_CODE_PARAM));
    }

    public static void fillTheMapParametersWithReconciliationBankStatementInfos(Map<String, Object> parameters,
                                                                                AccountDto accountdto, String period) {
        parameters.put("accountName", accountdto.getLabel());
        parameters.put("code", accountdto.getCode());
        parameters.put("period", period);
        ResourceBundle i18nResourceBundle = TraductionServiceUtil.getI18nResourceBundle();
        parameters.put(REPORT_NAME_PARAM, i18nResourceBundle.getString(RECONCILIATION_BANK_STATEMENT));
        parameters.put(DOCUMENT_CODE_PARAM, i18nResourceBundle.getString(DOCUMENT_CODE_PARAM));
        parameters.put(DOCUMENT_DATE_PARAM, i18nResourceBundle.getString(DOCUMENT_DATE_PARAM));
        parameters.put(REFERENCE_PARAM, i18nResourceBundle.getString(REFERENCE_PARAM));
        parameters.put(LABEL_PARAM, i18nResourceBundle.getString(LABEL_PARAM));
        parameters.put(DEBIT_PARAM, i18nResourceBundle.getString(DEBIT_PARAM));
        parameters.put(CREDIT_PARAM, i18nResourceBundle.getString(CREDIT_PARAM));
        parameters.put(ACCOUNT_CODE_PARAM, i18nResourceBundle.getString(ACCOUNT_CODE_PARAM));
    }

    public static void fillTheMapParametersWithStateOfIncome(Map<String, Object> parameters) {
        ResourceBundle i18nResourceBundle = TraductionServiceUtil.getI18nResourceBundle();
        parameters.put(REPORT_NAME_PARAM, i18nResourceBundle.getString(STATE_OF_INCOME));
        parameters.put(STATE_OF_INCOME, i18nResourceBundle.getString(STATE_OF_INCOME));
        parameters.put(ANNEX_PARAM, i18nResourceBundle.getString(ANNEX_PARAM));
        parameters.put(OPERATING_INCOMES, i18nResourceBundle.getString(OPERATING_INCOMES));
        parameters.put(INCOMES, i18nResourceBundle.getString(INCOMES));
        parameters.put(OTHER_OPERATING_PRODUCTS, i18nResourceBundle.getString(OTHER_OPERATING_PRODUCTS));
        parameters.put(OPERATING_EXPENSES, i18nResourceBundle.getString(OPERATING_EXPENSES));
        parameters.put(EXTRAORDINARY_ITEMS, i18nResourceBundle.getString(EXTRAORDINARY_ITEMS));
    }

    public static void fillTheMapParametersWithCashFlowOfReference(Map<String, Object> parameters) {
        ResourceBundle i18nResourceBundle = TraductionServiceUtil.getI18nResourceBundle();
        parameters.put(REPORT_NAME_PARAM, i18nResourceBundle.getString(CASH_FLOW_OF_REFERENCE));
        parameters.put(ANNEX_PARAM, i18nResourceBundle.getString(ANNEX_PARAM));
        parameters.put(CASH_FLOW_STATEMENT_PARAM, i18nResourceBundle.getString(CASH_FLOW_STATEMENT_PARAM));
    }

    public static void fillTheMapParametersWithCashFlowOfReferenceAnnex(Map<String, Object> parameters) {
        ResourceBundle i18nResourceBundle = TraductionServiceUtil.getI18nResourceBundle();
        parameters.put(REPORT_NAME_PARAM, i18nResourceBundle.getString(CASH_FLOW_OF_REFERENCE_ANNEX));
        parameters.put(ANNEX_PARAM, i18nResourceBundle.getString(ANNEX_PARAM));
        parameters.put(LABEL_PARAM, i18nResourceBundle.getString(LABEL_PARAM));
        parameters.put(ACCOUNT_CODE_PARAM, i18nResourceBundle.getString(ACCOUNT_CODE_PARAM));
        parameters.put(ACCOUNT_LABEL_PARAM, i18nResourceBundle.getString(ACCOUNT_LABEL_PARAM));
        parameters.put(DEBIT_PARAM, i18nResourceBundle.getString(DEBIT_PARAM));
        parameters.put(CREDIT_PARAM, i18nResourceBundle.getString(CREDIT_PARAM));
        parameters.put(TOTAL_BALANCE_PARAM, i18nResourceBundle.getString(TOTAL_BALANCE_PARAM));
    }

    public static void fillTheMapParametersWithAmortizationTable(Map<String, Object> parameters) {
        ResourceBundle i18nResourceBundle = TraductionServiceUtil.getI18nResourceBundle();
        parameters.put(REPORT_NAME_PARAM, i18nResourceBundle.getString(AMORTIZATION_TABLE));
        parameters.put(DESIGNATION, i18nResourceBundle.getString(DESIGNATION));
        parameters.put(RELEASE_DATE, i18nResourceBundle.getString(RELEASE_DATE));
        parameters.put(ACQUISITION_VALUE, i18nResourceBundle.getString(ACQUISITION_VALUE));
        parameters.put(PREVIOUS_DEPRECIATION, i18nResourceBundle.getString(PREVIOUS_DEPRECIATION));
        parameters.put(ANNUITY_OF_FISCAL_YEAR, i18nResourceBundle.getString(ANNUITY_OF_FISCAL_YEAR));
        parameters.put(VCN, i18nResourceBundle.getString(VCN));
    }

}
