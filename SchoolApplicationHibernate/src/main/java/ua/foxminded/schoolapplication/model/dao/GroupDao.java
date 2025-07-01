package ua.foxminded.schoolapplication.model.dao;

import jakarta.transaction.Transactional;
import jakarta.validation.Validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import ua.foxminded.schoolapplication.model.domain.Group;

import java.util.List;

@Repository
@Transactional
public class GroupDao extends Dao<Group> {

	private static final Logger logger = LoggerFactory.getLogger(GroupDao.class);

	private static final String FIND_GROUPS_WITH_STUDENT_COUNT_LESS_OR_EQUAL = """
			SELECT DISTINCT g
			FROM Group g
			LEFT JOIN FETCH g.students
			WHERE g.id IN (
			    SELECT g2.id
			    FROM Group g2
			    LEFT JOIN g2.students s2
			    GROUP BY g2.id, g2.groupName
			    HAVING COUNT(s2.id) <= :maxStudents
			)
			""";

	private static final String PARAMETER_MAX_STYDENTS = "maxStudents";

	public GroupDao(Validator validator) {
		super(Group.class, validator);
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
