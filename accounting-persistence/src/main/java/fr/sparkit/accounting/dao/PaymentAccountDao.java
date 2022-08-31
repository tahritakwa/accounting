package fr.sparkit.accounting.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import fr.sparkit.accounting.entities.account.relations.PaymentAccount;

@Repository
public interface PaymentAccountDao extends JpaRepository<PaymentAccount, Long> {

    Optional<PaymentAccount> findByTaxId(Long taxId);
}
