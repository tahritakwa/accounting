package fr.sparkit.accounting.services;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public interface IGenericService<T, D extends Serializable> {

    List<T> findAll();

    List<T> findAll(Sort sort);

    List<T> findAll(Iterable<D> ids);

    <S extends T> List<S> save(Iterable<S> entities);

    void flush();

    <S extends T> S saveAndFlush(S entity);

    void deleteInBatchSoft(Iterable<T> entities);

    void delete(D id);

    void delete(T entity);

    void deleteList(Iterable<T> entities);

    T findOne(D id);

    Page<T> findAllByPaginationAndIsDeletedFalse(Pageable pageable);

    boolean isDynamicSoftDelete(D id, String className, String fieldToShow, String messageToShow);

    boolean isRelated(D id, String className);

}
