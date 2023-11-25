import domain.Person;
import domain.SchoolClass;
import domain.type.SchoolRank;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.List;

/*
jpql 예시
jpql 페이징 예시
typedQuery, query 예시
 */
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

        //given
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        SchoolClass tiger = SchoolClass.builder()
                .name("호랑이반")
                .build();
        for(int i = 0 ; i < 30 ; i++) {
            Person person = Person.builder()
                    .rank(SchoolRank.STUDENT)
                    .email(i + "@naver.com")
                    .name("tiger반 이름" + i)
                    .build();
            tiger.addPerson(person);
        }
        em.persist(tiger);
        tx.commit(); // commit 전 자동 flush
    }
    @AfterClass
    public static void afterClass() {
        //애플리케이션이 종료되면 공장을 닫는다.
        emf.close();
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

            Assert.assertEquals("홍길동", p.getName());
            Assert.assertEquals("201700000", p.getIdNumber());
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
        } finally {
            em.close();
        }
    }

    @Test
    public void jpqlPaging() {
        EntityManager em = emf.createEntityManager();
        List<Person> personList = em.createQuery("select p from Person p", Person.class)
                .setFirstResult(0)
                .setMaxResults(20)
                .getResultList();
        Assert.assertEquals(20, personList.size());
    }

    @Test
    public void TypedQueryAndQueryExample() {
        // 작성한 JPQL을 실행하려면 쿼리 객체를 만들어야 한다.
        // 쿼리 객체는 TypedQuery와 Query가 있는데 반환할 타입이 명확하게 지정 할 수 있으면 TypedQuery 아니면 Query를 선택하면 된다.
        // 즉 지금까지는 TypedQuery였고 Query예제를 알아보자
        EntityManager em = emf.createEntityManager();

        Query query = em.createQuery("select p.name, p.id from Person p where p.name like '%5%' ");
        List<Object[]> list = query.getResultList();
        for(Object[] objects : list) {
            for(int i = 0 ; i < objects.length ; i++) {
                System.out.print(objects[i] + " ");
            }
            System.out.println();
        }
    }

    @Test
    public void fromSubQueryExample() {
        // TODO : hibernate 6부터는 FROM 절의 서브쿼리를 지원한다.
    }
}
