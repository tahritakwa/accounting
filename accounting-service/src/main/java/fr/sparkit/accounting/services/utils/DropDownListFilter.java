package fr.sparkit.accounting.services.utils;

import static fr.sparkit.accounting.constants.FilterConstants.EQUAL_FILTER_OPERATOR;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

import fr.sparkit.accounting.dto.Filter;

public final class DropDownListFilter {

    private DropDownListFilter() {
        super();
    }

    private static boolean isValueANumber(String value) {
        if (value.startsWith("-")) {
            value = value.substring(1);
        }
        return StringUtils.isNumeric(value);
    }

    private static <T> Specification<T> getSpecificationByEqualOperator(String field, String value) {
        if (!isValueANumber(value)) {
            return null;
        }
        Long longValue = Long.valueOf(value);
        return (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) -> builder.equal(root.get(field),
                longValue);
    }

    public static <T> Specification<T> getSpecification(Filter filter) {
        Specification<T> specificationByOperatorAndFieldAndValue;
        if (filter.getOperator().equals(EQUAL_FILTER_OPERATOR)) {
            specificationByOperatorAndFieldAndValue = getSpecificationByEqualOperator(filter.getField(),
                    filter.getValue());
        } else {
            specificationByOperatorAndFieldAndValue = null;
        }
        return specificationByOperatorAndFieldAndValue;
    }

}
