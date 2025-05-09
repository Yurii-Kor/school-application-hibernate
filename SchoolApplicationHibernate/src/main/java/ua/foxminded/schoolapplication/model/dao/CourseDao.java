package ua.foxminded.schoolapplication.model.dao;

import java.util.List;
import java.util.Optional;
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
public class CourseDao extends Dao<Course> {
	private static final String FIND_COURSE_BY_NAME_QUERY = "SELECT c FROM Course c WHERE c.courseName = :courseName";

	private static final Logger logger = LoggerFactory.getLogger(CourseDao.class);

	private final EntityExistenceValidator<Student> studentValidator;

	public CourseDao(EntityValidator<Course> validator, EntityExistenceValidator<Course> courseExistenceValidator,
			EntityExistenceValidator<Student> studentExistenceValidator) {

		super(Course.class, validator, courseExistenceValidator);
		this.studentValidator = studentExistenceValidator;
	}

	public void addStudentsToCourse(Course course, Set<Student> studentsToAdd) {
		logger.debug("Adding students to course: {}", course);
		existenceValidator.validateEntitiesExist(List.of(course));
		studentValidator.validateEntitiesExist(studentsToAdd.stream().toList());

		course.getStudents().addAll(studentsToAdd);
		em.merge(course);
		em.flush();
		logger.info("Students added to course with ID: {}", course.getId());
	}

	public void removeStudentsFromCourse(Course course, Set<Student> studentsToRemove) {
		logger.debug("Removing students from course: {}", course);
		existenceValidator.validateEntitiesExist(List.of(course));
		studentValidator.validateEntitiesExist(studentsToRemove.stream().toList());

		course.getStudents().removeAll(studentsToRemove);
		em.merge(course);
		em.flush();
		logger.info("Students removed from course with ID: {}", course.getId());
	}

	@Transactional(Transactional.TxType.SUPPORTS)
	public Set<Student> findStudentsOfCourse(Course course) {
		Course managed = em.find(Course.class, course.getId());
		if (managed == null) {
			throw new EntityIdNotFoundException("Course not found", List.of(course.getId()));
		}

		managed.getStudents().size();
		return managed.getStudents();
	}

	@Transactional(Transactional.TxType.SUPPORTS)
	public Optional<Course> findByCourseName(String courseName) {
		logger.debug("Looking for course with name: {}", courseName);

		List<Course> result = em.createQuery(FIND_COURSE_BY_NAME_QUERY, Course.class)
				.setParameter("courseName", courseName)
				.getResultList();

		if (result.isEmpty()) {
			logger.info("No course found with name: {}", courseName);
			return Optional.empty();
		}

		return Optional.of(result.get(0));
	}
}
