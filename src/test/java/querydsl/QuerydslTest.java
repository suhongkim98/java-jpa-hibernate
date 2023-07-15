package querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import domain.Person;
import domain.QSchool;
import domain.School;
import domain.SchoolClass;
import domain.type.SchoolRank;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class QuerydslTest {

    private static EntityManagerFactory emf;
    @BeforeClass
    public static void beforeClass() {
        emf = Persistence.createEntityManagerFactory("hello-h2");

        // given
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        School school = School.builder()
                .name("떡잎유치원")
                .build();
        SchoolClass tiger = SchoolClass.builder()
                .name("호랑이반")
                .build();
        SchoolClass rabbit = SchoolClass.builder()
                .name("토끼반")
                .build();
        school.addClass(tiger);
        school.addClass(rabbit);
        for(int i = 0 ; i < 30 ; i++) {
            Person person = Person.builder()
                    .rank(SchoolRank.STUDENT)
                    .email(i + "@naver.com")
                    .name("tiger반 이름" + i)
                    .build();
            tiger.addPerson(person);
        }
        for(int i = 0 ; i < 30 ; i++) {
            Person person = Person.builder()
                    .rank(SchoolRank.STUDENT)
                    .email(i + "@naver.com")
                    .name("rabbit반 이름" + i)
                    .build();
            rabbit.addPerson(person);
        }

        em.persist(school); // 영속성전이에 의해 연관된 엔티티도 persist
        tx.commit();
    }

    @Test
    public void basicQueryDslTest() {
        EntityManager em = emf.createEntityManager();
        JPAQueryFactory jpaQueryFactory = new JPAQueryFactory(em);

        QSchool s = new QSchool("s");
        School school = jpaQueryFactory
                .select(s)
                .from(s)
                .where(s.name.eq("떡잎유치원"))
                .fetchOne();

        Assert.assertNotNull(school);
    }
}
