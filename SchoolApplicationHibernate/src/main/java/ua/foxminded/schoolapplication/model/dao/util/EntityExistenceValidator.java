package ua.foxminded.schoolapplication.model.dao.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import org.springframework.stereotype.Component;
import ua.foxminded.schoolapplication.model.dao.exception.EntityIdNotFoundException;
import ua.foxminded.schoolapplication.model.domain.Identifiable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EntityExistenceValidator<T extends Identifiable> {

	private static final String SELECT_EXISTING_IDS = "SELECT e.id FROM %s e WHERE e.id IN :ids";

	@PersistenceContext
	private EntityManager em;

	private final Class<T> entityClass;

	public EntityExistenceValidator(Class<T> entityClass) {
		this.entityClass = entityClass;
	}

	@Transactional(Transactional.TxType.SUPPORTS)
	public void validateEntitiesExist(List<T> entities) {
		if (entities == null || entities.isEmpty()) {
			throw new EntityIdNotFoundException("List of entities is empty");
		}

		List<Long> ids = entities.stream().map(Identifiable::getId).toList();

		List<Long> foundIds = em
				.createQuery(String.format(SELECT_EXISTING_IDS, entityClass.getSimpleName()), Long.class)
				.setParameter("ids", ids)
				.getResultList();

		Set<Long> foundSet = new HashSet<>(foundIds);
		List<Long> missingIds = ids.stream().filter(id -> !foundSet.contains(id)).toList();

		if (!missingIds.isEmpty()) {
			throw new EntityIdNotFoundException("Missing entities with IDs: ", missingIds);
		}
	}
}
