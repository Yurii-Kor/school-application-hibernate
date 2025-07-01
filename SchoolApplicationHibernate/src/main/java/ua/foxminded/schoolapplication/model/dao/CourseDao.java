package ua.foxminded.schoolapplication.model.dao;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import jakarta.transaction.Transactional;
import jakarta.validation.Validator;
import ua.foxminded.schoolapplication.model.dao.exception.IdAwareEntityNotFoundException;
import ua.foxminded.schoolapplication.model.domain.Course;
import ua.foxminded.schoolapplication.model.domain.Student;

@Repository
@Transactional(Transactional.TxType.REQUIRED)
public class CourseDao extends Dao<Course> {

	private static final String FIND_COURSE_BY_NAME_QUERY = """
			SELECT DISTINCT c
			FROM Course c
			LEFT JOIN FETCH c.students
			WHERE c.courseName = :courseName
			""";

	private static final Logger logger = LoggerFactory.getLogger(CourseDao.class);

	public CourseDao(Validator validator) {
		super(Course.class, validator);
	}

	public void addStudentsToCourse(Course course, Set<Student> studentsToAdd) {
		validateInput(course, studentsToAdd);
		logger.debug("Adding students to course by ID: {}", course.getId());

		Course managedCourse = getManagedCourseOrThrow(course);
		Set<Student> currentStudents = managedCourse.getStudents();

		Set<Long> studentIdsToAdd = extractStudentIds(studentsToAdd);

		Set<Student> referencesToAdd = studentIdsToAdd.stream()
				.map(id -> em.getReference(Student.class, id))
				.collect(Collectors.toSet());

		currentStudents.addAll(referencesToAdd);

		logger.info("Students with IDs {} added to course ID: {}", studentIdsToAdd, course.getId());
	}

	public Set<Student> removeStudentsFromCourse(Course course, Set<Student> studentsToRemove) {
		validateInput(course, studentsToRemove);
		logger.debug("Removing students from course by ID: {}", course.getId());

		Course managedCourse = getManagedCourseOrThrow(course);
		Set<Student> currentStudents = managedCourse.getStudents();

		Set<Long> studentIdsToRemove = extractStudentIds(studentsToRemove);

		Set<Student> toRemove = currentStudents.stream()
				.filter(student -> studentIdsToRemove.contains(student.getId()))
				.collect(Collectors.toSet());

		currentStudents.removeAll(toRemove);

		logger.info("Students with IDs {} removed from course ID: {}", studentIdsToRemove, course.getId());

		return toRemove;
	}

	@Transactional(Transactional.TxType.SUPPORTS)
	public Set<Student> findStudentsOfCourse(Course course) {
		Course managed = em.find(Course.class, course.getId());
		if (managed == null) {
			throw new IdAwareEntityNotFoundException("Course not found", List.of(course.getId()));
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

	private void validateInput(Course course, Set<Student> students) {
		if (course == null || students == null) {
			logger.warn("Provided course or students is null");
			throw new IllegalArgumentException("Course and Students must not be null");
		}
	}

	private Course getManagedCourseOrThrow(Course course) {
		Course managed = em.find(Course.class, course.getId());
		if (managed == null) {
			logger.warn("Course with ID {} not found", course.getId());
			throw new IdAwareEntityNotFoundException("Course not found with ID: " + course.getId(),
					List.of(course.getId()));
		}
		
		return managed;
	}

	private Set<Long> extractStudentIds(Set<Student> students) {
		return students.stream().map(Student::getId).filter(Objects::nonNull).collect(Collectors.toSet());
	}
}
