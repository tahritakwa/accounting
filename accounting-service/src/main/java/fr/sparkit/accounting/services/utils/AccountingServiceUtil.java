package fr.sparkit.accounting.services.utils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import fr.sparkit.accounting.constants.AccountingConstants;
import fr.sparkit.accounting.util.errors.ApiErrors;
import fr.sparkit.accounting.util.http.HttpCustomException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class AccountingServiceUtil {

    private AccountingServiceUtil() {
        super();
    }

    public static boolean isCodeLowerThanOrEquals(String beginAccountCode,
            String endAccountCode) {
        boolean beginAccountCodeIsLowerThanEndAccountCode = true;
        if (!StringUtils.isNumeric(beginAccountCode) || !StringUtils.isNumeric(endAccountCode)) {
            return true;
        } else if (Integer.parseInt(beginAccountCode) > Integer.parseInt(endAccountCode)) {
            beginAccountCodeIsLowerThanEndAccountCode = false;
        }
        return beginAccountCodeIsLowerThanEndAccountCode;
    }

    private static boolean isNumberWithThreeDecimals(String string) {
        return string.matches("^\\d+\\.\\d{0," + AccountingConstants.DEFAULT_SCALE_FOR_BIG_DECIMAL + "}$|^\\d+");
    }

    public static void checkFilterOnDates(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate.isAfter(endDate)) {
            log.error(AccountingConstants.DATES_ORDER_INVALID);
            throw new HttpCustomException(ApiErrors.Accounting.START_DATE_IS_AFTER_END_DATE);
        }
    }

    public static void checkFilterOnAccounts(String beginAccountCode, String endAccountCode) {
        if (!AccountingServiceUtil.isCodeLowerThanOrEquals(beginAccountCode,
                endAccountCode)) {
            log.error(AccountingConstants.BEGIN_ACCOUNT_MUST_NOT_GRATER_THAN_END_ACCOUNT);
            throw new HttpCustomException(ApiErrors.Accounting.BEGIN_ACCOUNT_CODE_IS_GREATER_THAN_END_ACCOUNT);
        }
    }

    public static void checkFilterOnAmounts(String beginAmount, String endAmount) {
        String stringBeginAmount = beginAmount.replace(',', '.');
        String stringEndAmount = endAmount.replace(',', '.');
        if ((!isNumberWithThreeDecimals(stringBeginAmount) && !stringBeginAmount.isEmpty())
                || (!isNumberWithThreeDecimals(stringEndAmount) && !stringEndAmount.isEmpty())) {
            log.error(AccountingConstants.IS_NOT_VALID_FORMAT_AMOUNT_FILTER);
            throw new HttpCustomException(ApiErrors.Accounting.BEGIN_AMOUNT_OR_END_AMOUNT_FORMAT_INCORRECT);
        }
    }

    public static boolean isDateBeforeOrEquals(LocalDateTime localDateTime1, LocalDateTime localDateTime2) {
        return localDateTime1.isBefore(localDateTime2) || localDateTime1.equals(localDateTime2);
    }

    public static boolean dateIsAfterOrEquals(LocalDateTime ldt1, LocalDateTime ldt2) {
        return ldt1.isAfter(ldt2) || ldt1.equals(ldt2);
    }

    public static BigDecimal getBigDecimalAmount(String stringAmount, BigDecimal amount) {
        BigDecimal parseAmount = amount;
        if (!stringAmount.isEmpty()) {
            parseAmount = new BigDecimal(stringAmount);
        }
        return parseAmount;
    }

    public static int getDefaultAccountCode(String accountCode, int code) {
        int parseAccountCode = code;
        if (StringUtils.isNumeric(accountCode)) {
            parseAccountCode = Integer.parseInt(accountCode);
        }
        return parseAccountCode;
    }

    public static boolean fieldExistsInEntity(String fieldName, Class entityClass) {
        boolean fieldExists = Arrays.stream(entityClass.getDeclaredFields())
                .anyMatch(field -> field.getName().equalsIgnoreCase(fieldName));
        if (!fieldExists && !fieldName.isEmpty()) {
            log.error(AccountingConstants.TRYING_TO_SORT_USING_NON_EXISTENT_FIELD_REVERTING_TO_DEFAULT,
                    entityClass.getName(), fieldName);
        }
        return fieldExists;
    }

    public static Pageable getPageable(int page, int size, String field, String direction) {
        Sort.Order order = new Sort.Order(Sort.Direction.valueOf(direction.toUpperCase(AccountingConstants.LANGUAGE)),
                field).ignoreCase();
        return PageRequest.of(page, size, Sort.by(order));
    }

    public static String getConcatinationCodeWithLabel(String code, String label) {
        return code + " - " + label;
    }
}
