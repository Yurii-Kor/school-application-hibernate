package ua.foxminded.schoolapplication.model.dao;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.hibernate.exception.ConstraintViolationException;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import ua.foxminded.schoolapplication.model.domain.Group;
import ua.foxminded.schoolapplication.model.domain.Student;
import ua.foxminded.schoolapplication.testutil.TestDataInitializer;
import ua.foxminded.schoolapplication.testutil.TestEntities;
import ua.foxminded.schoolapplication.testutil.TestcontainersConfiguration;
import ua.foxminded.schoolapplication.model.dao.exception.FieldConstraintException;
import ua.foxminded.schoolapplication.config.ValidatorConfig;
import ua.foxminded.schoolapplication.model.dao.exception.IdAwareEntityNotFoundException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestInstance(Lifecycle.PER_CLASS)
@Testcontainers
@Import({ GroupDao.class, StudentDao.class, CourseDao.class, ValidatorConfig.class, TestcontainersConfiguration.class,
		TestDataInitializer.class })

class GroupDaoTest {

	static final String EMPTY_GROUP_NAME = "EmptyGroup-11";
	static final String FULL_GROUP_NAME = "FullGroup-22";
	static final String DEFAULT_GROUP_NAME = "TestGroup-33";
	static final String UPDATED_GROUP_NAME = "UpdatedGroup-44";
	static final String[] STUDENT_FIRST_NAMES = { "John", "Jane" };
	static final String[] STUDENT_LAST_NAMES = { "Doe", "Smith" };
	static final String UNSAVED_NAME = null;

	static final Long NON_EXISTENT_ID = 999L;
	static final int ONE_STUDENT_PROCESSED = 1;

	private static final String SELECT_EXISTING_ENTITIES = "SELECT e FROM %s e WHERE e.id IN :ids";
	private static final String PARAMETER_IDS = "ids";

	@Autowired
	private EntityManager em;

	@Autowired
	private TestDataInitializer initializer;

	@Autowired
	private GroupDao groupDao;

	private Group emptyGroup;
	private Group fullGroup;

	@BeforeAll
	void setup() {
		List<Group> inputGroups = List.of(Group.builder().groupName(EMPTY_GROUP_NAME).build(),
				Group.builder().groupName(FULL_GROUP_NAME).build());

		List<Student> inputStudents = List.of(
				Student.builder()
						.group(inputGroups.get(1))
						.firstName(STUDENT_FIRST_NAMES[0])
						.lastName(STUDENT_LAST_NAMES[0])
						.build(),

				Student.builder()
						.group(inputGroups.get(1))
						.firstName(STUDENT_FIRST_NAMES[1])
						.lastName(STUDENT_LAST_NAMES[1])
						.build());

		TestEntities entities = initializer.initialize(inputGroups, List.of(), inputStudents);

		emptyGroup = entities.groups().get(0);
		fullGroup = entities.groups().get(1);
	}

	@Test
	@DisplayName("Save and retrieve a group")
	void saveShouldSaveAndFindGroup() {
		Group saved = groupDao.save(List.of(Group.builder().groupName(DEFAULT_GROUP_NAME).build())).get(0);

		assertNotNull(saved.getId(), "Group ID should not be null after saving");
		List<Group> fetched = groupDao.findByIds(List.of(saved.getId()));
		assertFalse(fetched.isEmpty(), "Group should be found by ID");
		assertEquals(DEFAULT_GROUP_NAME, saved.getGroupName());
	}

	@Test
	@DisplayName("Save duplicate group name should throw PersistenceException")
	void saveShouldThrowExceptionOnDuplicateGroupName() {
		Group originalGroup = Group.builder().groupName(DEFAULT_GROUP_NAME).build();
		Group duplicateGroup = Group.builder().groupName(DEFAULT_GROUP_NAME).build();

		groupDao.save(List.of(originalGroup));

		PersistenceException exception = assertThrows(PersistenceException.class, () -> {
			groupDao.save(List.of(duplicateGroup));
			em.flush();
		}, "Saving a group with duplicate groupName should throw PersistenceException");

		System.out.println("Caught expected exception: " + exception.getMessage());
	}

	@Test
	@DisplayName("Saving two groups with same name in one call should throw PersistenceException")
	void saveTwoGroupsWithDuplicateNameShouldThrowException() {
		Group originalGroup = Group.builder().groupName(DEFAULT_GROUP_NAME).build();
		Group duplicateGroup = Group.builder().groupName(DEFAULT_GROUP_NAME).build();

		PersistenceException exception = assertThrows(PersistenceException.class, () -> {
			groupDao.save(List.of(originalGroup, duplicateGroup));
			em.flush();
		}, "Saving two groups with duplicate groupName in one call should throw PersistenceException");

		System.out.println("Caught expected exception: " + exception.getMessage());
	}

	@Test
	@DisplayName("Saving group with null name should throw ValidationException")
	void saveShouldThrowExceptionIfGroupNameIsNull() {
		Group invalidGroup = Group.builder().groupName(UNSAVED_NAME).build();

		assertThrows(FieldConstraintException.class,
				() -> groupDao.save(List.of(invalidGroup)),
				"Expected ValidationException when group name is null");
	}

	@Test
	@DisplayName("Update (merge) should modify existing group")
	void updateShouldModifyExistingGroup() {
		List<Group> currentGroups = groupDao.findByIds(List.of(emptyGroup.getId(), fullGroup.getId()));

		currentGroups.get(0).setGroupName(DEFAULT_GROUP_NAME);
		currentGroups.get(1).setGroupName(UPDATED_GROUP_NAME);

		List<Group> updatedGroups = groupDao.update(currentGroups);

		List<Group> groupGotDefaultName = groupDao.findByIds(List.of(updatedGroups.get(0).getId()));
		assertFalse(groupGotDefaultName.isEmpty(), "Updated group should be found");
		assertEquals(DEFAULT_GROUP_NAME, groupGotDefaultName.get(0).getGroupName(), "Group name should be updated");

		List<Group> groupGotUpdatedName = groupDao.findByIds(List.of(updatedGroups.get(1).getId()));
		assertFalse(groupGotUpdatedName.isEmpty(), "Updated group should be found");
		assertEquals(UPDATED_GROUP_NAME, groupGotUpdatedName.get(0).getGroupName(), "Group name should be updated");
	}

	@Test
	@DisplayName("Update non-existent group should throw EntityNotFoundException")
	void updateShouldThrowExceptionIfGroupNotFound() {
		Group nonExistent = Group.builder().id(NON_EXISTENT_ID).groupName(UPDATED_GROUP_NAME).build();

		assertThrows(IdAwareEntityNotFoundException.class,
				() -> groupDao.update(List.of(nonExistent)),
				"Expected exception when updating non-existent group");

	}

	@Test
	@DisplayName("Delete an existing group")
	void deleteShouldRemoveGroupById() {
		List<Group> fetched = groupDao.findByIds(List.of(emptyGroup.getId()));
		groupDao.deleteAll(List.of(fetched.get(0)));

		List<Group> deleted = em
				.createQuery(String.format(SELECT_EXISTING_ENTITIES, Group.class.getSimpleName()), Group.class)
				.setParameter(PARAMETER_IDS, List.of(emptyGroup.getId()))
				.getResultList();

		assertTrue(deleted.isEmpty(), "Group should no longer exist after deletion");
	}

	@Test
	@DisplayName("Delete non-existent group should return empty list")
	void deleteShouldReturnEmptyListIfGroupNotFound() {
		Group nonExistent = Group.builder().id(NON_EXISTENT_ID).groupName(DEFAULT_GROUP_NAME).build();

		List<Group> deleted = groupDao.deleteAll(List.of(nonExistent));
		assertTrue(deleted.isEmpty(), "Deleting non-existent group should return empty list");
	}

	@Test
	@DisplayName("Deleting a group with existing students should throw ConstraintViolationException")
	void deleteGroupWithStudentsShouldThrowConstraintViolation() {
		assertNotNull(fullGroup.getId(), "Group ID must not be null");

		assertThrows(ConstraintViolationException.class, () -> {
			groupDao.deleteAll(groupDao.findByIds(List.of(fullGroup.getId())));
			em.flush();
		}, "Expected ConstraintViolationException due to existing students");
	}

	@Test
	@DisplayName("Find non-existent group should return empty Optional")
	void findByIdShouldReturnEmptyIfNotFound() {
		List<Group> result = groupDao.findByIds(List.of(NON_EXISTENT_ID));

		assertTrue(result.isEmpty(), "Expected empty Optional when group is not found");
	}

	@Test
	@DisplayName("Find groups with student count <= N should include only qualifying groups")
	void findGroupsWithStudentCountLessOrEqualShouldReturnCorrectGroups() {
		List<Group> result = groupDao.findGroupsWithStudentCountLessOrEqual(ONE_STUDENT_PROCESSED);

		assertTrue(result.stream().anyMatch(g -> g.getId().equals(emptyGroup.getId())),
				"Empty group should be included");

		assertTrue(result.stream().noneMatch(g -> g.getId().equals(fullGroup.getId())),
				"Full group should not be included");
	}
}