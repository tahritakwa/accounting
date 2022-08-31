package fr.sparkit.accounting.services.impl;

import static fr.sparkit.accounting.constants.AccountingConstants.IS_DELETED;
import static fr.sparkit.accounting.constants.FilterConstants.*;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import fr.sparkit.accounting.dao.BaseRepository;
import fr.sparkit.accounting.dto.Filter;
import fr.sparkit.accounting.services.utils.BooleanFilter;
import fr.sparkit.accounting.services.utils.DateFilter;
import fr.sparkit.accounting.services.utils.DropDownListFilter;
import fr.sparkit.accounting.services.utils.StringFilter;

public final class FilterService {
    private FilterService() {
        super();
    }

    @SuppressWarnings("unchecked")
    public static <T, I extends Serializable> Page<T> getPageOfFilterableEntity(Class<T> entity,
            BaseRepository<T, I> entityDao, List<Filter> filters, Pageable pageable) {
        Specification<T> specifications = getSpecificationsForFilterableEntity(entity, filters);
        return entityDao.findAll(specifications, pageable);
    }

    @SuppressWarnings("unchecked")
    public static <T, I extends Serializable> List getPageOfFilterableEntity(Class<T> entity,
            BaseRepository<T, I> entityDao, List<Filter> filters) {
        Specification<T> specifications = getSpecificationsForFilterableEntity(entity, filters);
        return entityDao.findAll(specifications);
    }

    public static <T> Specification<T> getSpecificationsForFilterableEntity(Class<T> entity, Iterable<Filter> filters) {
        Specification<T> specifications = initSpecificationByIsDeletedFalse();
        for (Filter filter : filters) {
            if (isFieldExistsInEntity(entity, filter.getField())) {
                specifications = specifications.and(getSpecificationByFilterType(filter));
            }
        }
        return specifications;
    }

    public static <T> Specification<T> initSpecificationByIsDeletedFalse() {
        return Specification.where((Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) -> builder
                .equal(root.get(IS_DELETED).as(Boolean.class), false));
    }

    public static <T> boolean isFieldExistsInEntity(Class<T> entity, String field) {
        return Arrays.stream(entity.getDeclaredFields())
                .anyMatch(classField -> classField.getName().equalsIgnoreCase(field));
    }

    public static <T> Specification<T> getSpecificationByFilterType(Filter filter) {
        Specification<T> specification;
        switch (filter.getType()) {
        case STRING:
            specification = StringFilter.getSpecification(filter);
            break;
        case BOOLEAN:
            specification = BooleanFilter.getSpecification(filter);
            break;
        case DROP_DOWN_LIST:
            specification = DropDownListFilter.getSpecification(filter);
            break;
        case DATE:
            specification = DateFilter.getSpecification(filter);
            break;
        default:
            specification = null;
        }
        return specification;
    }

}
