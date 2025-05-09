package ua.foxminded.schoolapplication.service.exception;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import ua.foxminded.schoolapplication.model.domain.Identifiable;

public class ServiceOperationException extends RuntimeException {

	private final List<? extends Identifiable> entities;

	public ServiceOperationException(String message) {
		super(message);
		this.entities = List.of();
	}

	public ServiceOperationException(String message, Throwable cause) {
		super(message, cause);
		this.entities = List.of();
	}

	public ServiceOperationException(String message, List<? extends Identifiable> entities) {
		super(message);
		this.entities = (entities == null) ? Collections.emptyList()
				: entities.stream().filter(Objects::nonNull).toList();
	}

	public List<? extends Identifiable> getEntities() {
		return entities;
	}
}
