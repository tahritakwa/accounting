package fr.sparkit.accounting.entities.account.relations;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import fr.sparkit.accounting.entities.Account;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@Table(name = "TJ_ACCOUNT_COFFER", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "GAR_DELETED_TOKEN", "GAR_RELATION_ENTITY_ID" }) })
public class AccountCoffer extends GenericAccountRelation implements Serializable {
    private static final long serialVersionUID = 1L;

    public AccountCoffer(Long id, Account account, Long relationEntityId) {
        super(id, account, relationEntityId, false, null);
    }
}
