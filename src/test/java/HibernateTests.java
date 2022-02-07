import domain.Person;
import domain.type.SchoolRank;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import java.util.List;

public class HibernateTests {
    private static EntityManagerFactory emf;
    @BeforeClass
    public static void beforeClass(){
        // 애플리케이션이 실행되면 처음에
        // persistence.xml 설정 정보를 조회하여 EntityManagerFactory를 생성한다.
        // EntityManagerFactory는 엔티티매니저를 생성하는 역할을 수행한다.
        // 엔티티매니저는 트랜잭션 단위로 업무를 처리할 때마다 엔티티매니저를 새로 만들어서 사용한다.
        // 즉 엔티티매니저팩토리는 하나만 생성해서 공유하고 엔티티매니저팩토리에서 엔티티매니저를 생성하는 것
        emf = Persistence.createEntityManagerFactory("hello-h2");
    }
    @AfterClass
    public static void afterClass() {
        //애플리케이션이 종료되면 공장을 닫는다.
        emf.close();
    }
    @Test
    public void persistExample() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        try {
            Person person = Person.builder()
                    .idNumber("201700000")
                    .email("zzz@naver.com")
                    .name("홍길동")
                    .rank(SchoolRank.STUDENT)
                    .build();
            em.persist(person);

            tx.commit();
        } catch (Exception e) {
            tx.rollback();
        } finally {
            em.close();
        }
    }

    @Test
    public void jpqlExample() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        try {
            Person person = Person.builder()
                    .idNumber("201700000")
                    .email("zzz@naver.com")
                    .name("홍길동")
                    .rank(SchoolRank.STUDENT)
                    .build();
            em.persist(person);

            //jpql 쿼리가 나가기 전 flush 가 자동으로 발생한다.
            String query = "select p from Person p where p.name = :name";
            Person p = em.createQuery(query, Person.class)
                    .setParameter("name", "홍길동")
                    .getSingleResult();

            Assert.assertEquals(p.getName(), "홍길동");
            Assert.assertEquals(p.getIdNumber(), "201700000");
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
        } finally {
            em.close();
        }
    }

}
