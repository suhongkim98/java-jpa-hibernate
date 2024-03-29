import domain.Person;
import domain.School;
import domain.SchoolClass;
import domain.type.SchoolRank;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import java.util.List;

/**
 * cascade 는 애그리거트 안에서 엔티티 간에서만 사용하는 것이 좋다.
 * 본인의 상태를 연관 엔티티에게 전파하고 싶을 때 사용
 */
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
        EntityTransaction tx = em.getTransaction();
        tx.begin();

        School school = em.find(School.class, 1L);
        Assert.assertNotNull(school); // 조회성공
        List<SchoolClass> classList = school.getClassList();
        Assert.assertNotEquals(0,  classList.size()); // 반 개수는 0이 아님, 즉 존재한다.
        em.remove(school); // school 삭제되며 속한 반도 remove 전이

        // 해당 학교에 있던 반 조회 시도
        SchoolClass class1 = em.find(SchoolClass.class, classList.get(0).getId());
        SchoolClass class2 = em.find(SchoolClass.class, classList.get(1).getId());

        // 학교가 삭제되며 remove 전이에 의해 학교 내에 존재하던 호랑이반 토끼반 다 삭제됨
        Assert.assertNull(class1);
        Assert.assertNull(class2);

        tx.commit();
    }

    /**
     * 리스트 add 후 cascade.PERSIST 상황
     */
    @Test
    public void collectionAddCascade() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        try {
            SchoolClass schoolClass = SchoolClass.builder()
                    .name("해바라기반")
                    .build();
            em.persist(schoolClass); // 일단 OneToMany 중 One에 해당하는 엔티티 persist

            // 비영속 엔티티 생성(Many에 해당하는 엔티티)
            Person person = Person.builder()
                    .name("길동이")
                    .build();
            // 리스트에 add
            schoolClass.addPerson(person);

            System.out.println("^^cascade persist 발생 전^^");
            em.flush(); // flush 발생, cascade persist 동작하여 리스트에 있는 비영속 엔티티도 영속화됨
            System.out.println("^^cascade persist 발생 전^^");
            // cascade PERSIST 인해 Person entity insert 쿼리가 나갔음

            tx.commit(); // commit 전에 flush 자동발생함
        } catch (Exception e) {
            tx.rollback();
        } finally {
            em.close();
        }
    }
}
