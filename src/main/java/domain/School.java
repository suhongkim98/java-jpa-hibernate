package domain;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "school")
@Getter
@NoArgsConstructor
public class School {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @OneToMany(mappedBy = "school", cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, fetch = FetchType.EAGER) // classList 에 persist, remove 전이
    private List<SchoolClass> classList = new ArrayList<>();

    public void addClass(SchoolClass schoolClass) {
        schoolClass.setSchool(this); // 연관관계의 주인에 class 설정
        classList.add(schoolClass);
    }

    @Builder
    public School(String name) {
        this.name = name;
    }
}
