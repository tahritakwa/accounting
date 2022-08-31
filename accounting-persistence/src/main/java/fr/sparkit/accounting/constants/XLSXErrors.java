package fr.sparkit.accounting.constants;

public final class XLSXErrors {

    public static final class DocumentAccountXLSXErrors {
        public static final String DOCUMENT_DATE_CELL_TYPE_SHOULD_BE_DATE = "Le format de la cellule de la date du document doit être Date";
        public static final String DOCUMENT_ACCOUNT_DATE_NOT_IN_FISCAL_YEAR = "La date n'est pas dans l'exercice courant";
        public static final String DOCUMENT_ACCOUNT_DATE_IN_CLOSED_PERIOD = "La date est dans une période fermée";
        public static final String DOCUMENT_ACCOUNT_DATE_AFTER_TODAY = "La date ne doit pas être antérieure à la date d'aujourd'hui";
        public static final String MONETARY_VALUE_CANT_BE_NEGATIVE = "Les valeurs monétaires ne peuvent pas être négatives";
        public static final String MONETARY_VALUE_SCALE_REACHED = "Les valeurs monétaires doivent avoir %d chiffres après la virgule au maximum";
        public static final String MONETARY_CELL_SHOULD_BE_OF_TYPE_NUMBER = "Le contenu des cellules monétaires doit être de type numérique";
        public static final String SUM_DEBIT_DIFFERENT_THAN_SUM_CREDIT = "La somme du débit ne doit pas"
                + " être différente à celle du crédit pour une pièce comptable";

        private DocumentAccountXLSXErrors() {
            super();
        }
    }

    public static final class ChartAccountXLSXErrors {
        public static final String NO_CHART_ACCOUNT_WITH_CODE = "Il n'existe pas un plan comptable avec le code %s";
        public static final String CHART_ACCOUNT_CODE_CANT_BE_NEGATIVE_OR_ZERO = "Le code du plan ne peut pas être négatif ou 0";
        public static final String CHART_ACCOUNT_EXISTS = "Un plan avec ce code existe";
        public static final String CHART_ACCOUNT_CODE_CELL_SHOULD_BE_OF_TYPE_NUMBER = "Le contenu des cellules du code du plan"
                + " doit être de type entier";
        public static final String CHART_ACCOUNT_CODE_SHOULD_BE_IMMEDIAT_CHILD_TO_PARENT_CODE = "Le code du plan doit être préfixé par le"
                + " code du plan parent avec un seul écart";
        public static final String PARENT_CODE_SHOULD_BE_SPECIFIED = "Le code du plan parent doit être spécifié";
        public static final String CHART_ACCOUNT_MAX_CHART_ACCOUNT_CODE_EXCEEDED = "Le code maximum que peut prendre un plan comptable est %d";
        public static final String CHART_ACCOUNT_MAX_PARENT_CHART_ACCOUNT_CODE_EXCEEDED = "Le code maximum que peut prendre un plan parent est %d";

        private ChartAccountXLSXErrors() {
            super();
        }

    }

    public static final class JournalXLSXErrors {
        public static final String JOURNAL_CODE_LENGTH_MUST_BE_BETWEEN = "La longueur du code journal doit être entre %d et %d";
        public static final String JOURNAL_LABEL_LENGTH_MUST_BE_AT_LEAST = "La longueur du libellé du journal doit être au minimum %d";
        public static final String JOURNAL_WITH_CODE_EXISTS = "Un journal avec ce code existe";
        public static final String JOURNAL_WITH_LABEL_EXISTS = "Un journal avec ce libellé existe";
        public static final String NO_JOURNAL_WITH_CODE = "Il n'existe pas un journal avec le code %s";

        private JournalXLSXErrors() {
            super();
        }

    }

    public static final class AccountXLSXErrors {
        public static final String ACCOUNT_WITH_CODE_EXISTS = "Un compte avec ce code existe";
        public static final String ACCOUNT_CODE_SHOULD_BE_IMMEDIATE_CHILD_TO_PARENT_CODE = "Le code du compte doit être préfixé par"
                + " le code du plan parent";
        public static final String ACCOUNT_CODE_INVALID_FORMAT = "Le code du compte comptable doit être"
                + " un entier positif de %d chiffres";

        private AccountXLSXErrors() {
            super();
        }
    }

    public static final String REQUIRED_FIELD = "Ce champs est obligatoire";
    public static final String LENGTH_INVALID = "Longueur minimale %d et maximale %d caractères";
    public static final String INVALID_CELL_FORMAT = "{}. Emplacement [ colonne : {}  , ligne : {} , feuille : {} ]";
    public static final String RECONCILABLE_VALUE_NOT_ALLOWED = "La valeur du champs rapprochable n'est pas valide, seules %s et %s sont valides";
    public static final String CASH_FLOW_VALUE_NOT_ALLOWED = "La valeur du champs trésorerie n'est pas valide, seules %s et %s sont valides";
    public static final String ACCOUNT_ACCOUNT_CODE_CELL_SHOULD_BE_OF_TYPE_NUMBER = "Le contenu des cellules du code du compte"
            + " doit être de type numérique";
    public static final String NO_ACCOUNT_WITH_CODE = "Il n'existe pas un compte avec le code %s";
    public static final String DUPLICATE_ACCOUNTS_FOUND_WITH_THE_SAME_CODE = "Il existe plusieurs comptes avec le code %s";
    public static final String DEBIT_OR_CREDIT_VALUE_INVALID = "La valeur du débit ou du crédit est invalide";

    private XLSXErrors() {
        super();
    }
}
