package ua.foxminded.schoolapplication.model.dao.exception;

import java.util.List;

import jakarta.persistence.EntityNotFoundException;

public class IdAwareEntityNotFoundException extends EntityNotFoundException {
	private final List<Long> missingIds;

	public IdAwareEntityNotFoundException(String message, List<Long> missingIds) {
		super(message);
		this.missingIds = missingIds;
	}
}
