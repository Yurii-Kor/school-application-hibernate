package ua.foxminded.schoolapplication.model.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ua.foxminded.schoolapplication.model.dao.exception.FieldConstraintException;
import ua.foxminded.schoolapplication.model.dao.exception.IdAwareEntityNotFoundException;
import ua.foxminded.schoolapplication.model.domain.Identifiable;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Transactional(Transactional.TxType.REQUIRED)
public abstract class Dao<T extends Identifiable> {

	private static final Logger logger = LoggerFactory.getLogger(Dao.class);
	
    private final Validator validator;

	@PersistenceContext
	protected EntityManager em;

	protected final Class<T> entityClass;

	protected Dao(Class<T> entityClass, Validator validator) {
		this.entityClass = entityClass;
		this.validator = validator;
	}

	@Transactional(Transactional.TxType.SUPPORTS)
	public List<T> findByIds(List<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return List.of();
		}

		return ids.stream().map(id -> em.find(entityClass, id)).filter(entity -> entity != null).toList();
	}

	public List<T> save(List<T> entities) {
		validateEntities(entities);

		logger.debug("Saving [{}] entities: {}", entityClass.getSimpleName(), entities);

		entities.stream().forEach(em::persist);

		logger.info("All [{}] entities saved successfully", entityClass.getSimpleName());
		return entities;
	}

	public List<T> update(List<T> entities) {
		validateEntities(entities);

		List<Long> ids = entities.stream().map(Identifiable::getId).filter(Objects::nonNull).toList();
		List<T> existedEntities = findByIds(ids);

		if (existedEntities.size() != entities.size()) {
			List<Long> foundIds = existedEntities.stream().map(Identifiable::getId).toList();
			List<Long> missingIds = ids.stream().filter(id -> !foundIds.contains(id)).toList();

			logger.warn("Entity existence check failed. Missing IDs for type [{}]: {}",
					entityClass.getSimpleName(),
					missingIds);
			throw new IdAwareEntityNotFoundException("Missing entities with IDs: ", missingIds);
		}

		logger.debug("Updating [{}] entities: {}", entityClass.getSimpleName(), entities);

		List<T> mergedEntities = entities.stream().map(em::merge).toList();

		logger.info("All [{}] entities updated successfully", entityClass.getSimpleName());
		return mergedEntities;
	}

	public List<T> deleteAll(List<T> entities) {
		if (entities == null || entities.isEmpty()) {
			return List.of();
		}
		logger.debug("Attempting to remove [{}] entities of type [{}]", entities.size(), entityClass.getSimpleName());

		List<Long> ids = entities.stream().map(Identifiable::getId).toList();
		List<T> entitiesToRemove = findByIds(ids);

		if (entitiesToRemove.isEmpty()) {
			logger.warn("No entities found to remove for type [{}] with IDs: {}", entityClass.getSimpleName(), ids);
			return List.of();
		}

		entitiesToRemove.forEach(entity -> {
			em.remove(entity);
		});

		logger.info("All [{}] entities removed successfully", entityClass.getSimpleName());
		return entitiesToRemove;
	}
	
	protected void validateEntities(List<T> entities) {
		if (entities == null || entities.isEmpty()) {
			throw new IllegalArgumentException("The list of entities must not be null or empty");

		}
		
	    Set<ConstraintViolation<?>> allViolations = new HashSet<>();

	    for (T entity : entities) {
	        Set<? extends ConstraintViolation<?>> violations = validator.validate(entity);
	        allViolations.addAll(violations);
	    }

	    if (!allViolations.isEmpty()) {
	        throw new FieldConstraintException(allViolations);
	    }
	}
}
