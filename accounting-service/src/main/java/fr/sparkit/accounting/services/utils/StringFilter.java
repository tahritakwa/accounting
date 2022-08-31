package fr.sparkit.accounting.services.utils;

import static fr.sparkit.accounting.constants.AccountingConstants.*;
import static fr.sparkit.accounting.constants.FilterConstants.*;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.springframework.data.jpa.domain.Specification;

import fr.sparkit.accounting.dto.Filter;

public final class StringFilter {

    private StringFilter() {
        super();
    }

    private static <T> Specification<T> getSpecificationByEqualOperator(String field, String value) {
        return (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) -> builder
                .equal(root.get(field).as(String.class), value);
    }

    private static <T> Specification<T> getSpecificationByNotEqualOperator(String field, String value) {
        return (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) -> builder
                .notEqual(root.get(field).as(String.class), value);
    }

    private static <T> Specification<T> getSpecificationByContainsOperator(String field, String value) {
        return (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) -> builder
                .like(root.get(field).as(String.class), String.format(PREDICATE_FILTER_LIKE, value));
    }

    private static <T> Specification<T> getSpecificationByDoesNotContainOperator(String field, String value) {
        return (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) -> builder
                .notLike(root.get(field).as(String.class), String.format(PREDICATE_FILTER_LIKE, value));
    }

    private static <T> Specification<T> getSpecificationByStartsWithOperator(String field, String value) {
        return (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) -> builder
                .like(root.get(field).as(String.class), String.format(PREDICATE_FILTER_STARTS_WITH, value));
    }

    private static <T> Specification<T> getSpecificationByEndsWithOperator(String field, String value) {
        return (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) -> builder
                .like(root.get(field).as(String.class), String.format(PREDICATE_FILTER_ENDS_WITH, value));
    }

    private static <T> Specification<T> getSpecificationByIsNullOperator(String field) {
        return (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) -> builder
                .isNull(root.get(field).as(String.class));
    }

    private static <T> Specification<T> getSpecificationByIsNotNullOperator(String field) {
        return (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) -> builder
                .isNotNull(root.get(field).as(String.class));
    }

    private static <T> Specification<T> getSpecificationByIsEmptyOperator(String field) {
        return (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) -> builder
                .equal(root.get(field).as(String.class), EMPTY_STRING);
    }

    private static <T> Specification<T> getSpecificationByIsNotEmptyOperator(String field) {
        return (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) -> builder
                .notEqual(root.get(field).as(String.class), EMPTY_STRING);
    }

    public static <T> Specification<T> getSpecification(Filter filter) {
        Specification<T> specificationByOperatorAndFieldAndValue;
        String field = filter.getField();
        String value = filter.getValue();
        if (value != null) {
            value = value.trim();
        }
        switch (filter.getOperator()) {
        case EQUAL_FILTER_OPERATOR:
            specificationByOperatorAndFieldAndValue = getSpecificationByEqualOperator(field, value);
            break;
        case NOT_EQUAL_FILTER_OPERATOR:
            specificationByOperatorAndFieldAndValue = getSpecificationByNotEqualOperator(field, value);
            break;
        case CONTAINS_FILTER_OPERATOR:
            specificationByOperatorAndFieldAndValue = getSpecificationByContainsOperator(field, value);
            break;
        case DOES_NOT_CONTAIN_FILTER_OPERATOR:
            specificationByOperatorAndFieldAndValue = getSpecificationByDoesNotContainOperator(field, value);
            break;
        case STARTS_WITH_FILTER_OPERATOR:
            specificationByOperatorAndFieldAndValue = getSpecificationByStartsWithOperator(field, value);
            break;
        case END_WITH_FILTER_OPERATOR:
            specificationByOperatorAndFieldAndValue = getSpecificationByEndsWithOperator(field, value);
            break;
        case IS_NULL_FILTER_OPERATOR:
            specificationByOperatorAndFieldAndValue = getSpecificationByIsNullOperator(field);
            break;
        case IS_NOT_NULL_FILTER_OPERATOR:
            specificationByOperatorAndFieldAndValue = getSpecificationByIsEmptyOperator(field);
            break;
        case IS_EMPTY_FILTER_OPERATOR:
            specificationByOperatorAndFieldAndValue = getSpecificationByIsNotNullOperator(field);
            break;
        case IS_NOT_EMPTY_FILTER_OPERATOR:
            specificationByOperatorAndFieldAndValue = getSpecificationByIsNotEmptyOperator(field);
            break;
        default:
            specificationByOperatorAndFieldAndValue = null;
        }
        return specificationByOperatorAndFieldAndValue;
    }

}
