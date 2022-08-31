package fr.sparkit.accounting.services.utils;

import static fr.sparkit.accounting.constants.FilterConstants.*;

import java.time.LocalDate;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.springframework.data.jpa.domain.Specification;

import fr.sparkit.accounting.constants.NumberConstant;
import fr.sparkit.accounting.dto.Filter;

public final class DateFilter {

    private DateFilter() {
        super();
    }

    private static <T> Specification<T> getSpecificationByEqualOperator(String field, String value) {
        return (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) -> builder
                .like(root.get(field).as(String.class), String.format(PREDICATE_FILTER_LIKE, value));
    }

    @SuppressWarnings("unchecked")
    private static <T> Specification<T> getSpecificationByNotEqualOperator(String field, String value) {
        return (Specification<T>) getSpecificationByIsNullOperator(field)
                .or(getSpecificationByDoesNotContainOperator(field, value));
    }

    private static <T> Specification<T> getSpecificationByDoesNotContainOperator(String field, String value) {
        return (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) -> builder
                .notLike(root.get(field).as(String.class), String.format(PREDICATE_FILTER_LIKE, value));
    }

    private static <T> Specification<T> getSpecificationByIsNullOperator(String field) {
        return (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) -> builder
                .isNull(root.get(field).as(String.class));
    }

    private static <T> Specification<T> getSpecificationByIsEmptyOperator(String field) {
        return (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) -> builder
                .isNull(root.get(field).as(String.class));
    }

    private static <T> Specification<T> getSpecificationByIsAfter(String field, String value) {
        return (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) -> builder.greaterThanOrEqualTo(
                root.get(field), LocalDate.parse(value).plusDays(NumberConstant.ONE).atStartOfDay());
    }

    @SuppressWarnings("unchecked")
    private static <T> Specification<T> getSpecificationByIsAfterOrEqual(String field, String value) {
        return (Specification<T>) getSpecificationByIsAfter(field, value)
                .or(getSpecificationByEqualOperator(field, value));
    }

    private static <T> Specification<T> getSpecificationByIsBefore(String field, String value) {
        return (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) -> builder.lessThan(root.get(field),
                LocalDate.parse(value).atStartOfDay());
    }

    @SuppressWarnings("unchecked")
    private static <T> Specification<T> getSpecificationByIsBeforeOrEqual(String field, String value) {
        return (Specification<T>) getSpecificationByIsBefore(field, value)
                .or(getSpecificationByEqualOperator(field, value));
    }

    public static <T> Specification<T> getSpecification(Filter filter) {
        Specification<T> specificationByOperatorAndFieldAndValue;
        String field = filter.getField();
        String value = filter.getValue();
        switch (filter.getOperator()) {
        case EQUAL_FILTER_OPERATOR:
            specificationByOperatorAndFieldAndValue = getSpecificationByEqualOperator(field, value);
            break;
        case NOT_EQUAL_FILTER_OPERATOR:
            specificationByOperatorAndFieldAndValue = getSpecificationByNotEqualOperator(field, value);
            break;
        case IS_NULL_FILTER_OPERATOR:
            specificationByOperatorAndFieldAndValue = getSpecificationByIsNullOperator(field);
            break;
        case IS_NOT_NULL_FILTER_OPERATOR:// empty
            specificationByOperatorAndFieldAndValue = getSpecificationByIsEmptyOperator(field);
            break;
        case GREATER_THAN_OR_EQUAL_OPERATOR:
            specificationByOperatorAndFieldAndValue = getSpecificationByIsAfterOrEqual(field, value);
            break;
        case GREATER_THAN_OPERATOR:
            specificationByOperatorAndFieldAndValue = getSpecificationByIsAfter(field, value);
            break;
        case LESS_THAN_OR_EQUAL_OPERATOR:
            specificationByOperatorAndFieldAndValue = getSpecificationByIsBeforeOrEqual(field, value);
            break;
        case LESS_THAN_OPERATOR:
            specificationByOperatorAndFieldAndValue = getSpecificationByIsBefore(field, value);
            break;
        default:
            specificationByOperatorAndFieldAndValue = null;
        }
        return specificationByOperatorAndFieldAndValue;
    }

}
