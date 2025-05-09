package ua.foxminded.schoolapplication.model.dao;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

import ua.foxminded.schoolapplication.config.EntityExistenceValidatorConfig;
import ua.foxminded.schoolapplication.model.dao.exception.EntityIdNotFoundException;
import ua.foxminded.schoolapplication.model.dao.exception.ValidationException;
import ua.foxminded.schoolapplication.model.domain.Course;
import ua.foxminded.schoolapplication.model.domain.Group;
import ua.foxminded.schoolapplication.model.domain.Student;
import ua.foxminded.schoolapplication.model.validation.EntityValidator;
import ua.foxminded.schoolapplication.model.validation.FieldStringValidator;
import ua.foxminded.schoolapplication.testutil.TestDataInitializer;
import ua.foxminded.schoolapplication.testutil.TestEntities;
import ua.foxminded.schoolapplication.testutil.TestcontainersConfiguration;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import({ TestcontainersConfiguration.class, CourseDao.class, GroupDao.class, StudentDao.class, EntityValidator.class,
		FieldStringValidator.class, EntityExistenceValidatorConfig.class, TestDataInitializer.class })

class CourseDaoTest {

	static final String MATHEMATICS = "Mathematics";
	static final String BIOLOGY = "Biology";
	static final String DEFAULT_GROUP_NAME = "TestGroupCourse-11";
	static final String DEFAULT_FIRST_NAME = "Alice";
	static final String DEFAULT_LAST_NAME = "Smith";
	static final Long NON_EXISTENT_ID = 999L;
	static final int ONE_STUDENT_RELATED = 1;

	@Autowired
	private TestDataInitializer initializer;

	@Autowired
	private CourseDao courseDao;

	private Course testCourse;
	private Student testStudent;
	private Group testGroup;

	@BeforeAll
	void setup() {
		testGroup = Group.builder().groupName(DEFAULT_GROUP_NAME).build();
		testCourse = Course.builder().courseName(MATHEMATICS).build();
		testStudent = Student.builder()
				.group(testGroup)
				.firstName(DEFAULT_FIRST_NAME)
				.lastName(DEFAULT_LAST_NAME)
				.build();

		TestEntities entities = initializer.initialize(List.of(testGroup), List.of(testCourse), List.of(testStudent));

		testGroup = entities.groups().get(0);
		testCourse = entities.courses().get(0);
		testStudent = entities.students().get(0);
	}

	@Test
	@DisplayName("Save a course should persist course and assign ID")
	void saveShouldPersistCourse() {
		Course saved = courseDao.save(List.of(Course.builder().courseName(BIOLOGY).build())).get(0);
		assertNotNull(saved.getId(), "Course ID should not be null after saving");

		Optional<Course> fetched = courseDao.findById(saved.getId());
		assertTrue(fetched.isPresent(), "Course should be found after saving");
		assertEquals(BIOLOGY, fetched.get().getCourseName());
	}

	@Test
	@DisplayName("Save with null list should throw ValidationException")
	void saveWithNullListShouldThrow() {
		assertThrows(ValidationException.class, () -> courseDao.save(null));
	}

	@Test
	@DisplayName("Save with empty list should throw ValidationException")
	void saveWithEmptyListShouldThrow() {
		assertThrows(ValidationException.class, () -> courseDao.save(List.of()));
	}

	@Test
	@DisplayName("Save course with duplicate name should throw ConstraintViolationException")
	void saveDuplicateCourseNameShouldThrow() {
		Course duplicatedNameCourse = Course.builder().courseName(MATHEMATICS).build();

		assertThrows(ConstraintViolationException.class,
				() -> courseDao.save(List.of(duplicatedNameCourse)),
				"Saving a course with existing name should throw ConstraintViolationException");
	}

	@Test
	@DisplayName("Save two courses with duplicate name in one call should throw ConstraintViolationException")
	void saveTwoCoursesWithSameNameInOneCallShouldThrow() {
		List<Course> duplicatedNameCourses = List.of(Course.builder().courseName(BIOLOGY).build(),
				Course.builder().courseName(BIOLOGY).build());

		assertThrows(ConstraintViolationException.class,
				() -> courseDao.save(duplicatedNameCourses),
				"Saving two courses with duplicate name in one call should throw ConstraintViolationException");
	}

	@Test
	@DisplayName("Update should modify course")
	void updateShouldModifyCourse() {
		testCourse.setCourseName(BIOLOGY);
		Course updated = courseDao.update(List.of(testCourse)).get(0);

		Optional<Course> fetched = courseDao.findById(updated.getId());
		assertTrue(fetched.isPresent());
		assertEquals(BIOLOGY, fetched.get().getCourseName());
		testCourse.setCourseName(MATHEMATICS);
	}

	@Test
	@DisplayName("Update with non-existent course should throw exception")
	void updateNonExistentShouldThrow() {
		Course nonExistent = Course.builder().id(NON_EXISTENT_ID).courseName(BIOLOGY).build();
		assertThrows(EntityIdNotFoundException.class, () -> courseDao.update(List.of(nonExistent)));
	}

	@Test
	@DisplayName("Update course to existing name should throw ConstraintViolationException")
	void updateCourseToExistingNameShouldThrow() {
		courseDao.save(List.of(Course.builder().courseName(BIOLOGY).build()));

		Course math = courseDao.findByCourseName(MATHEMATICS).orElseThrow();

		math.setCourseName(BIOLOGY);

		assertThrows(ConstraintViolationException.class,
				() -> courseDao.update(List.of(math)),
				"Updating course to duplicate name should throw ConstraintViolationException");

		math.setCourseName(MATHEMATICS);
	}

	@Test
	@DisplayName("Delete should remove course")
	void deleteShouldRemoveCourse() {
		Course course = courseDao.findById(testCourse.getId()).get();
		courseDao.deleteAll(List.of(course));
		Optional<Course> deleted = courseDao.findById(course.getId());
		assertFalse(deleted.isPresent(), "Course should be deleted");
	}

	@Test
	@DisplayName("Delete non-existent course should throw exception")
	void deleteNonExistentShouldThrow() {
		Course ghost = Course.builder().id(NON_EXISTENT_ID).courseName(BIOLOGY).build();
		assertThrows(EntityIdNotFoundException.class, () -> courseDao.deleteAll(List.of(ghost)));
	}

	@Test
	@DisplayName("Add student to course should establish relation")
	void addStudentToCourseShouldWork() {
		Set<Student> before = courseDao.findStudentsOfCourse(testCourse);
		assertTrue(before.isEmpty());

		courseDao.addStudentsToCourse(testCourse, Set.of(testStudent));

		Set<Student> after = courseDao.findStudentsOfCourse(testCourse);
		assertEquals(ONE_STUDENT_RELATED, after.size());
		assertTrue(after.contains(testStudent));
	}

	@Test
	@DisplayName("Remove student from course should remove relation")
	void removeStudentFromCourseShouldWork() {
		courseDao.addStudentsToCourse(testCourse, Set.of(testStudent));
		Set<Student> before = courseDao.findStudentsOfCourse(testCourse);
		assertTrue(before.contains(testStudent));

		courseDao.removeStudentsFromCourse(testCourse, Set.of(testStudent));

		Set<Student> after = courseDao.findStudentsOfCourse(testCourse);
		assertFalse(after.contains(testStudent));
	}

	@Test
	@DisplayName("Find course by name should return correct course")
	void findByCourseNameShouldReturnCourse() {
		Optional<Course> result = courseDao.findByCourseName(MATHEMATICS);
		assertTrue(result.isPresent());
		assertEquals(MATHEMATICS, result.get().getCourseName());
	}

	@Test
	@DisplayName("Find course by name should return empty if not found")
	void findByCourseNameShouldReturnEmpty() {
		Optional<Course> result = courseDao.findByCourseName(BIOLOGY);
		assertFalse(result.isPresent());
	}
}
