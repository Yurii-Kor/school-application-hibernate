package ua.foxminded.schoolapplication.model.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import ua.foxminded.schoolapplication.model.dao.exception.ValidationException;

import java.lang.reflect.Field;
import java.util.List;

@Component
public class EntityValidator<T> {
	private static final Logger logger = LoggerFactory.getLogger(EntityValidator.class);

	private FieldStringValidator stringValidator;

	public EntityValidator(FieldStringValidator stringValidator) {
		this.stringValidator = stringValidator;
	}

	public void validateEntities(List<T> entities) throws ValidationException {
		if (entities == null || entities.size() == 0) {
			String methodName = Thread.currentThread().getStackTrace()[2].getMethodName();
			String errorMessage = String.format("Method '%s' received invalid data", methodName);
			logger.warn(errorMessage);
			throw new ValidationException(errorMessage);
		}

		for (var entity : entities) {
			validateEntity(entity);
		}
	}

	public void validateEntity(T entity) throws ValidationException {
		if (entity == null) {
			throw new ValidationException("Entity cannot be null.");
		}

		for (var field : entity.getClass().getDeclaredFields()) {
			field.setAccessible(true);

			try {
				var value = field.get(entity);

				if (handleNullField(value, field)) {
					continue;
				} else if (value instanceof Long longValue) {
					validateLongField(longValue, field.getName());
				} else if (value instanceof String stringValue) {
					validateStringField(stringValue, field);
				} else {
					logger.info("Field '{}' is of unsupported type '{}' — skipping validation.",
							field.getName(),
							value.getClass().getSimpleName());
				}
			} catch (Exception e) {
				logger.error("Failed to validate field: {}", field.getName(), e);
				throw new ValidationException("Internal error validating field: " + field.getName(), e);
			} finally {
				field.setAccessible(false);
			}
		}

		logger.info("Validation successful for entity: {}", entity);
	}

	private void validateLongField(Long value, String fieldName) throws ValidationException {
		if (value < 0) {
			throw new ValidationException(String.format("Field '%s' cannot be negative (value: %d)", fieldName, value));
		}
	}

	private void validateStringField(String value, Field field) throws ValidationException {
		var annotation = field.getAnnotation(StringValidationParameters.class);

		if (annotation == null) {
			throw new ValidationException(
					"Missing @StringValidationParameters annotation for field: " + field.getName());
		}

		stringValidator.validate(value,
				annotation.minLength(),
				annotation.maxLength(),
				annotation.pattern(),
				annotation.isNullPossible());
	}

	private boolean handleNullField(Object value, Field field) throws ValidationException {
		if (value != null) {
			return false;
		}

		if (field.getType() == String.class) {
			validateStringField(null, field);
		} else {
			logger.info("Field '{}' has value = NULL", field.getName());
		}

		return true;
	}
}