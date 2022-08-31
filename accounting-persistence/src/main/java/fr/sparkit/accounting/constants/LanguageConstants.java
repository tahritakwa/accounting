package fr.sparkit.accounting.constants;

public final class LanguageConstants {
    public static final String REPORT_NAME_TITLE_AUXILIARY_JOURNAL = "Journaux auxiliaires";
    public static final String REPORT_NAME_TITLE_BILAN_ANNEXE = "Bilan annexe";
    public static final String REPORT_NAME_TITLE_GENERAL_BALANCE = "Balance Générale";
    public static final String REPORT_NAME_TITLE_RECONCILIATION_BANK_STATEMENT = "État rapprochement bancaire";
    public static final String REPORT_NAME_TITLE_RECONCILIATION_BANK = "Rapprochement bancaire";
    public static final String REPORT_NAME_TITLE_CENTRALIZING_JOURNAL = "Journal Centralisateur";
    public static final String REPORT_NAME_TITLE_STATE_OF_INCOME = "État des résultats";
    public static final String REPORT_NAME_TITLE_GENERAL_LEGEDER = "Grand Livre";
    public static final String REPORT_NAME_TITLE_BALANCE_SHEET = "Bilan";
    public static final String REPORT_NAME_TITLE_INTERMEDIARY_BALANCE = "État des soldes intermédiaires de gestion";
    public static final String TOTAL = "Total";
    public static final String MONTH = "Mois";
    public static final String TOTAL_MONTH = "Total Mois";
    public static final String TOTAL_JOURNAL = "Total Journal";
    public static final String GENERAL_TOTAL = "Total Général";
    public static final String JOURNAL = "Journal";
    public static final String REPORT_NAME_TITLE_ANNEXE_STATE_OF_INCOME = "État des résultats annexe";
    public static final String DOCUMENT_ACCOUNT_HEADERS_TOOLTIP_COMMENT = "Entête de la pièce comptable\n(unique par pièce)";
    public static final String ACCOUNTING_TEMPLATE_HEADERS_TOOLTIP_COMMENT = "Entête de la maquette comptable\n(unique par maquette)";
    public static final String REPORT_NAME_TITLE_STATE_OF_JOURNAL = "État des journaux";
    public static final String REPORT_NAME_TITLE_AMORTIZATION_TABLE = "Tableau d'amortissement";
    public static final String REPORT_NAME_TITLE_FLUX_CASH_FLOW = "Flux de trésorerie";
    public static final String REPORT_NAME_TITLE_CASH_FLOW_ANNEX = "Flux de trésorerie annexe";
    public static final String REPORT_NAME_TITLE_FLUX_CASH_FLOW_AUTHORIZED = "Flux de trésorerie-autorisé";
    public static final String REPORT_NAME_TITLE_CASH_FLOW_ANNEX_AUTHORIZED = "Flux de trésorerie annexe-autorisé";
    public static final String TRUE = "VRAI";
    public static final String FALSE = "FAUX";
    public static final String BOOLEAN_OPTIONS_TOOLTIP_COMMENT = "Valeurs acceptées : VRAI, FAUX";
    public static final String JOURNAL_SHEET_NAME = "Journaux";
    public static final String ACCOUNT_SHEET_NAME = "Comptes comptables";
    public static final String ACCOUNTING_TEMPLATE_SHEET_NAME = "Maquettes comptables";
    public static final String GENERATION_DATE = "Le %s";
    public static final String GENERATION_TIME = "à %s";
    public static final String PROVISIONAL_EDITION = "Édition provisoire";
    public static final String FISCAL_YEAR_DATE = "Du %s au %s";

    private LanguageConstants() {

    }

    public static final class XLSXHeaders {
        public static final String PLAN_CODE_HEADER_NAME = "Code plan";
        public static final String PARENT_PLAN_CODE_HEADER_NAME = "Plan parent";
        public static final String LABEL_HEADER_NAME = "Libellé";
        public static final String DOCUMENT_DATE_HEADER_NAME = "Date de facturation";
        public static final String DOCUMENT_LABEL_HEADER_NAME = "Libellé document";
        public static final String JOURNAL_HEADER_NAME = "Journal";
        public static final String ACCOUNT_CODE_HEADER_NAME = "Code compte";
        public static final String REFERENCE_HEADER_NAME = "Référence";
        public static final String LINE_LABEL_HEADER_NAME = "Libellé ligne";
        public static final String LINE_DATE = "Date pièce";
        public static final String DEBIT_HEADER_NAME = "Débit";
        public static final String CREDIT_HEADER_NAME = "Crédit";
        public static final String JOURNAL_LABEL_HEADER_NAME = "Étiquette";
        public static final String JOURNAL_CODE_HEADER_NAME = "Code journal";
        public static final String RECONCILABLE_HEADER_NAME = "Rapprochable";
        public static final String CASH_FLOW_HEADER_NAME = "Trésorerie";
        public static final String LITERABLE_HEADER_NAME = "Lettrable";
        public static final String ACCOUNT_OPENING_DEBIT_HEADER_NAME = "Débit ouverture";
        public static final String ACCOUNT_OPENING_CREDIT_HEADER_NAME = "Crédit ouverture";
        public static final String CODE_HEADER_NAME = "Code";

        private XLSXHeaders() {

        }
    }
}
