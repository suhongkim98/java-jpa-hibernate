import domain.Person;
import domain.School;
import domain.SchoolClass;
import domain.type.SchoolRank;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.transaction.Transactional;
import java.util.List;

// cascade, 고아객체 테스트
public class CascadeTests {
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
    @AfterClass
    public static void afterClass() {
        emf.close();
    }

    @Test
    public void REMOVE_전이_테스트() {
        EntityManager em = emf.createEntityManager();

        School school = em.find(School.class, 1L);
        Assert.assertNotNull(school); // 조회성공
        List<SchoolClass> classList = school.getClassList();
        Assert.assertNotEquals(classList.size(), 0); // 반 개수는 0이 아님, 즉 존재한다.
        em.remove(school); // school 삭제되며 속한 반도 remove 전이

        // 해당 학교에 있던 반 조회 시도
        SchoolClass class1 = em.find(SchoolClass.class, classList.get(0).getId());
        SchoolClass class2 = em.find(SchoolClass.class, classList.get(1).getId());

        Assert.assertNull(class1);
        Assert.assertNull(class2); // 학교가 삭제되며 remove 전이에 의해 학교 내에 존재하던 호랑이반 토끼반 다 삭제됨
    }
    @Test
    public void 고아객체_테스트() {
        EntityManager em = emf.createEntityManager();
        //given
        SchoolClass class1 = em.find(SchoolClass.class, 1L);
        List<Person> personList1 = class1.getPersonList(); // 기존 해당 반에 속한 리스트

        em.remove(class1);
        Person person = em.find(Person.class, personList1.get(0).getId());
        Assert.assertNull(person); // 반이 삭제되며 해당 반에 속한 person 엔티티들이 고아객체가 되어 설정에 따라 사라짐
    }
}
