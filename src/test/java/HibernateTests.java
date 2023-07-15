import domain.Person;
import domain.School;
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
엔티티 상태 detach, remove 예시
persist, merge 예시
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
            em.persist(person); // 영속화되며 insert 쓰기지연 발생

            tx.commit(); // commit 전에 flush 자동발생
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

    @Test
    public void jpqlPaging() {
        EntityManager em = emf.createEntityManager();
        List<Person> personList = em.createQuery("select p from Person p", Person.class)
                .setFirstResult(0)
                .setMaxResults(20)
                .getResultList();
        Assert.assertEquals(personList.size(), 20);
    }

    @Test
    public void 엔티티_상태_DETACH_예제() {
        //1차 캐시부터 쓰기 지연 SQL 저장소까지 해당 Entity를 관리하기 위한 정보가 제거된다. ( 한마디로 영속성 컨텍스트에서 더이상 해당 엔티티를 관리하지 않는다.)
        //비영속과 상태가 비슷해 영속성 컨텍스트 도움을 받을 수 없으니 지연 로딩도 불가
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction(); // flush 호출은 트랜잭션이 있어야 호출가능
        tx.begin();

        Person person = Person.builder()
                .idNumber("201700000")
                .email("zzz@naver.com")
                .name("홍길동")
                .rank(SchoolRank.STUDENT)
                .build();
        em.persist(person); // 엔티티가 영속화되며 영속성 컨텍스트에서 관리
        em.detach(person); // 엔티티를 준영속 상태로 변환
        person.changeName("두식이"); // flush 시 업데이트가 일어나기를 기대하지만 준영속이기에 되지 않을 것

        em.flush(); // 더티체킹 및 쓰기지연 쿼리 보내기
        Person entity = em.find(Person.class, person.getId());
        Assert.assertNotEquals(entity.getName(), "두식이"); // 더티체킹이 일어나지 않음

        tx.commit();
    }

    @Test
    public void 엔티티_상태_REMOVE_예제() {
        //리무브된 엔티티는 엔티티 상태가 삭제 상태임, 조회는 가능하다.
        EntityManager em = emf.createEntityManager();
        SchoolClass tiger = em.createQuery("select c from SchoolClass c where name = '호랑이반'", SchoolClass.class)
                .getSingleResult();
        Assert.assertNotNull(tiger);

        em.remove(tiger); // 삭제
        Assert.assertNotNull(tiger);
        System.out.println(tiger.getName()); // 삭제 상태인 엔티티지만 조회 가능
    }
    @Test
    public void MERGE_예제() {
        // persist, merge 모두 엔티티를 영속성 컨텍스트에서 관리하고 싶을 때 사용한다.
        // spring data JPA 에서는 해당 엔티티가 새로 생성된 것일 경우 persist 를 호출하고 아니면 merge 를 호출한다.
        // persist 는 새로 생성된 객체(비영속)를 영속화할 때 사용, 반환값 없고 해당 엔티티가 바로 영속상태가 됨
        // merge 는 비영속, 준영속(detached 된)을 병합하며 영속화할 때 사용, 반환값 있고 반환된 엔티티가 영속상태인 엔티티이다. (파라미터에 넣은 엔티티는 영속x)
        // 병합은 파라미터로 넘어온 Entity의 식별자 값으로 영속성 컨텍스트를 조회하고 찾는 Entity가 없다면 DB를 조회한다.
        // 만약에 Entity가 DB에 조차도 없다면 새로운 Entity를 생성해서 병합
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        tx.begin();

        // 시나리오1
        SchoolClass schoolClass1 = SchoolClass.builder()
                .name("aaa")
                .build();
        em.merge(schoolClass1);
        schoolClass1.changeName("bbb"); // school1은 비영속이므로 flush 시 더티체킹 발생하지 않음

        em.flush(); // commit, jpql 호출 전 자동발생하기도 하는 flush 호출, 더티체킹 진행
        SchoolClass class1 = em.createQuery("select c from SchoolClass c where name = :name", SchoolClass.class)
                .setParameter("name", "aaa")
                .getSingleResult();
        Assert.assertEquals(class1.getName(), "aaa"); // 업데이트 되지 않음을 확인
        System.out.println(class1.getName());

        // 시나리오2
        SchoolClass schoolClass2 = SchoolClass.builder()
                .name("aaazz")
                .build();
        SchoolClass entity = em.merge(schoolClass2);
        entity.changeName("bbbzz"); // entity 는 영속이므로 flush 시 더티체킹 발생

        em.flush(); // commit, jpql 호출 전 자동발생하기도 하는 flush 호출, 더티체킹 진행
        SchoolClass class2 = em.createQuery("select c from SchoolClass c where name = :name", SchoolClass.class)
                .setParameter("name", "bbbzz")
                .getSingleResult();
        Assert.assertEquals(class2.getName(), "bbbzz"); // 업데이트 됨을 확인
        System.out.println(class2.getName());
        tx.commit();
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
}
