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
@Table(name = "students")
public class Student implements Identifiable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "student_id_seq")
    @SequenceGenerator(name = "student_id_seq", sequenceName = "student_id_seq", allocationSize = 1)
    @Column(name = "student_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @StringValidationParameters(minLength = 2, maxLength = 50, pattern = "^[A-Za-z]+$")
    @Column(nullable = false)
    private String firstName;

    @StringValidationParameters(minLength = 2, maxLength = 50, pattern = "^[A-Za-z]+$")
    @Column(nullable = false)
    private String lastName;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    @ManyToMany(
        cascade = {CascadeType.PERSIST, CascadeType.MERGE},
        fetch = FetchType.LAZY
    )
    @JoinTable(
        name = "students_courses",
        joinColumns = @JoinColumn(name = "student_id", nullable = false),
        inverseJoinColumns = @JoinColumn(name = "course_id", nullable = false)
    )
    private Set<Course> courses = new HashSet<>();
}
