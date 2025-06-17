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

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import ua.foxminded.schoolapplication.config.ValidatorConfig;
import ua.foxminded.schoolapplication.model.dao.exception.IdAwareEntityNotFoundException;
import ua.foxminded.schoolapplication.model.domain.Course;
import ua.foxminded.schoolapplication.model.domain.Group;
import ua.foxminded.schoolapplication.model.domain.Student;
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
@Import({ TestcontainersConfiguration.class, CourseDao.class, GroupDao.class, StudentDao.class, ValidatorConfig.class,
		TestDataInitializer.class })

class CourseDaoTest {

	static final String MATHEMATICS = "Mathematics";
	static final String BIOLOGY = "Biology";
	static final String DEFAULT_GROUP_NAME = "TestGroupCourse-11";
	static final String DEFAULT_FIRST_NAME = "Alice";
	static final String DEFAULT_LAST_NAME = "Smith";
	static final Long NON_EXISTENT_ID = 999L;
	static final int ONE_STUDENT_RELATED = 1;

	@Autowired
	private EntityManager em;

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

		List<Course> fetched = courseDao.findByIds(List.of(saved.getId()));
		assertTrue(!fetched.isEmpty(), "Course should be found after saving");
		assertEquals(BIOLOGY, fetched.get(0).getCourseName());
	}

	@Test
	@DisplayName("Save with null list should throw ValidationException")
	void saveWithNullListShouldThrow() {
		assertThrows(IllegalArgumentException.class, () -> courseDao.save(null));
	}

	@Test
	@DisplayName("Save with empty list should throw ValidationException")
	void saveWithEmptyListShouldThrow() {
		assertThrows(IllegalArgumentException.class, () -> courseDao.save(List.of()));
	}

	@Test
	@DisplayName("Save course with duplicate name should throw ConstraintViolationException")
	void saveDuplicateCourseNameShouldThrow() {
		Course duplicatedNameCourse = Course.builder().courseName(MATHEMATICS).build();

		assertThrows(ConstraintViolationException.class, () -> {
			courseDao.save(List.of(duplicatedNameCourse));
			em.flush();
		}, "Saving a course with existing name should throw ConstraintViolationException");
	}

	@Test
	@DisplayName("Save two courses with duplicate name in one call should throw ConstraintViolationException")
	void saveTwoCoursesWithSameNameInOneCallShouldThrow() {
		List<Course> duplicatedNameCourses = List.of(Course.builder().courseName(BIOLOGY).build(),
				Course.builder().courseName(BIOLOGY).build());

		assertThrows(ConstraintViolationException.class, () -> {
			courseDao.save(duplicatedNameCourses);
			em.flush();
		}, "Saving two courses with duplicate name in one call should throw ConstraintViolationException");
	}

	@Test
	@DisplayName("Update should modify course")
	void updateShouldModifyCourse() {
		testCourse.setCourseName(BIOLOGY);
		Course updated = courseDao.update(List.of(testCourse)).get(0);

		List<Course> fetched = courseDao.findByIds(List.of(updated.getId()));
		assertFalse(fetched.isEmpty());
		assertEquals(BIOLOGY, fetched.get(0).getCourseName());
		testCourse.setCourseName(MATHEMATICS);
	}

	@Test
	@DisplayName("Update with non-existent course should throw exception")
	void updateNonExistentShouldThrow() {
		Course nonExistent = Course.builder().id(NON_EXISTENT_ID).courseName(BIOLOGY).build();
		assertThrows(IdAwareEntityNotFoundException.class, () -> courseDao.update(List.of(nonExistent)));
	}

	@Test
	@DisplayName("Update course to existing name should throw ConstraintViolationException")
	void updateCourseToExistingNameShouldThrow() {
		courseDao.save(List.of(Course.builder().courseName(BIOLOGY).build()));

		Course math = courseDao.findByCourseName(MATHEMATICS).orElseThrow();

		math.setCourseName(BIOLOGY);

		assertThrows(ConstraintViolationException.class, () -> {
			courseDao.update(List.of(math));
			em.flush();
		}, "Updating course to duplicate name should throw ConstraintViolationException");

		math.setCourseName(MATHEMATICS);
	}

	@Test
	@DisplayName("Delete should remove course")
	void deleteShouldRemoveCourse() {
		Course course = courseDao.findByIds(List.of(testCourse.getId())).get(0);
		courseDao.deleteAll(List.of(course));
		List<Course> deleted = courseDao.findByIds(List.of(course.getId()));
		assertTrue(deleted.isEmpty(), "Course should be deleted");
	}

	@Test
	@DisplayName("Delete non-existent course should return empty list")
	void deleteNonExistentShouldReturnEmptyList() {
		Course ghost = Course.builder().id(NON_EXISTENT_ID).courseName(BIOLOGY).build();

		List<Course> deleted = courseDao.deleteAll(List.of(ghost));
		assertTrue(deleted.isEmpty(), "Deleting non-existent course should return empty list");
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
	@DisplayName("Add non-existent student to course should throw EntityIdNotFoundException with missing ID")
	void addNonExistentStudentToCourseShouldThrowException() {
		long missingId = NON_EXISTENT_ID;
		Student ghost = Student.builder()
				.id(missingId)
				.firstName(DEFAULT_FIRST_NAME)
				.lastName(DEFAULT_LAST_NAME)
				.group(testGroup)
				.build();

		assertThrows(EntityNotFoundException.class,
				() -> courseDao.addStudentsToCourse(testCourse, Set.of(ghost)),
				"Expected exception when adding non-existent student");
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
	@DisplayName("Remove non-existent student from course should do nothing and return empty set")
	void removeNonExistentStudentFromCourseShouldDoNothing() {
		Student ghost = Student.builder()
				.id(NON_EXISTENT_ID)
				.firstName(DEFAULT_FIRST_NAME)
				.lastName(DEFAULT_LAST_NAME)
				.group(testGroup)
				.build();

		Set<Student> removed = courseDao.removeStudentsFromCourse(testCourse, Set.of(ghost));

		assertTrue(removed.isEmpty(), "Expected no students to be removed");
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
