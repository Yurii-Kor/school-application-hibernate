package ua.foxminded.schoolapplication.model.domain;

import jakarta.persistence.*;
import lombok.*;
import ua.foxminded.schoolapplication.model.validation.StringValidationParameters;

import java.util.Set;
import java.util.HashSet;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "courses")
public class Course implements Identifiable {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "course_id_seq")
	@SequenceGenerator(name = "course_id_seq", sequenceName = "course_id_seq", allocationSize = 1)
	@Column(name = "course_id", nullable = false)
	private Long id;

	@StringValidationParameters(minLength = 2, maxLength = 100, pattern = "^[A-Za-z0-9\\s\\-:,.'&]+$")
	@Column(nullable = false, unique = true)
	private String courseName;

	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	@StringValidationParameters(isNullPossible = true, maxLength = 1000)
	@Basic(fetch = FetchType.LAZY)
	@Column(columnDefinition = "TEXT")
	private String courseDescription;

	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	@Builder.Default
	@ManyToMany(mappedBy = "courses", fetch = FetchType.LAZY)
	private Set<Student> students = new HashSet<>();
}
