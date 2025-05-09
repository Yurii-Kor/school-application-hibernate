package ua.foxminded.schoolapplication.model.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ua.foxminded.schoolapplication.model.dao.exception.ValidationException;
import ua.foxminded.schoolapplication.model.domain.Group;
import ua.foxminded.schoolapplication.model.domain.Student;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { EntityValidator.class, FieldStringValidator.class })
class StudentValidatorTest {

	static final String VALID_FIRST_NAME = "John";
	static final String VALID_LAST_NAME = "Doe";

	static final String NULL = "null";
	static final String INVALID_NAME_EMPTY = "";
	static final String INVALID_NAME_TOO_SHORT = "J";
	static final String INVALID_NAME_WITH_DIGIT = "John1";
	static final String INVALID_NAME_WITH_SYMBOL = "Doe!";
	static final String INVALID_NAME_WITH_SPACE = "John Doe";

	static final String STUDENTS_PATTERN = "firstName: {0}, lastName: {1} | Expected valid: {2}";

	@Autowired
	private EntityValidator<Student> validator;

	@ParameterizedTest(name = STUDENTS_PATTERN)
	@MethodSource("provideStudentsForValidation")
	@DisplayName("Student entity validation should behave as expected")
	void validateStudent_ShouldBehaveAsExpected(String firstName, String lastName, boolean shouldPass) {
		String validatedFirstName = NULL.equals(firstName) ? null : firstName;
		String validatedLastName = NULL.equals(lastName) ? null : lastName;

		Group group = Group.builder().build();

		Student student = Student.builder()
				.group(group)
				.firstName(validatedFirstName)
				.lastName(validatedLastName)
				.build();

		if (shouldPass) {
			assertDoesNotThrow(() -> validator.validateEntities(List.of(student)),
					"Validation should pass for student: " + student);
		} else {
			assertThrows(ValidationException.class,
					() -> validator.validateEntities(List.of(student)),
					"Validation should fail for student: " + student);
		}
	}

	static Stream<Arguments> provideStudentsForValidation() {
		return Stream.of(Arguments.of(VALID_FIRST_NAME, VALID_LAST_NAME, true),
				Arguments.of(NULL, VALID_LAST_NAME, false),
				Arguments.of(VALID_FIRST_NAME, NULL, false),
				Arguments.of(INVALID_NAME_EMPTY, VALID_LAST_NAME, false),
				Arguments.of(VALID_FIRST_NAME, INVALID_NAME_EMPTY, false),
				Arguments.of(INVALID_NAME_TOO_SHORT, VALID_LAST_NAME, false),
				Arguments.of(VALID_FIRST_NAME, INVALID_NAME_TOO_SHORT, false),
				Arguments.of(INVALID_NAME_WITH_DIGIT, VALID_LAST_NAME, false),
				Arguments.of(VALID_FIRST_NAME, INVALID_NAME_WITH_SYMBOL, false),
				Arguments.of(INVALID_NAME_WITH_SPACE, VALID_LAST_NAME, false),
				Arguments.of(VALID_FIRST_NAME, INVALID_NAME_WITH_SPACE, false));
	}
}
