package fr.sparkit.accounting.dao;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.transaction.annotation.Transactional;

@NoRepositoryBean
public interface BaseRepository<T, I extends Serializable> extends JpaRepository<T, I>, JpaSpecificationExecutor {

    @Override
    @Transactional(readOnly = true)
    @Query("select e from #{#entityName} e where e.isDeleted = false")
    List<T> findAll();

    @Override
    @Transactional(readOnly = true)
    @Query("select e from #{#entityName} e where e.isDeleted = false")
    List<T> findAll(Sort sort);

    @Override
    @Transactional(readOnly = true)
    @Query("select e from #{#entityName} e where e.id in ?1 and e.isDeleted = false")
    List<T> findAllById(Iterable<I> ids);

    @Override
    @Transactional(readOnly = true)
    @Query("select count(e) from #{#entityName} e where e.isDeleted = false")
    long count();

    @Query("update #{#entityName} e set e.isDeleted=true, e.deletedToken = ?2  where e.id = ?1 ")
    @Transactional
    @Modifying
    void delete(I id, UUID uuid);

    @Query("update #{#entityName} e set e.isDeleted=true, e.deletedToken = ?2 where e = ?1")
    @Transactional
    @Modifying
    void delete(T entity, UUID uuid);

    @Override
    @Transactional(readOnly = true)
    @Query("select e from #{#entityName} e where e.id = ?1 and e.isDeleted = false")
    Optional<T> findById(I id);

    @Transactional(readOnly = true)
    @Query("select e from #{#entityName} e where e.id = ?1 and e.isDeleted = false")
    T findOne(I id);

    Page<T> findAllByIsDeletedFalse(Pageable pageable);

    @Transactional
    @Modifying
    @Query("update #{#entityName} e set e.isDeleted=true, e.deletedToken = ?2 WHERE e IN ?1")
    void deleteInBatchSoft(Iterable<T> entities, UUID uuid);

    @Transactional(readOnly = true)
    @Query("select e from #{#entityName} e where e.id = ?1")
    Optional<T> findByIdEvenIfIsDeleted(I id);
}
