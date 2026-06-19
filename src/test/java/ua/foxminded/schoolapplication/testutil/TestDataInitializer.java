package ua.foxminded.schoolapplication.testutil;

import java.util.List;

import org.springframework.stereotype.Component;

import jakarta.transaction.Transactional;
import ua.foxminded.schoolapplication.model.dao.CourseDao;
import ua.foxminded.schoolapplication.model.dao.GroupDao;
import ua.foxminded.schoolapplication.model.dao.StudentDao;
import ua.foxminded.schoolapplication.model.domain.Course;
import ua.foxminded.schoolapplication.model.domain.Group;
import ua.foxminded.schoolapplication.model.domain.Student;

@Component
public class TestDataInitializer {

	private final GroupDao groupDao;
	private final StudentDao studentDao;
	private final CourseDao courseDao;

	public TestDataInitializer(GroupDao groupDao, StudentDao studentDao, CourseDao courseDao) {
		this.groupDao = groupDao;
		this.studentDao = studentDao;
		this.courseDao = courseDao;
	}

	@Transactional
	public TestEntities initialize(List<Group> rawGroups, List<Course> rawCourses, List<Student> rawStudents) {
		List<Group> savedGroups = rawGroups.isEmpty() ? List.of() : groupDao.save(rawGroups);
		List<Course> savedCourses = rawCourses.isEmpty() ? List.of() : courseDao.save(rawCourses);
		List<Student> savedStudents = rawStudents.isEmpty() ? List.of() : studentDao.save(rawStudents);

		return new TestEntities(savedGroups, savedCourses, savedStudents);
	}
}
