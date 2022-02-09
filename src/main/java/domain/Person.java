package domain;

import domain.type.SchoolRank;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "student")
@Getter
@NoArgsConstructor
public class Person {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "school_id")
    private String idNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "rank")
    private SchoolRank rank;

    @Column(name = "name")
    private String name;

    @Column
    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_class") // 연관관계의 주인
    @Setter
    private SchoolClass myClass;

    public void changeName(String name) {
        this.name = name;
    }
    @Builder
    public Person(String name, String email, String idNumber, SchoolRank rank) {
        this.idNumber = idNumber;
        this.rank = rank;
        this.name = name;
        this.email = email;
    }
}
