package ua.foxminded.schoolapplication.model.dao;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import jakarta.transaction.Transactional;
import ua.foxminded.schoolapplication.model.dao.exception.EntityIdNotFoundException;
import ua.foxminded.schoolapplication.model.dao.util.EntityExistenceValidator;
import ua.foxminded.schoolapplication.model.domain.Course;
import ua.foxminded.schoolapplication.model.domain.Student;
import ua.foxminded.schoolapplication.model.validation.EntityValidator;

@Repository
@Transactional(Transactional.TxType.REQUIRED)
public class StudentDao extends Dao<Student> {

	private static final Logger logger = LoggerFactory.getLogger(StudentDao.class);

	private final EntityExistenceValidator<Course> courseValidator;

	public StudentDao(EntityValidator<Student> validator, EntityExistenceValidator<Student> studentExistenceValidator,
			EntityExistenceValidator<Course> courseExistenceValidator) {

		super(Student.class, validator, studentExistenceValidator);
		this.courseValidator = courseExistenceValidator;
	}

	public void addCoursesToStudent(Student student, Set<Course> coursesToAdd) {
		logger.debug("Adding courses to student: {}", student);
		existenceValidator.validateEntitiesExist(List.of(student));
		courseValidator.validateEntitiesExist(coursesToAdd.stream().toList());

		student.getCourses().addAll(coursesToAdd);
		em.merge(student);
		em.flush();
		logger.info("Courses added to student with ID: {}", student.getId());

	}

	public void removeCoursesFromStudent(Student student, Set<Course> coursesToRemove) {
		logger.debug("Removing courses from student: {}", student);
		existenceValidator.validateEntitiesExist(List.of(student));
		courseValidator.validateEntitiesExist(coursesToRemove.stream().toList());

		student.getCourses().removeAll(coursesToRemove);
		em.merge(student);
		em.flush();
		logger.info("Courses removed from student with ID: {}", student.getId());
	}

	@Transactional(Transactional.TxType.SUPPORTS)
	public Set<Course> findCoursesOfStudent(Student student) {
		Student managed = em.find(Student.class, student.getId());
		if (managed == null) {
			throw new EntityIdNotFoundException("Student not found", List.of(student.getId()));
		}

		managed.getCourses().size();
		return managed.getCourses();
	}
}
