package ua.foxminded.schoolapplication.model.dao;

import java.util.List;
import java.util.Objects;
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
public class StudentDao extends Dao<Student> {

	private static final Logger logger = LoggerFactory.getLogger(StudentDao.class);

	public StudentDao(Validator validator) {
		super(Student.class, validator);
	}

	public void addCoursesToStudent(Student student, Set<Course> coursesToAdd) {
		validateInput(student, coursesToAdd);
		logger.debug("Adding courses to student by ID: {}", student.getId());

		Student managedStudent = getManagedStudentOrThrow(student);
		Set<Course> currentCourses = managedStudent.getCourses();

		Set<Long> courseIdsToAdd = extractCourseIds(coursesToAdd);

		Set<Course> referencesToAdd = courseIdsToAdd.stream()
				.map(id -> em.getReference(Course.class, id))
				.collect(Collectors.toSet());

		currentCourses.addAll(referencesToAdd);

		logger.info("Courses with IDs {} added to student ID: {}", courseIdsToAdd, student.getId());
	}

	public Set<Course> removeCoursesFromStudent(Student student, Set<Course> coursesToRemove) {
		validateInput(student, coursesToRemove);
		logger.debug("Removing courses from student by ID: {}", student.getId());

		Student managedStudent = getManagedStudentOrThrow(student);
		Set<Course> currentCourses = managedStudent.getCourses();

		Set<Long> courseIdsToRemove = extractCourseIds(coursesToRemove);

		Set<Course> toRemove = currentCourses.stream()
				.filter(course -> courseIdsToRemove.contains(course.getId()))
				.collect(Collectors.toSet());

		currentCourses.removeAll(toRemove);

		logger.info("Courses with IDs {} removed from student ID: {}", courseIdsToRemove, student.getId());

		return toRemove;
	}

	@Transactional(Transactional.TxType.SUPPORTS)
	public Set<Course> findCoursesOfStudent(Student student) {
		Student managed = em.find(Student.class, student.getId());
		if (managed == null) {
			throw new IdAwareEntityNotFoundException("Student not found", List.of(student.getId()));
		}

		managed.getCourses().size();
		return managed.getCourses();
	}

	private void validateInput(Student student, Set<Course> courses) {
		if (student == null || courses == null) {
			logger.warn("Provided student or course set is null");
			throw new IllegalArgumentException("Student and Courses must not be null");
		}
	}

	private Student getManagedStudentOrThrow(Student student) {
		Student managed = em.find(Student.class, student.getId());
		if (managed == null) {
			logger.warn("Student with ID {} not found", student.getId());
			throw new IdAwareEntityNotFoundException("Student not found with ID: " + student.getId(),
					List.of(student.getId()));
		}
		return managed;
	}

	private Set<Long> extractCourseIds(Set<Course> courses) {
		return courses.stream().map(Course::getId).filter(Objects::nonNull).collect(Collectors.toSet());
	}
}
