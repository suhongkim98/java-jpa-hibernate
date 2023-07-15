import domain.Person;
import domain.SchoolClass;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;

/*
* 더티체킹 예제
* 일반적인 더티체킹(update)
* 리스트 add, remove(insert, delete)
 */
public class DirtyCheckingTests {

    private static EntityManagerFactory emf;
    @BeforeClass
    public static void beforeClass() {
        emf = Persistence.createEntityManagerFactory("hello-h2");
    }
    @AfterClass
    public static void afterClass() {
        emf.close();
    }

    @Test
    public void commonDirtyChecking() {
        // 일반적인 더티체킹
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        try {
            Person person = Person.builder()
                    .email("kkk@naver.com")
                    .name("길동이")
                    .build();

            em.persist(person); // persist

            Person check = em.find(Person.class, person.getId());
            check.changeName("두식이");
            System.out.println("^^더티채킹 발생 전^^");

            em.flush(); // flush 발생으로 인해 더티채킹 발생
            System.out.println("^^더티채킹 발생 후^^");
            // 더티채킹으로 인해 update 쿼리가 나갔음

            tx.commit(); // commit 전에 flush 자동발생함
        } catch (Exception e) {
            tx.rollback();
        } finally {
            em.close();
        }
    }
    @Test
    public void collectionAddDirtyChecking() {
        // 리스트 add 더티체킹
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

            System.out.println("^^더티채킹 발생 전^^");
            em.flush(); // flush 발생으로 인해 더티채킹 발생
            System.out.println("^^더티채킹 발생 후^^");
            // 더티채킹으로 인해 Person entity insert 쿼리가 나갔음

            tx.commit(); // commit 전에 flush 자동발생함
        } catch (Exception e) {
            tx.rollback();
        } finally {
            em.close();
        }
    }

    @Test
    public void collectionRemoveDirtyChecking() {
        // 리스트 remove 더티체킹
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
            System.out.println("^^더티채킹 발생 전^^");
            em.flush(); // flush 발생으로 인해 더티채킹 발생
            System.out.println("^^더티채킹 발생 후^^");
            // 더티채킹으로 인해 Person entity delete 쿼리가 2개 나갔음

            tx.commit(); // commit 전에 flush 자동발생함
        } catch (Exception e) {
            tx.rollback();
        } finally {
            em.close();
        }
    }
}
