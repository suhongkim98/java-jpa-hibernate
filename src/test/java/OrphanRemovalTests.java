import domain.Person;
import domain.School;
import domain.SchoolClass;
import domain.type.SchoolRank;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

/**
 * OrphanRemoval 기능은 애그리거트 안에서 엔티티 간에서만 사용하는 것이 좋다.
 */
public class OrphanRemovalTests {
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
        for (int i = 0; i < 30; i++) {
            Person person = Person.builder()
                    .rank(SchoolRank.STUDENT)
                    .email(i + "@naver.com")
                    .name("tiger반 이름" + i)
                    .build();
            tiger.addPerson(person);
        }
        for (int i = 0; i < 30; i++) {
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
    public void 고아객체_테스트() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        tx.begin();

        //given
        SchoolClass class1 = em.find(SchoolClass.class, 1L);
        List<Person> personList1 = class1.getPersonList(); // 기존 해당 반에 속한 리스트

        Person person = em.find(Person.class, personList1.get(0).getId());
        Assert.assertNotNull(person);
        em.remove(class1);

        person = em.find(Person.class, personList1.get(0).getId()); // 재검색
        Assert.assertNull(person); // 반이 삭제되며 해당 반에 속한 person 엔티티들이 고아객체가 되어 설정에 따라 사라짐
        tx.commit();
    }

    @Test
    public void 부모의_부모가_지워져도_고아_객체로_판단해서_삭제됨() {
        /**
         * School -> SchoolClass는 cascade Remove 전이
         * SchoolClass -> Person은 Orphan Removal 작동
         */
        EntityManager em = emf.createEntityManager();

        School school = em.find(School.class, 1L);
        Assert.assertNotNull(school); // 조회성공
        List<SchoolClass> classList = school.getClassList();
        Assert.assertNotEquals(0, classList.size()); // 반 개수는 0이 아님, 즉 존재한다.
        em.remove(school); // school 삭제되며 속한 반도 remove 전이

        // 해당 학교에 있던 반 조회 시도
        SchoolClass class1 = em.find(SchoolClass.class, classList.get(0).getId());
        SchoolClass class2 = em.find(SchoolClass.class, classList.get(1).getId());

        // 학교가 삭제되며 remove 전이에 의해 학교 내에 존재하던 호랑이반 토끼반 다 삭제됨
        Assert.assertNull(class1);
        Assert.assertNull(class2);

        // SchoolClass의 orphan removal이 동작해서 person도 삭제됨
        Person person1 = em.find(Person.class, classList.get(0).getPersonList().get(0).getId());
        Assert.assertNull(person1);
    }

    /**
     * list remove 시 Orphan Removal 동작 상황
     */
    @Test
    public void collectionRemoveOrphanRemoval() {
        // 리스트 remove
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        try {
            // given
            SchoolClass schoolClass = SchoolClass.builder()
                    .name("해바라기반")
                    .build();
            Person person1 = Person.builder()
                    .name("길동이")
                    .build();
            Person person2 = Person.builder()
                    .name("길동이")
                    .build();
            schoolClass.addPerson(person1);
            schoolClass.addPerson(person2);
            em.persist(schoolClass); // cascade persist에 의해 해바라기반, 학생들 엔티티 persist // 이건 더티체킹과 별개의 cascade 쪽임

            // when
            // 학생 2명 리스트에서 제거
            schoolClass.removePerson(person1);
            schoolClass.removePerson(person2);
            System.out.println("^^orphan removal 발생 전^^");
            em.flush(); // flush 발생으로 인해 더티채킹 발생
            System.out.println("^^orphan removal 발생 후^^");
            // orphan removal 동작으로 인해 Person entity delete 쿼리가 2개 나갔음

            tx.commit(); // commit 전에 flush 자동발생함
        } catch (Exception e) {
            tx.rollback();
        } finally {
            em.close();
        }
    }
}
