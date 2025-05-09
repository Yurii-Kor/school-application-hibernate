package ua.foxminded.schoolapplication.model.dao;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import ua.foxminded.schoolapplication.model.dao.util.EntityExistenceValidator;
import ua.foxminded.schoolapplication.model.domain.Group;
import ua.foxminded.schoolapplication.model.validation.EntityValidator;

import java.util.List;

@Repository
@Transactional
public class GroupDao extends Dao<Group> {

	private static final Logger logger = LoggerFactory.getLogger(GroupDao.class);

	private static final String FIND_GROUPS_WITH_STUDENT_COUNT_LESS_OR_EQUAL = "SELECT g FROM Group g LEFT JOIN g.students s GROUP BY g.id, g.groupName HAVING COUNT(s.id) <= :maxStudents";

	private static final String PARAMETER_MAX_STYDENTS = "maxStudents";

	public GroupDao(EntityValidator<Group> validator, EntityExistenceValidator<Group> groupExistenceValidator) {
		super(Group.class, validator, groupExistenceValidator);
	}

	@Transactional(Transactional.TxType.SUPPORTS)
	public List<Group> findGroupsWithStudentCountLessOrEqual(int maxStudents) {
		logger.debug("Finding groups with student count <= {}", maxStudents);

		List<Group> groups = em.createQuery(FIND_GROUPS_WITH_STUDENT_COUNT_LESS_OR_EQUAL, Group.class)
				.setParameter(PARAMETER_MAX_STYDENTS, (long) maxStudents)
				.getResultList();

		logger.info("Found [{}] groups with student count <= {}", groups.size(), maxStudents);
		return groups;
	}
}
