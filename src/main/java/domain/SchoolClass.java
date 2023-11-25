package domain;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "school_class")
@Getter
@NoArgsConstructor
public class SchoolClass {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    // mappedBy는 읽기전용 필드 // personList 에 담긴 엔티티에 대해 persist 영속성전이
    // orphanRemoval: 부모 엔티티와 연관관계가 끊어진 자식 엔티티를 고아객체로 보고 자동으로 삭제해주는 기능
    @OneToMany(mappedBy = "myClass", cascade = {CascadeType.PERSIST}, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Person> personList = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id") // 연관관계의 주인
    @Setter // 해당 필드는 세터 적용
    private School school;

    public void addPerson(Person person) {
        person.setMyClass(this); // 연관관계의 주인에 class 설정
        this.personList.add(person); // OOP 관점에서 봤을 때 얘도 설정해주어야 함
    }
    public void removePerson(Person person) {
        this.personList.remove(person);
    }
    public void changeName(String name) {
        this.name = name;
    }

    @Builder
    public SchoolClass(String name) {
        this.name = name;
    }
}
