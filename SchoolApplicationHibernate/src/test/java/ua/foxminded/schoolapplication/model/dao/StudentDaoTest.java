package ua.foxminded.schoolapplication.model.dao;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.hibernate.exception.ConstraintViolationException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.testcontainers.junit.jupiter.Testcontainers;

import ua.foxminded.schoolapplication.config.ValidatorConfig;
import ua.foxminded.schoolapplication.model.dao.exception.IdAwareEntityNotFoundException;
import ua.foxminded.schoolapplication.model.domain.Course;
import ua.foxminded.schoolapplication.model.domain.Group;
import ua.foxminded.schoolapplication.model.domain.Student;
import ua.foxminded.schoolapplication.testutil.TestDataInitializer;
import ua.foxminded.schoolapplication.testutil.TestEntities;
import ua.foxminded.schoolapplication.testutil.TestcontainersConfiguration;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import({ TestcontainersConfiguration.class, StudentDao.class, GroupDao.class, CourseDao.class, ValidatorConfig.class,
		TestDataInitializer.class })

class StudentDaoTest {

	static final String DEFAULT_GROUP_NAME = "TestGroupStudent-11";
	static final String NON_EXISTENT_GROUP_NAME = "NonExistentGroup-22";
	static final String DEFAULT_COURSE_NAME = "Cyber Security";
	static final String NON_EXISTENT_COURSE_NAME = "NonExistentCourse";
	static final String DEFAULT_FIRST_NAME = "John";
	static final String UPDATED_FIRST_NAME = "UpdatedJohn";
	static final String DEFAULT_LAST_NAME = "Doe";
	static final String UPDATED_LAST_NAME = "UpdatedDoe";

	static final Long NON_EXISTENT_ID = 999L;
	static final int ONE_COURSE_PROCESSED = 1;

	@Autowired
	private EntityManager em;

	@Autowired
	private TestDataInitializer initializer;

	@Autowired
	private StudentDao studentDao;

	private Group testGroup;
	private Student testStudent;
	private Course testCourse;

	@BeforeAll
	void setup() {
		testGroup = Group.builder().groupName(DEFAULT_GROUP_NAME).build();
		testCourse = Course.builder().courseName(DEFAULT_COURSE_NAME).build();
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
	@DisplayName("Save a student should persist student and assign ID")
	void saveShouldPersistStudent() {
		Student saved = studentDao.save(List.of(
				Student.builder().group(testGroup).firstName(DEFAULT_FIRST_NAME).lastName(DEFAULT_LAST_NAME).build()))
				.get(0);
		assertNotNull(saved.getId(), "Student ID should not be null after saving");

		List<Student> fetched = studentDao.findByIds(List.of(saved.getId()));
		assertFalse(fetched.isEmpty(), "Student should be found after saving");
		assertEquals(testGroup.getId(), fetched.get(0).getGroup().getId());
		assertEquals(DEFAULT_FIRST_NAME, fetched.get(0).getFirstName());
		assertEquals(DEFAULT_LAST_NAME, fetched.get(0).getLastName());
	}

	@Test
	@DisplayName("Saving student with non-existent group should throw ConstraintViolationException")
	void saveStudentWithNonExistentGroupShouldThrowException() {
		Group detachedGroup = Group.builder().id(NON_EXISTENT_ID).groupName(NON_EXISTENT_GROUP_NAME).build();

		Student student = Student.builder()
				.group(detachedGroup)
				.firstName(DEFAULT_FIRST_NAME)
				.lastName(DEFAULT_LAST_NAME)
				.build();

		assertThrows(ConstraintViolationException.class, () -> {
			studentDao.save(List.of(student));
			em.flush();
		}, "Expected ConstraintViolationException when saving student with non-existent group");
	}

	@Test
	@DisplayName("Save with null entity list should throw ValidationException")
	void saveWithNullEntityListShouldThrowException() {
		assertThrows(IllegalArgumentException.class,
				() -> studentDao.save(null),
				"Expected ValidationException for null entity list");
	}

	@Test
	@DisplayName("Save with empty entity list should throw ValidationException")
	void saveWithEmptyEntityListShouldThrowException() {
		assertThrows(IllegalArgumentException.class,
				() -> studentDao.save(List.of()),
				"Expected ValidationException for empty entity list");
	}

	@Test
	@DisplayName("Update should modify existing student")
	void updateShouldModifyStudent() {
		testStudent.setFirstName(UPDATED_FIRST_NAME);
		testStudent.setLastName(UPDATED_LAST_NAME);

		Student updated = studentDao.update(List.of(testStudent)).get(0);

		List<Student> fetched = studentDao.findByIds(List.of(updated.getId()));
		assertFalse(fetched.isEmpty(), "Updated student should be found");
		assertEquals(UPDATED_FIRST_NAME, fetched.get(0).getFirstName());
		assertEquals(UPDATED_LAST_NAME, fetched.get(0).getLastName());
	}

	@Test
	@DisplayName("Update non-existent student should throw EntityNotFoundException")
	void updateNonExistentStudentShouldThrowException() {
		Student nonExistent = Student.builder()
				.id(NON_EXISTENT_ID)
				.group(testGroup)
				.firstName(UPDATED_FIRST_NAME)
				.lastName(UPDATED_LAST_NAME)
				.build();

		assertThrows(IdAwareEntityNotFoundException.class,
				() -> studentDao.update(List.of(nonExistent)),
				"Expected EntityNotFoundException for non-existent student");
	}

	@Test
	@DisplayName("Updating student with non-existent group should throw ConstraintViolationException")
	void updateStudentWithNonExistentGroupShouldThrowException() {
		Group detachedGroup = Group.builder().id(NON_EXISTENT_ID).groupName(NON_EXISTENT_GROUP_NAME).build();
		testStudent.setGroup(detachedGroup);

		assertThrows(EntityNotFoundException.class,
				() -> studentDao.update(List.of(testStudent)),
				"Expected EntityNotFoundException when updating student with non-existent group");

		testStudent.setGroup(testGroup);
	}

	@Test
	@DisplayName("Update with null entity list should throw ValidationException")
	void updateWithNullEntityListShouldThrowException() {
		assertThrows(IllegalArgumentException.class,
				() -> studentDao.update(null),
				"Expected ValidationException for null entity list");
	}

	@Test
	@DisplayName("Update with empty entity list should throw ValidationException")
	void updateWithEmptyEntityListShouldThrowException() {
		assertThrows(IllegalArgumentException.class,
				() -> studentDao.update(List.of()),
				"Expected ValidationException for empty entity list");
	}

	@Test
	@DisplayName("Find by ID should return empty if student does not exist")
	void findByIdShouldReturnEmptyOptionalIfNotFound() {
		List<Student> result = studentDao.findByIds(List.of(NON_EXISTENT_ID));

		assertTrue(result.isEmpty(), "Expected empty optional for non-existent student");
	}

	@Test
	@DisplayName("Delete should remove student from database")
	void deleteShouldRemoveStudent() {
		Student student = studentDao.findByIds(List.of(testStudent.getId())).get(0);

		studentDao.deleteAll(List.of(student));

		List<Student> deleted = studentDao.findByIds(List.of(student.getId()));
		assertTrue(deleted.isEmpty(), "Student should be deleted");
	}

	@Test
	@DisplayName("Delete non-existent student should return empty list")
	void deleteNonExistentStudentShouldReturnEmptyList() {
		Student detachedStudent = Student.builder()
				.id(NON_EXISTENT_ID)
				.group(testGroup)
				.firstName(DEFAULT_FIRST_NAME)
				.lastName(DEFAULT_LAST_NAME)
				.build();

		List<Student> deleted = studentDao.deleteAll(List.of(detachedStudent));

		assertTrue(deleted.isEmpty(), "Deleting non-existent student should return empty list");
	}

	@Test
	@DisplayName("Add course to student and verify association is established")
	void addCourseToStudentShouldEstablishRelation() {
		Set<Course> initialCourses = studentDao.findCoursesOfStudent(testStudent);
		assertTrue(initialCourses.isEmpty(), "Student should initially have no courses");

		studentDao.addCoursesToStudent(testStudent, Set.of(testCourse));

		Set<Course> updatedCourses = studentDao.findCoursesOfStudent(testStudent);
		assertEquals(ONE_COURSE_PROCESSED, updatedCourses.size(), "Student should have one course after adding");
		assertTrue(updatedCourses.contains(testCourse), "Student should be enrolled in the test course");
	}

	@Test
	@DisplayName("Adding non-existent course to student should throw EntityIdNotFoundException")
	void addNonExistentCourseToStudentShouldThrowException() {
		Course detachedCourse = Course.builder().id(NON_EXISTENT_ID).courseName(NON_EXISTENT_COURSE_NAME).build();

		assertThrows(EntityNotFoundException.class,
				() -> studentDao.addCoursesToStudent(testStudent, Set.of(detachedCourse)),
				"Expected EntityIdNotFoundException when adding non-existent course");
	}

	@Test
	@DisplayName("Remove course from student and verify association is removed")
	void removeCourseFromStudentShouldRemoveRelation() {
		studentDao.addCoursesToStudent(testStudent, Set.of(testCourse));
		Set<Course> beforeRemoval = studentDao.findCoursesOfStudent(testStudent);
		assertTrue(beforeRemoval.contains(testCourse), "Student should be enrolled in the course before removal");

		studentDao.removeCoursesFromStudent(testStudent, Set.of(testCourse));

		Set<Course> afterRemoval = studentDao.findCoursesOfStudent(testStudent);
		assertFalse(afterRemoval.contains(testCourse), "Student should not be enrolled in the course after removal");
	}
}
