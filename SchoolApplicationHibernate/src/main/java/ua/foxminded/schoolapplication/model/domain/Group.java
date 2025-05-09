package ua.foxminded.schoolapplication.model.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import ua.foxminded.schoolapplication.model.validation.StringValidationParameters;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "groups")
public class Group implements Identifiable {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "group_id_seq")
	@SequenceGenerator(name = "group_id_seq", sequenceName = "group_id_seq", allocationSize = 1)
	@Column(name = "group_id", nullable = false)
	private Long id;

	@Column(nullable = false, unique = true)
	@StringValidationParameters(minLength = 3, maxLength = 21, pattern = "^[A-Za-z]+-\\d+$")
	private String groupName;

	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	@Builder.Default
	@OneToMany(mappedBy = "group", fetch = FetchType.LAZY)
	private List<Student> students = List.of();
}
