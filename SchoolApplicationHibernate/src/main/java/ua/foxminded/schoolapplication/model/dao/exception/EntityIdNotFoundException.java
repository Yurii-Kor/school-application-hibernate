package ua.foxminded.schoolapplication.model.dao.exception;

import java.util.List;

public class EntityIdNotFoundException extends RuntimeException {
	private final List<Long> missingIds;

	public EntityIdNotFoundException(String message, List<Long> missingIds) {
		super(message);
		this.missingIds = missingIds;
	}

	public EntityIdNotFoundException(String message) {
		super(message);
		this.missingIds = List.of();
	}

	public List<Long> getMissingIds() {
		return missingIds;
	}
}
