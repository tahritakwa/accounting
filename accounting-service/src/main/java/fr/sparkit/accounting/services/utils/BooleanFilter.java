package fr.sparkit.accounting.services.utils;

import static fr.sparkit.accounting.constants.FilterConstants.*;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.springframework.data.jpa.domain.Specification;

import fr.sparkit.accounting.dto.Filter;

public final class BooleanFilter {

    private BooleanFilter() {
        super();
    }

    private static <T> Specification<T> getSpecificationByEqualOperator(String field, String value) {
        return (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) -> builder.equal(root.get(field),
                new Boolean(value));
    }

    public static <T> Specification<T> getSpecification(Filter filter) {
        Specification<T> specificationByOperatorAndFieldAndValue = null;
        if (EQUAL_FILTER_OPERATOR.equals(filter.getOperator())) {
            specificationByOperatorAndFieldAndValue = getSpecificationByEqualOperator(filter.getField(),
                    filter.getValue());
        }
        return specificationByOperatorAndFieldAndValue;
    }
}
