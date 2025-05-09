package ua.foxminded.schoolapplication.model.dao.exception;

import org.springframework.dao.DataAccessException;

public class ValidationException extends DataAccessException {
	public ValidationException(String msg) {
		super(msg);
	}

	public ValidationException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
