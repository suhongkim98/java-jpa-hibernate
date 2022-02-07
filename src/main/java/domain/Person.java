package domain;

import domain.type.SchoolRank;
import lombok.Builder;
import lombok.Getter;

import javax.persistence.*;

@Entity
@Table(name = "student")
@Getter
public class Person {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "school_id")
    private String idNumber;

    @Column(name = "rank")
    private SchoolRank rank;

    @Column(name = "name")
    private String name;

    @Column
    private String email;

    @Builder
    public Person(String name, String email, String idNumber, SchoolRank rank) {
        this.idNumber = idNumber;
        this.rank = rank;
        this.name = name;
        this.email = email;
    }
}
