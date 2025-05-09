package ua.foxminded.schoolapplication.model.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import ua.foxminded.schoolapplication.model.dao.exception.ValidationException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Component
public class FieldStringValidator {
	private static final Logger logger = LoggerFactory.getLogger(FieldStringValidator.class);

	private static final int DEFAULT_MIN_LENGTH = 0;
	private static final int DEFAULT_MAX_LENGTH = Integer.MAX_VALUE;

	private static final Map<String, Pattern> PATTERN_CACHE = new ConcurrentHashMap<>();

	public void validate(String fieldValue, int minLength, int maxLength, String regexPattern, boolean isNullPossible)
			throws ValidationException {

		minLength = (minLength > 0) ? minLength : DEFAULT_MIN_LENGTH;
		maxLength = (maxLength > 0) ? maxLength : DEFAULT_MAX_LENGTH;

		logger.debug("Validating field value: '{}', minLength: {}, maxLength: {}, pattern: '{}'",
				fieldValue,
				minLength,
				maxLength,
				regexPattern);

		if (isEmptyValue(fieldValue, isNullPossible)) {
			logger.info("Validation successful because Null or Empty string is possible.");
			return;
		}

		checkLength(fieldValue, minLength, maxLength);

		checkPattern(fieldValue, regexPattern);

		logger.info("Validation successful for field value: '{}'", fieldValue.trim());
	}

	private boolean isEmptyValue(String fieldValue, boolean isNullPossible) {
		if (fieldValue == null || fieldValue.trim().isEmpty()) {
			if (isNullPossible) {
				return true;
			} else {
				logger.error("Field value cannot be null or empty.");
				throw new ValidationException("Field value cannot be null or empty.");
			}
		}

		logger.info("Validation of Null or Empty String successful.");
		return false;
	}

	private void checkLength(String fieldValue, int minLength, int maxLength) {
		int length = fieldValue.trim().length();

		if (length < minLength || length > maxLength) {
			logger.warn("Validation failed: Field value '{}' length {} is out of bounds [{}, {}]",
					fieldValue.trim(),
					length,
					minLength,
					maxLength);

			throw new ValidationException(String.format("Field value must be between %d and %d characters, but was: %d",
					minLength,
					maxLength,
					length));
		}
	}

	private void checkPattern(String fieldValue, String regexPattern) {
		Pattern pattern = PATTERN_CACHE.computeIfAbsent(regexPattern, key -> {
			try {
				logger.debug("Compiling and caching regex pattern: '{}'", key);
				return Pattern.compile(key);
			} catch (Exception e) {
				logger.error("Invalid regex pattern: '{}'", key, e);
				throw new RuntimeException("Invalid regex pattern: " + key, e);
			}
		});

		if (!pattern.matcher(fieldValue.trim()).matches()) {
			logger.warn("Validation failed: Field value '{}' does not match pattern '{}'",
					fieldValue.trim(),
					regexPattern);
			throw new ValidationException(
					String.format("Field value does not match the required format: %s", regexPattern));
		}
	}
}
