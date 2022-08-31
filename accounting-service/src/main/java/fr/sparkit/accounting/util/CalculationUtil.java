package fr.sparkit.accounting.util;

import static fr.sparkit.accounting.constants.AccountingConstants.THOUSANDS_SEPARATOR;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import fr.sparkit.accounting.constants.AccountingConstants;
import fr.sparkit.accounting.util.errors.ApiErrors;
import fr.sparkit.accounting.util.errors.ErrorsResponse;
import fr.sparkit.accounting.util.http.HttpCustomException;

public final class CalculationUtil {

    public static final List<String> DUPLICATE_OPERATIONS = Collections
            .unmodifiableList(Arrays.asList("++", "--", "+-", "-+"));
    public static final char CREDIT_SYMBOL = 'c';
    public static final char DEBIT_SYMBOL = 'd';
    public static final char RESULT_SYMBOL = 'r';
    public static final char BILAN_SYMBOL = 'b';
    public static final char VARIATION_SYMBOL = 'v';
    public static final char EVOLUTION_BILAN_SYMBOL = 'e';

    private static final char GROUPING_SEPARATOR = ' ';

    private CalculationUtil() {
        super();
    }

    public static Collection<String> divideSimpleStringFormulaToStringElements(String formula) {
        for (String invalidDuplicateOperation : DUPLICATE_OPERATIONS) {
            if (formula.contains(invalidDuplicateOperation)) {
                throw new HttpCustomException(ApiErrors.Accounting.REPORT_LINE_INVALID_FORMULA,
                        new ErrorsResponse().error(formula));
            }
        }
        checkMethodStartsOrEndsWithOperationSign(formula);
        String[] formulaElements = formula.toLowerCase(AccountingConstants.LANGUAGE).replace("-", "+-").split("\\+");
        Collection<String> formulas = new ArrayList<>();
        for (String formulaElement : formulaElements) {
            try {
                if (Character.isDigit(formulaElement.charAt(formulaElement.length() - 1))) {
                    formulas.add(String.valueOf(Integer.parseInt(formulaElement)));
                } else {
                    validateFormulaElement(formula, formulas, formulaElement);
                }
            } catch (NumberFormatException e) {
                throw new HttpCustomException(ApiErrors.Accounting.REPORT_LINE_INVALID_FORMULA,
                        new ErrorsResponse().error(formula));
            }
        }
        return formulas;
    }

    private static void validateFormulaElement(String formula, Collection<String> formulas, String formulaElement) {
        if (formulaElement.charAt(formulaElement.length() - 1) == CREDIT_SYMBOL
                || formulaElement.charAt(formulaElement.length() - 1) == DEBIT_SYMBOL
                || formulaElement.charAt(formulaElement.length() - 1) == RESULT_SYMBOL
                || formulaElement.charAt(formulaElement.length() - 1) == BILAN_SYMBOL
                || formulaElement.charAt(formulaElement.length() - 1) == VARIATION_SYMBOL
                || formulaElement.charAt(formulaElement.length() - 1) == EVOLUTION_BILAN_SYMBOL) {
            formulas.add(String.valueOf(Integer.parseInt(formulaElement.substring(0, formulaElement.length() - 1)))
                    + formulaElement.charAt(formulaElement.length() - 1));
        } else {
            throw new HttpCustomException(ApiErrors.Accounting.REPORT_LINE_INVALID_FORMULA,
                    new ErrorsResponse().error(formula));
        }
    }

    private static void checkMethodStartsOrEndsWithOperationSign(String formula) {
        if (formula.startsWith("+") || formula.startsWith("-") || formula.endsWith("+") || formula.endsWith("-")) {
            throw new HttpCustomException(ApiErrors.Accounting.REPORT_LINE_INVALID_FORMULA,
                    new ErrorsResponse().error(formula));
        }
    }

    public static Collection<Integer> complexStringFormulaToCollection(String formula) {
        String tempFormula = formula.replace("-(", ",(-").replace("+", ",");
        String[] chartAccounts = tempFormula.split(",");
        Collection<Integer> formulas = new ArrayList<>();
        for (String chartAccount : chartAccounts) {
            if (chartAccount.isEmpty()) {
                throw new HttpCustomException(ApiErrors.Accounting.REPORT_LINE_INVALID_FORMULA,
                        new ErrorsResponse().error(formula));
            } else {
                try {
                    chartAccount = chartAccount.substring(1, chartAccount.length() - 1);
                    formulas.add(Integer.parseInt(chartAccount));
                } catch (NumberFormatException e) {
                    throw new HttpCustomException(ApiErrors.Accounting.REPORT_LINE_INVALID_FORMULA,
                            new ErrorsResponse().error(formula));
                }
            }
        }
        return formulas;
    }

    public static boolean isFormulaComplex(String formula) {
        return formula.contains("(") || formula.contains(")");
    }

    public static String getFormattedBigDecimalValueOrEmptyStringIfZero(BigDecimal value) {
        if (value != null && value.compareTo(BigDecimal.ZERO) != 0) {
            return getAccountingDecimalFormat().format(value);
        } else {
            return StringUtils.EMPTY;
        }
    }

    public static String getFormattedBigDecimalValueOrEmptyString(BigDecimal value) {
        if (value != null) {
            return getAccountingDecimalFormat().format(value);
        } else {
            return StringUtils.EMPTY;
        }
    }

    public static String getFormattedBigDecimalValue(BigDecimal value) {
        return getAccountingDecimalFormat().format(value);
    }

    public static BigDecimal getBigDecimalValueFromFormattedString(String formattedString) {
        if (formattedString == null || formattedString.isEmpty()) {
            return BigDecimal.ZERO;
        } else {
            return new BigDecimal(formattedString.replace(String.valueOf(GROUPING_SEPARATOR), StringUtils.EMPTY)
                    .replace(THOUSANDS_SEPARATOR, '.'));
        }
    }

    public static DecimalFormat getAccountingDecimalFormat() {
        DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
        decimalFormatSymbols.setDecimalSeparator(THOUSANDS_SEPARATOR);
        decimalFormatSymbols.setGroupingSeparator(GROUPING_SEPARATOR);
        DecimalFormat decimalFormat = new DecimalFormat(AccountingConstants.ACCOUNTING_AMOUNT_DISPLAY_FORMATTED);
        decimalFormat.setDecimalFormatSymbols(decimalFormatSymbols);
        return decimalFormat;
    }

    public static DecimalFormat getRateFormat() {
        DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
        DecimalFormat decimalFormat = new DecimalFormat(AccountingConstants.ACCOUNTING_RATE_DISPLAY_FORMATTED);
        decimalFormat.setDecimalFormatSymbols(decimalFormatSymbols);
        return decimalFormat;
    }

}
