import domain.Person;
import domain.SchoolClass;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;

/*
* 더티체킹 예제
* 일반적인 더티체킹(update)
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

    /**
     * 일반적인 더티체킹
     * 일반 더티체킹으로 연관관계 컬렉션 추가, 삭제까지 변화를 감지하지 못함, 이는 cascade, orphan removal과 관계있음
     */
    @Test
    public void commonDirtyChecking() {
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
    public void ID가_동일하면_더티체킹_발생() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        try {
            Person person = Person.builder()
                    .email("kkk@naver.com")
                    .name("길동이")
                    .build();

            person = em.merge(person); // merge 수행 후 결과를 다시 담음

            Person newPerson = Person.builder()
                    .id(person.getId()) // 위에서 받은 id 삽입
                    .email("google@google.com")
                    .name("두식이")
                    .build();
            newPerson = em.merge(newPerson);

            // 영속성 컨텍스트에 동일한 id가 있음으로 merge 시 같은 객체를 꺼내옴
            Assert.assertEquals(person, newPerson);
            // 또한 더티체킹 발생함
            Assert.assertEquals("두식이", person.getName());

            tx.commit(); // commit 전에 flush 자동발생함
        } catch (Exception e) {
            tx.rollback();
        } finally {
            em.close();
        }
    }
}
