package ua.foxminded.schoolapplication.model.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ua.foxminded.schoolapplication.model.dao.util.EntityExistenceValidator;
import ua.foxminded.schoolapplication.model.domain.Identifiable;
import ua.foxminded.schoolapplication.model.validation.EntityValidator;

import java.util.List;
import java.util.Optional;

@Transactional(Transactional.TxType.REQUIRED)
public abstract class Dao<T extends Identifiable> {

	private static final Logger logger = LoggerFactory.getLogger(Dao.class);

	private static final String SELECT_EXISTING_ENTITIES = "SELECT e FROM %s e WHERE e.id IN :ids";
	private static final String PARAMETER_IDS = "ids";

	@PersistenceContext
	protected EntityManager em;

	protected final Class<T> entityClass;
	protected final EntityValidator<T> validator;
	protected final EntityExistenceValidator<T> existenceValidator;

	protected Dao(Class<T> entityClass, EntityValidator<T> validator, EntityExistenceValidator<T> existenceValidator) {
		this.entityClass = entityClass; 
		this.validator = validator;
		this.existenceValidator = existenceValidator;
	}

	@Transactional(Transactional.TxType.SUPPORTS)
	public Optional<T> findById(Long id) {
		Optional<T> entityOptional = Optional.ofNullable(em.find(entityClass, id));

		entityOptional.ifPresentOrElse(
				entity -> logger.info("Entity [{}] found: {}", entityClass.getSimpleName(), entity),
				() -> logger.warn("Entity [{}] not found with ID: {}", entityClass.getSimpleName(), id));

		return entityOptional;
	}

	@Transactional(Transactional.TxType.SUPPORTS)
	public List<T> findByIds(List<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return List.of();
		}

		return em.createQuery(String.format(SELECT_EXISTING_ENTITIES, entityClass.getSimpleName()), entityClass)
				.setParameter(PARAMETER_IDS, ids)
				.getResultList();
	}

	public List<T> save(List<T> entities) {
		validator.validateEntities(entities);

		logger.debug("Saving [{}] entities: {}", entityClass.getSimpleName(), entities);

		entities.stream().peek(entity -> logger.debug("Persisting entity: {}", entity)).forEach(em::persist);

		em.flush();

		logger.info("All [{}] entities saved successfully", entityClass.getSimpleName());
		return entities;
	}

	public List<T> update(List<T> entities) {
		validator.validateEntities(entities);
		existenceValidator.validateEntitiesExist(entities);

		logger.debug("Updating [{}] entities: {}", entityClass.getSimpleName(), entities);

		List<T> mergedEntities = entities.stream()
				.peek(entity -> logger.debug("Merging entity: {}", entity))
				.map(em::merge)
				.toList();

		em.flush();

		logger.info("All [{}] entities updated successfully", entityClass.getSimpleName());
		return mergedEntities;
	}

	public void deleteAll(List<T> entities) {
		logger.debug("Attempting to remove [{}] entities of type [{}]", entities.size(), entityClass.getSimpleName());

		existenceValidator.validateEntitiesExist(entities);

		entities.forEach(entity -> {
			em.remove(entity);
			logger.debug("Removed entity: {}", entity);
		});

		em.flush();

		logger.info("All [{}] entities removed successfully", entityClass.getSimpleName());
	}
}
