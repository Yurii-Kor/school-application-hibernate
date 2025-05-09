package ua.foxminded.schoolapplication.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ua.foxminded.schoolapplication.model.dao.util.EntityExistenceValidator;
import ua.foxminded.schoolapplication.model.domain.Course;
import ua.foxminded.schoolapplication.model.domain.Group;
import ua.foxminded.schoolapplication.model.domain.Student;

@Configuration
public class EntityExistenceValidatorConfig {

	@Bean
	public EntityExistenceValidator<Student> studentExistenceValidator() {
		return new EntityExistenceValidator<>(Student.class);
	}

	@Bean
	public EntityExistenceValidator<Course> courseExistenceValidator() {
		return new EntityExistenceValidator<>(Course.class);
	}

	@Bean
	public EntityExistenceValidator<Group> groupExistenceValidator() {
		return new EntityExistenceValidator<>(Group.class);
	}
}
