package fr.sparkit.accounting.services.utils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import fr.sparkit.accounting.constants.AccountingConstants;
import fr.sparkit.accounting.constants.NumberConstant;
import fr.sparkit.accounting.convertor.LetteringConverter;
import fr.sparkit.accounting.dao.DocumentAccountLineDao;
import fr.sparkit.accounting.dto.LiterableDocumentAccountLineDto;
import fr.sparkit.accounting.dto.LiterableDocumentAccountLinePageDto;
import fr.sparkit.accounting.entities.Account;
import fr.sparkit.accounting.entities.DocumentAccount;
import fr.sparkit.accounting.entities.DocumentAccountLine;
import fr.sparkit.accounting.entities.Journal;
import fr.sparkit.accounting.util.errors.ApiErrors;
import fr.sparkit.accounting.util.http.HttpCustomException;

public final class LettringUtil {

    private LettringUtil() {
        super();
    }

    public static String literateDocumentAccountLinesByAccountAndReference(
            List<DocumentAccountLine> documentAccountLines,
            Collection<DocumentAccountLine> documentAccountLinesByAccountAndReference, String letter,
            DocumentAccountLineDao documentAccountLineDao,
            LiterableDocumentAccountLinePageDto literableDocumentAccountLinePage) {

        List<DocumentAccountLine> creditList = initCreditListUsingDocumentAccountLines(
                documentAccountLinesByAccountAndReference);
        List<DocumentAccountLine> debitList = initDebitListUsingDocumentAccountLines(
                documentAccountLinesByAccountAndReference);
        if (!creditList.isEmpty() && !debitList.isEmpty()) {
            List<int[]> creditCombinationsList = initCombinationsListFromSize(creditList.size());
            List<int[]> debitCombinationsList = initCombinationsListFromSize(debitList.size());

            int i = NumberConstant.ZERO;
            int j = NumberConstant.ZERO;

            while (i < debitCombinationsList.size()) {
                while (j < creditCombinationsList.size()) {
                    if (i == debitCombinationsList.size()) {
                        break;
                    }

                    BigDecimal sumDebits = calculateSumFromDebitListUsingCombination(debitList,
                            debitCombinationsList.get(i));
                    BigDecimal sumCredits = calculateSumFromCreditListUsingCombination(creditList,
                            creditCombinationsList.get(j));

                    if (sumDebits.compareTo(sumCredits) == NumberConstant.ZERO) {

                        literableDocumentAccountLinePage
                                .setTotalDebit(literableDocumentAccountLinePage.getTotalDebit().add(sumDebits));
                        literableDocumentAccountLinePage
                                .setTotalCredit(literableDocumentAccountLinePage.getTotalCredit().add(sumCredits));

                        List<Long> debitListIds = debitList.stream().map(DocumentAccountLine::getId)
                                .collect(Collectors.toList());
                        List<Long> creditListIds = creditList.stream().map(DocumentAccountLine::getId)
                                .collect(Collectors.toList());

                        literateDocumentAccountLinesByLetterUsingCombinationAndIds(documentAccountLines, letter,
                                debitCombinationsList.get(i), debitListIds);
                        literateDocumentAccountLinesByLetterUsingCombinationAndIds(documentAccountLines, letter,
                                creditCombinationsList.get(j), creditListIds);

                        for (int index : debitCombinationsList.get(i)) {
                            debitCombinationsList = LettringUtil.removeAllArrayOfIntFromListContainingIndex(
                                    debitCombinationsList, Integer.valueOf(index));
                        }
                        for (int index : creditCombinationsList.get(j)) {
                            creditCombinationsList = LettringUtil.removeAllArrayOfIntFromListContainingIndex(
                                    creditCombinationsList, Integer.valueOf(index));
                        }
                        i = NumberConstant.ZERO;
                        j = NumberConstant.ZERO;
                    } else {
                        j++;
                    }
                }
                i++;
                j = NumberConstant.ZERO;
            }
        }
        return incrementLetter(letter, documentAccountLineDao);
    }

    private static List<int[]> initCombinationsListFromSize(int size) {
        List<int[]> combinationsList = new ArrayList<>();
        for (int i = NumberConstant.ZERO; i < size; i++) {
            combinationsList.addAll(LettringUtil.getCombinationsOfAllDigitFromZeroToI(i, size));
        }
        return combinationsList;
    }

    private static List<DocumentAccountLine> initDebitListUsingDocumentAccountLines(
            Collection<DocumentAccountLine> documentAccountLines) {
        return documentAccountLines.stream().filter(documentAccountLine -> documentAccountLine.getDebitAmount()
                .compareTo(BigDecimal.ZERO) != NumberConstant.ZERO).collect(Collectors.toList());
    }

    private static List<DocumentAccountLine> initCreditListUsingDocumentAccountLines(
            Collection<DocumentAccountLine> documentAccountLines) {
        return documentAccountLines.stream().filter(documentAccountLine -> documentAccountLine.getCreditAmount()
                .compareTo(BigDecimal.ZERO) != NumberConstant.ZERO).collect(Collectors.toList());
    }

    public static String incrementLetter(String previousLetter, DocumentAccountLineDao documentAccountLineDao) {
        if (previousLetter == null) {
            return AccountingConstants.FIRST_LETTERING_CODE;
        } else if (!previousLetter.equalsIgnoreCase(AccountingConstants.LAST_LETTERING_CODE)) {
            StringBuilder sb = new StringBuilder(previousLetter.toUpperCase(AccountingConstants.LANGUAGE));
            for (int i = sb.length() - NumberConstant.ONE; i >= NumberConstant.ZERO; i--) {
                if (sb.charAt(i) == 'Z') {
                    sb.setCharAt(i, 'A');
                } else {
                    sb.setCharAt(i, (char) (sb.charAt(i) + NumberConstant.ONE));
                    String letterIncrementation;
                    letterIncrementation = getLetterIncrementation(documentAccountLineDao, sb);
                    return letterIncrementation;
                }
            }
            return sb.toString();
        } else {
            throw new HttpCustomException(ApiErrors.Accounting.LAST_CODE_REACHED);
        }
    }

    private static String getLetterIncrementation(DocumentAccountLineDao documentAccountLineDao, StringBuilder sb) {
        String letterIncrementation;
        if (documentAccountLineDao.findFirstLetterByLetterAndIsDeletedFalse(sb.toString()) != null) {
            letterIncrementation = incrementLetter(sb.toString(), documentAccountLineDao);
        } else {
            letterIncrementation = sb.toString();
        }
        return letterIncrementation;
    }

    private static void generateCombinations(List<int[]> combinations, int[] data, int start, int end, int index) {
        if (index == data.length) {
            int[] combination = data.clone();
            combinations.add(combination);
        } else if (start <= end) {
            data[index] = start;
            generateCombinations(combinations, data, start + NumberConstant.ONE, end, index + NumberConstant.ONE);
            generateCombinations(combinations, data, start + NumberConstant.ONE, end, index);
        }
    }

    private static List<int[]> getCombinationsOfAllDigitFromZeroToI(int i, int size) {
        List<int[]> combinations = new ArrayList<>();
        generateCombinations(combinations, new int[i + 1], NumberConstant.ZERO, size - NumberConstant.ONE,
                NumberConstant.ZERO);
        return combinations;
    }

    private static List<int[]> removeAllArrayOfIntFromListContainingIndex(List<int[]> list, Integer index) {
        List<Integer> intList;
        for (Iterator<int[]> listIterator = list.iterator(); listIterator.hasNext();) {
            intList = Arrays.stream(listIterator.next()).boxed().collect(Collectors.toList());
            if (intList.contains(index)) {
                listIterator.remove();
            }
        }
        return list;
    }

    public static List<LiterableDocumentAccountLineDto> fillLiterableLinesUsingDocumentAccountLines(
            List<LiterableDocumentAccountLineDto> literableDocumentAccountLines,
            Iterable<DocumentAccountLine> documentAccountLines) {
        documentAccountLines.forEach((DocumentAccountLine documentAccountLine) -> {
            DocumentAccount documentAccount = documentAccountLine.getDocumentAccount();
            Account account = documentAccountLine.getAccount();
            Journal journal = documentAccount.getJournal();
            literableDocumentAccountLines.add(LetteringConverter
                    .documentAccountingLineToLetteringDto(documentAccountLine, documentAccount, account, journal));
        });
        return literableDocumentAccountLines;
    }

    private static void literateDocumentAccountLinesByLetterUsingCombinationAndIds(
            List<DocumentAccountLine> documentAccountLines, String letter, int[] combination, List<Long> ids) {
        for (int index : combination) {
            Long id = ids.get(index);
            documentAccountLines.stream().filter(documentAccountLine -> id.equals(documentAccountLine.getId()))
                    .forEach((DocumentAccountLine documentAccountLine) -> documentAccountLine.setLetter(letter));
        }
    }

    public static void sortLiterableDocumentAccountLinesByLetter(
            List<LiterableDocumentAccountLineDto> literableDocumentAccountLines) {
        Comparator<LiterableDocumentAccountLineDto> letterComparator = Comparator
                .comparing(LiterableDocumentAccountLineDto::getLetter, Comparator.nullsLast(String::compareTo));
        Collections.sort(literableDocumentAccountLines, letterComparator);
    }

    private static BigDecimal calculateSumFromDebitListUsingCombination(List<DocumentAccountLine> debitList,
            int[] combination) {
        BigDecimal sum = BigDecimal.ZERO;
        for (int index : combination) {
            sum = sum.add(debitList.get(index).getDebitAmount());
        }
        return sum;
    }

    private static BigDecimal calculateSumFromCreditListUsingCombination(List<DocumentAccountLine> creditList,
            int[] combination) {
        BigDecimal sum = BigDecimal.ZERO;
        for (int index : combination) {
            sum = sum.add(creditList.get(index).getCreditAmount());
        }
        return sum;
    }
}
