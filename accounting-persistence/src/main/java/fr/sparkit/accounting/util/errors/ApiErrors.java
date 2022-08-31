package fr.sparkit.accounting.util.errors;

public final class ApiErrors {

    private ApiErrors() {
        super();
    }

    public static final class Accounting {
        /* Naming convention : [ENTITY]_[SHORT_DESCRIPTION] */
        /* Generic Accounting codes [20000-20099] */
        public static final int ENTITY_NOT_FOUND = 20000;
        public static final int LABEL_MIN_LENGTH = 20001;
        public static final int ENTITY_REFERENCED = 20002;
        public static final int ENTITY_FIELD_NOT_VALID = 20003;
        public static final int INVALID_FORMAT_EXCEPTION = 20004;
        public static final int HTTP_MESSAGE_NOT_READABLE_EXCEPTION = 20005;
        public static final int EXCEPTION_DETECTED = 20006;
        public static final int TRYING_TO_SORT_USING_NON_EXISTENT_FIELD = 20007;
        public static final int NO_COMPANY_SPECIFIED_CANT_CONNECT_TO_DATA_SOURCE = 20008;
        public static final int INVALID_COMPANY_SPECIFIED_CANT_CONNECT_TO_DATA_SOURCE = 20009;
        public static final int EMPTY_COMPANIES_LIST_IN_APPLICATION_PROPERTIES = 20010;
        public static final int FIELD_NOT_ANNOTATED_WITH_EXCEL_CELL = 20011;
        public static final int NO_LANGUAGE_IS_SPECIFIED = 20012;
        public static final int FIELD_TYPE_NOT_COMPARABLE = 20013;
        public static final int NO_USER_IS_SPECIFIED = 20014;

        /* DocumentAccount codes [20100-20199] */
        public static final int DOCUMENT_ACCOUNT_AMOUNT_CODE = 20100;
        public static final int DOCUMENT_ACCOUNT_WITHOUT_LINES = 20101;
        public static final int DOCUMENT_ACCOUNT_NO_FISCAL_YEAR = 20102;
        public static final int DOCUMENT_ACCOUNT_MISSING_PARAMETERS = 20103;
        public static final int DOCUMENT_ACCOUNT_INVALID = 20104;
        public static final int DOCUMENT_ACCOUNT_DATE_IN_CLOSED_PERIOD = 20105;
        public static final int DOCUMENT_ACCOUNT_INVALID_DATE = 20106;
        public static final int DOCUMENT_ACCOUNT_CANT_HAVE_MULTIPLE_JOURNAL_ANEW_DOCUMENTS = 20107;
        public static final int BILL_DATE_AFTER_CURRENT_DATE = 20108;
        public static final int DOCUMENT_ACCOUNT_NON_EXISTENT = 20109;
        public static final int DOCUMENT_ACCOUNT_UPLOAD_DOCUMENT_ACCOUNT_ATTACHEMENT_FAIL = 20110;
        public static final int DOCUMENT_ACCOUNT_ATTACHEMENT_NOT_FOUND = 20111;
        public static final int DOCUMENT_ACCOUNT_ATTACHEMENT_ERROR_LOAD = 20112;
        public static final int DOCUMENT_ACCOUNT_ATTACHEMENT_ERROR_DELETING = 20112;
        public static final int DOCUMENT_ACCOUNT_COMING_FROM_CLOSING_FISCAL_YEAR_CANNOT_BE_DELETED = 20113;
        public static final int DOCUMENT_ACCOUNT_COMING_FROM_CLOSING_FISCAL_YEAR_CANNOT_BE_MANUALLY_UPDATED = 20114;
        public static final int DOCUMENT_ACCOUNT_COMING_FROM_BILL_CANNOT_BE_DELETED = 20115;
        public static final int DOCUMENT_ACCOUNT_COMING_FROM_A_BILL_CANNOT_BE_MANUALLY_UPDATED = 20116;
        public static final int DOCUMENT_ACCOUNT_CANT_DELETE_DOCUMENT_IN_CLOSED_PERIOD = 20117;
        public static final int DOCUMENT_ACCOUNT_LINE_NOT_FOUND = 20118;
        public static final int ENTITY_DOCUMENT_ACCOUNT_NOT_FOUND = 20119;
        public static final int DOCUMENT_ACCOUNT_CANT_UPDATE_DOCUMENT_IN_CLOSED_PERIOD = 20120;
        public static final int DOCUMENT_ACCOUNT_CODE_EXISTS = 20121;
        public static final int DOCUMENT_ACCOUNT_CONTAINS_RECONCILABLE_LINES_CANNOT_BE_DELETED = 20122;
        public static final int DOCUMENT_ACCOUNT_CONTAINS_LETTERED_LINES_CANNOT_BE_DELETED = 20123;
        public static final int MONTH_NOT_RECONCILED = 20124;
        public static final int DOCUMENT_ACCOUNT_LINE_ACCOUNT_DOES_NOT_EXIST = 20125;
        public static final int DOCUMENT_ACCOUNT_FROM_AMORTIZATION_NOT_GENERATED_YET = 20126;
        public static final int NO_ACTIVE_ASSET_IN_CURRENT_FISCAL_YEAR = 20127;
        public static final int DOCUMENT_ACCOUNT_FROM_BILL_AMOUNT_TTC_EQUAL_ZERO_CANNOT_BE_GENERATED = 20128;


        /* ChartAccount codes [20200-20299] */
        public static final int CHART_ACCOUNT_PARENT_CHART_ACCOUNT_DONT_EXIST = 20200;
        public static final int CHART_ACCOUNT_CODE_EXISTS = 20201;
        public static final int CHART_ACCOUNT_LABEL_EXISTS = 20202;
        public static final int CHART_ACCOUNT_CODE_AND_LABEL_EXIST = 20203;
        public static final int CHART_ACCOUNT_MISSING_PARAMETERS = 20204;
        public static final int CHART_ACCOUNT_INEXISTANT = 20205;
        public static final int CHART_ACCOUNT_ALREADY_USED_CANT_CHANGE_CODE = 20206;
        public static final int CHART_ACCOUNT_ALREADY_USED_CANT_DELETE = 20207;

        public static final int CHART_ACCOUNT_MAX_CODE_EXCEEDED = 20208;

        /* Account codes [20300-20399] */
        public static final int ACCOUNT_CODE_EXISTS = 20300;
        public static final int ACCOUNT_CREDIT_DEBIT_IS_DIFFERENT = 20301;
        public static final int ACCOUNT_CODE_DIFFERENT_THAN_PARENT = 20302;
        public static final int ACCOUNT_NEGATIVE_CREDIT_OR_DEBIT = 20303;
        public static final int ACCOUNT_COULD_NOT_BE_CREATED = 20304;
        public static final int ACCOUNT_MISSING_PARAMETERS = 20305;
        public static final int ACCOUNT_CODE_LENGTH_INVALID = 20306;
        public static final int NO_ACCOUNT_PREFIXED_BY_CODE = 20307;
        public static final int NO_OPENING_BALANCE_SHEET_ACCOUNT = 20308;
        public static final int NO_CLOSING_BALANCE_SHEET_ACCOUNT = 20309;
        public static final int ACCOUNT_DOES_NOT_EXIST_ALLOCATION_NOT_POSSIBLE = 20310;
        public static final int ACCOUNT_RELATION_TYPE_INVALID = 20311;
        public static final int ACCOUNT_RELATION_TYPE_DUPLICATES = 20312;
        public static final int ACCOUNT_RELATION_IMPLEMENTATION_NONEXISTENT = 20313;
        public static final int ACCOUNT_SUPPLIER_DOES_NOT_EXIST = 20314;
        public static final int ACCOUNT_CUSTOMER_DOES_NOT_EXIST = 20315;
        public static final int ACCOUNT_TAXE_DOES_NOT_EXIST = 20316;
        public static final int ACCOUNT_IS_USED = 20317;
        public static final int NO_ACCOUNT_WITH_CODE = 20318;
        public static final int ACCOUNT_WITHHOLDING_TAX_DOES_NOT_EXIST = 20319;

        /* Journal codes [20400-20499] */
        public static final int JOURNAL_CODE_EXISTS = 20400;
        public static final int JOURNAL_CODE_LENGTH = 20401;
        public static final int JOURNAL_MISSING_PARAMETERS = 20402;
        public static final int JOURNAL_NO_JOURNAL_A_NEW = 20403;
        public static final int JOURNAL_NOT_FOUND = 20404;
        public static final int JOURNAL_LABEL_EXISTS = 20405;
        public static final int JOURNAL_CONTAINS_CLOSED_LINES = 20406;

        /* FiscalYear codes [20500-20599] */
        public static final int FISCAL_YEAR_INEXISTANT_FISCAL_YEAR = 20501;
        public static final int FISCAL_YEAR_MISSING_PARAMETERS = 20502;
        public static final int FISCAL_YEAR_DATES_OVERLAP_ERROR = 20503;
        public static final int FISCAL_YEAR_DATES_ORDER_INVALID = 20504;
        public static final int FISCAL_YEAR_NAME_EXISTS = 20505;
        public static final int FISCAL_YEAR_CLOSED = 20506;
        public static final int FISCAL_YEAR_CLOSING_DATE_NULL = 20507;
        public static final int FISCAL_YEAR_CLOSING_INTERVAL_OVERLAP_WITH_ALREADY_CLOSED_ONE = 20508;
        public static final int FISCAL_YEAR_CLOSED_PERIOD_INEXISTANT = 20509;
        public static final int FISCAL_YEAR_NONEXISTENT_PREVIOUS_FISCAL_YEAR = 20510;
        public static final int FISCAL_YEAR_PREVIOUS_FISCAL_YEAR_HAS_NO_NON_LETTERED_DOCUMENT_ACCOUNT_LINES = 20511;
        public static final int FISCAL_YEAR_NOT_ALL_DOCUMENTS_IN_NEW_PERIOD = 20512;
        public static final int TARGET_FISCAL_YEAR_IS_CLOSED = 20513;
        public static final int PREVIOUS_FISCAL_YEARS_NOT_ALL_CONCLUDED = 20514;
        public static final int CURRENT_FISCAL_YEAR_IS_NOT_CLOSED = 20515;
        public static final int START_DATE_NOT_VALID = 20516;
        public static final int TARGET_FISCAL_YEAR_NON_EXISTENT = 20517;
        public static final int CURRENT_FISCAL_YEAR_NON_EXISTENT = 20518;
        public static final int RESULT_ACCOUNT_NON_EXISTENT = 20519;
        public static final int TARGET_FISCAL_YEAR_NOT_AFTER_SELECTED_FISCAL_YEAR = 20520;
        public static final int UPDATING_FISCAL_YEAR_THAT_IS_NOT_OPENED = 20521;
        public static final int UPDATING_FISCAL_YEAR_THAT_IS_NOT_LAST = 20522;
        public static final int DOCUMENT_ACCOUNT_NOT_IN_FISCAL_YEAR = 20523;
        public static final int FISCAL_YEAR_NOT_OPENED_OPERATION_NOT_ALLOWED = 20524;
        public static final int JOURNAL_A_NEW_TO_REMOVE_CONTAINS_LETTERED_LINES = 20525;
        public static final int FISCAL_YEAR_CLOSING_DATE_BEFORE_END_DATE = 20526;
        public static final int UPDATING_FISCAL_YEAR_THAT_IS_CONCLUDED = 20527;
        public static final int NO_FISCAL_YEARS_FOUND = 20528;

        /* AccountingConfiguration codes [20600-20699] */
        public static final int ACCOUNTING_CONFIGURATION_NO_CONFIGURATION_FOUND = 20600;
        public static final int ACCOUNTING_CONFIGURATION_ACCOUNTS_NOT_FOUND = 20601;
        public static final int ACCOUNTING_CONFIGURATION_FISCAL_YEAR_NOT_FOUND = 20602;
        public static final int ACCOUNTING_CONFIGURATION_MISSING_PARAMETERS = 20603;
        public static final int ACCOUNTING_CONFIGURATION_CATEGORY_NOT_FOUND = 20604;
        public static final int ACCOUNTING_CONFIGURATION_DEPRECITION_PERIOD_UNAFFECTED = 20605;
        public static final int ACCOUNTING_CONFIGURATION_CHART_ACCOUNTS_NOT_FOUND = 20606;
        public static final int ACCOUNTING_CONFIGURATION_RESULT_ACCOUNT_NOT_NOT_FOUND = 20607;

        /* Reporting codes [20700-20799] */
        public static final int START_DATE_IS_AFTER_END_DATE = 20701;
        public static final int BEGIN_ACCOUNT_CODE_IS_GREATER_THAN_END_ACCOUNT = 20702;

        /* TemplateAccounting codes [20800-20899] */
        public static final int TEMPLATE_ACCOUNTING_WITHOUT_LINES_CODE = 20800;
        public static final int TEMPLATE_ACCOUNTING_MISSING_PARAMETERS = 20801;
        public static final int TEMPLATE_ACCOUNTING_LABEL_EXISTS = 20802;

        /* BillDocument codes [20900-20920] */
        public static final int BILL_ALREADY_IMPORTED = 20900;
        public static final int BILL_SAVE_ERROR = 20901;
        public static final int BILL_DATE_NOT_IN_FISCAL_YEAR = 20902;
        public static final int NULL_BILL_ACCOUNT_ID = 20903;
        public static final int NULL_VAT_ACCOUNT_ID = 20904;
        public static final int NULL_TIER_ACCOUNT_ID = 20905;
        public static final int NULL_ARTICLE_ACCOUNT_ID = 20906;
        public static final int NULL_BILL_ID = 20907;

        /* Accounting Lettering codes [20921-20940] */
        public static final int YOU_MUST_CHOOSE_FOR_THE_THE_SAME_LETTER_SINGLE_ACCOUNT = 20921;
        public static final int TOTAL_DEBIT_SHOULD_BE_EQUAL_TO_TOTAL_CREDIT_FOR_ACCOUNT_AND_LETTER = 20922;
        public static final int CHOSEN_LETTERING_CODE_ALREADY_EXISTS = 20923;
        public static final int LAST_CODE_REACHED = 20924;
        public static final int LETTERING_OPERATION_IN_CLOSED_PERIOD = 20925;

        /* Accounting Report Line codes [20941-20960] */
        public static final int REPORT_LINE_INEXISTANT_REPORT_LINE = 20941;
        public static final int REPORT_LINE_INVALID_FORMULA = 20942;
        public static final int REPORT_LINE_STATE_OF_INCOME_FIELDS_MISMATCH = 20943;
        public static final int REPORT_LINE_INDEX_LINE_NOT_FOUND = 20944;
        public static final int REPORT_LINE_FORMULA_CONTAINS_REPETITION = 20945;
        public static final int REPORT_LINE_ASSETS_NOT_FOUND = 20946;
        public static final int REPORT_TYPE_INVALID = 20947;
        public static final int REPORT_LINE_INDEX_LINE_ORDER_INVALID = 20948;
        public static final int REPORT_LINE_NO_DEFAULT_REPORT_CONFIGURATION = 20949;
        public static final int REPORT_LINE_NO_DEFAULT_REPORT_CONFIGURATION_WITH_INDEX_FOR_THIS_REPORT_TYPE = 20950;
        public static final int REPORT_LINE_ANNEX_ALREADY_EXISTS = 20951;
        public static final int ACCOUNT_NOT_BALANCED = 20952;
        public static final int REPORT_LINE_CANNOT_UPDATE_WHEN_FISCAL_YEAR_NOT_OPENED = 20953;
        public static final int NO_ANNEX_REPORT_SUPPORTED_FOR_THIS_REPORT_TYPE = 20954;
        public static final int ERROR_JASPER_FILE_GENERATION = 20955;

        /* Accounting GeneralLedger codes [20961-20980] */
        public static final int END_AMOUNT_LESS_THAN_BEGIN_AMOUNT = 20961;
        public static final int BEGIN_AMOUNT_OR_END_AMOUNT_FORMAT_INCORRECT = 20962;

        /* Accounting Exporting/importing data to/from excel file [20981-21099] */
        public static final int EXCEL_FILE_CREATION_FAIL = 20981;
        public static final int EXPORT_COULD_NOT_CREATE_DIRECTORY_FOR_FILES = 20982;
        public static final int EXCEL_ERROR_WHILE_READING_FILE = 20983;
        public static final int EXCEL_ERROR_DOWNLOADING_THE_FILE = 20984;
        public static final int EXCEL_INVALID_HEADERS = 20985;
        public static final int EXCEL_EMPTY_FILE = 20986;
        public static final int EXCEL_INVALID_CONTENT_FORMAT = 20987;
        public static final int EXCEL_INVALID_ROW = 20988;
        public static final int EXCEL_FIRST_ROW_SHOULD_CONTAIN_DOCUMENT_INFORMATION = 20989;
        public static final int EXCEL_HEADER_SHOULD_BE_IN_FIRST_ROW = 20990;
        public static final int EXCEL_HEADER_SHOULD_START_IN_FIRST_CELL = 20991;
        public static final int EXCEL_NO_FOLDER_FOR_GENERATED_REPORTS = 20992;
        public static final int EXCEL_FILE_LOCKED_BY_PASSWORD = 20993;
        public static final int EXCEL_FILE_NOT_FOUND = 20994;
        public static final int EXCEL_NO_CHART_ACCOUNTS_TO_BE_SAVED = 20995;
        public static final int EXCEL_NO_DOCUMENT_ACCOUNTS_TO_BE_SAVED = 20996;
        public static final int EXCEL_OLD_FORMAT_NOT_SUPPORTED = 20997;
        public static final int EXCEL_OOXML_FORMAT_NOT_SUPPORTED = 20998;
        public static final int EXCEL_NO_JOURNALS_TO_BE_SAVED = 20999;
        public static final int EXCEL_NO_ACCOUNTS_TO_BE_SAVED = 21000;
        public static final int EXCEL_NO_ACCOUNTING_TEMPLATES_TO_BE_SAVED = 21001;
        public static final int EXCEL_REPORT_TEMPLATES_NOT_FOUND = 21002;
        public static final int EXCEL_ERROR_PARSING_LOCAL_DATE_OBJECT = 21003;

        /* Accounting AmortizationTable codes [21100-21199] */
        public static final int ASSETS_OUT_OF_SERVICE = 21101;
        public static final int DATE_CESSION_AFTER_DATE_COMMISSIONING = 21102;
        public static final int DEPRECIATION_ASSETS_NOT_ACCOUNTED = 21103;
        public static final int AMORTIZATION_OF_ASSETS_NOT_FOUND = 21104;
        public static final int DEPRECIATION_ASSETS_FIELD_EMPTY = 21105;
        public static final int DATE_CESSION_NULL = 21106;
        public static final int RESOURCE_NOT_FOUND = 21107;
        public static final int DATE_CESSION_OUT_OF_SERVICE = 21108;

        /* Accounting Template codes [21200-21299] */
        public static final int DOCUMENT_ACCOUNT_LINE_WITH_BOTH_DEBIT_AND_CREDIT = 21200;

        public static final String INVOICE_SALES = "I-SA";
        public static final String SALES_ASSETS = "IA-SA";
        public static final String INVOICE_PURCHASES = "I-PU";
        public static final String PURCHASES_ASSETS = "A-PU";
        /* Accounting User Journal codes [22200-22299] */
        public static final int USER_NOT_FOUND = 21201;
        public static final int USER_HAS_A_JOURNAL = 21202;

        private Accounting() {
        }
    }

}
