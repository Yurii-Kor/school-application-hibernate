package ua.foxminded.schoolapplication.service;

import java.util.List;
import java.util.Set;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ua.foxminded.schoolapplication.model.dao.CourseDao;
import ua.foxminded.schoolapplication.model.dao.GroupDao;
import ua.foxminded.schoolapplication.model.dao.StudentDao;
import ua.foxminded.schoolapplication.model.domain.Course;
import ua.foxminded.schoolapplication.model.domain.Group;
import ua.foxminded.schoolapplication.model.domain.Student;
import jakarta.persistence.EntityNotFoundException;

@Service
@Transactional(value = TxType.REQUIRES_NEW)
public class ViewDaoService {

	private static final Logger logger = LoggerFactory.getLogger(ViewDaoService.class);

	private final GroupDao groupDao;
	private final StudentDao studentDao;
	private final CourseDao courseDao;

	public ViewDaoService(GroupDao groupDao, StudentDao studentDao, CourseDao courseDao) {
		this.groupDao = groupDao;
		this.studentDao = studentDao;
		this.courseDao = courseDao;
	}

	public List<Group> findGroupsWithStudentCountLessOrEqual(String maxStudentsInput) {
		validateInputString(maxStudentsInput, "Maximum students input must not be null or empty");
		logger.debug("Received input for maxStudents: '{}'", maxStudentsInput);

		long parsed = parseLong(maxStudentsInput);
		validateNonNegativeLong(parsed, String.format("Maximum number of students cannot be negative: %d", parsed));
		int maxStudents = parsed > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) parsed;

		List<Group> result = groupDao.findGroupsWithStudentCountLessOrEqual(maxStudents);
		logger.debug("Found {} groups with student count <= {}", result.size(), maxStudents);

		return result;
	}

	public Set<Student> findStudentsByCourseName(String courseNameInput) {
		validateInputString(courseNameInput, "Course name must not be null or empty");
		logger.debug("Received input for course name: '{}'", courseNameInput);

		Course course = courseDao.findByCourseName(courseNameInput).orElseThrow(() -> {
			logger.warn("No course found with name '{}'", courseNameInput);
			throw new EntityNotFoundException(
					String.format("Could not find students because course '%s' seems to have vanished mysteriously",
							courseNameInput));
		});

		Set<Student> students = courseDao.findStudentsOfCourse(course);
		logger.debug("Found {} student(s) enrolled in course '{}'", students.size(), courseNameInput);

		return students;
	}

	public Student addStudent(Student student) {
		if (student == null) {
			throw new IllegalArgumentException("Cannot add student: input is null.");
		}

		logger.debug("Attempting to add student: {}", student);
		Student savedStudent = studentDao.save(List.of(student)).get(0);
		logger.info("Student saved successfully: {}", savedStudent);

		return savedStudent;
	}

	public Student deleteStudentById(String studentIdInput) {
		validateInputString(studentIdInput, "Student ID input must not be null or empty");

		logger.debug("Received input to delete student by ID: '{}'", studentIdInput);

		Long parsedId = parseLong(studentIdInput);
		validateNonNegativeLong(parsedId, String.format("Student ID cannot be negative: %d", parsedId));

		Student studentToDelete = studentDao.findByIds(List.of(parsedId)).stream().findFirst().orElseThrow(() -> {
			logger.warn("Student with ID {} does not exist", parsedId);
			return new EntityNotFoundException(
					String.format("Cannot delete student: no student found with ID %d", parsedId));
		});

		logger.debug("Deleting student: {}", studentToDelete);
		studentDao.deleteAll(List.of(studentToDelete));
		logger.info("Successfully deleted student with ID {}", parsedId);

		return studentToDelete;
	}

	public Course addStudentToCourse(String studentIdInput, String courseNameInput) {
		validateInputString(studentIdInput, "Student ID input must not be null or empty");
		validateInputString(courseNameInput, "Course name must not be null or empty");
		logger.debug("Received request to add student '{}' to course '{}'", studentIdInput, courseNameInput);

		long parsedStudentId = parseLong(studentIdInput);
		validateNonNegativeLong(parsedStudentId, String.format("Student ID cannot be negative: %d", parsedStudentId));

		Course course = courseDao.findByCourseName(courseNameInput).orElseThrow(() -> {
			logger.warn("Course with name '{}' does not exist", courseNameInput);
			return new EntityNotFoundException(
					String.format("Cannot add student to course: course '%s' not found", courseNameInput));
		});

		Student student = Student.builder().id(parsedStudentId).build();

		logger.debug("Adding student '{}' to course '{}'", student, course);
		courseDao.addStudentsToCourse(course, Set.of(student));
		logger.info("Student with ID {} successfully added to course '{}'", student.getId(), courseNameInput);

		return course;
	}

	public Course removeStudentFromCourse(String studentIdInput, String courseNameInput) {
		validateInputString(studentIdInput, "Student ID input must not be null or empty");
		validateInputString(courseNameInput, "Course name must not be null or empty");
		logger.debug("Received request to remove student '{}' from course '{}'", studentIdInput, courseNameInput);

		long parsedStudentId = parseLong(studentIdInput);
		validateNonNegativeLong(parsedStudentId, String.format("Student ID cannot be negative: %d", parsedStudentId));

		Course course = courseDao.findByCourseName(courseNameInput).orElseThrow(() -> {
			logger.warn("Course with name '{}' does not exist", courseNameInput);
			return new EntityNotFoundException(
					String.format("Cannot remove student from course: course '%s' not found", courseNameInput));
		});
		
		Student student = Student.builder().id(parsedStudentId).build();

		logger.debug("Removing student '{}' from course '{}'", student, course);
		courseDao.removeStudentsFromCourse(course, Set.of(student));
		logger.info("Student with ID {} successfully removed from course '{}'", student.getId(), courseNameInput);

		return course;
	}

	private Long parseLong(String input) {
		try {
			return Long.parseLong(input);
		} catch (NumberFormatException e) {
			logger.error("Failed to parse input '{}' as number", input, e);
			throw new IllegalArgumentException(String.format("Invalid Input: '%s' is not a number.", input), e);
		}
	}

	private void validateInputString(String input, String errorMessage) {
		if (input == null || input.trim().isEmpty()) {
			logger.warn("Provided input string is null or empty");
			throw new IllegalArgumentException(errorMessage);
		}
	}

	private void validateNonNegativeLong(Long id, String errorMessage) {
		if (id < 0) {
			logger.warn("Provided ID is negative: {}", id);
			throw new IllegalArgumentException(errorMessage);
		}
	}
}
