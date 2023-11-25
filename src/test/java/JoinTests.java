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
ManyToOne join 예시
OneToMany join 예시
join on 예시
theta join 예시
 */
public class JoinTests {
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

    // sql join과 마찬가지로 무언가 조건 걸고 검색하고자 할 때 사용
    // 일반 조인은 N + 1 문제 해결과는 전혀 관련 없다. (N+1은 fetch join 이나 BatchSize 설정으로 해결)
    @Test
    public void manyToOneInnerJoinExample() {
        // 다대일 조회
        EntityManager em = emf.createEntityManager();

        String query = "select p from Person p " +
                "inner join p.myClass c ";
        //query = "select p from Person p "; // 조건문이 없으니 위와 아래는 결과가 완전 동일

        // myClass 가 즉시로딩이였다면 이 시점에 쿼리 3번 나감, 지연로딩이기에 createQuery 에 1번 나가고 프록시로 받아서 호출시점에 쿼리 2번 더 나감
        List<Person> personList = em.createQuery(query, Person.class).getResultList();

        System.out.println("쉬고~"); // 즉시로딩과 지연로딩에 따라 이게 언제 호출되나 확인해보자

        for(Person person : personList) {
            System.out.println(person.getMyClass().getName()); // 호랑이반, 토끼반해서 2번 나감
        }
    }
    @Test
    public void oneToManyInnerJoinExample() {
        // hibernate 6로 업그레이드하며 5버전과 다른 중대한 변경점이 있었다.
        // hibernate 5까지는 OneToMany에 해당하는 엔티티와 조인을 걸면 가짜 데이터가 생성되어 별도로 distinct 키워드를 통해 중복제거 하는 과정이 필요했다.
        // hibernate 6부터는 별도로 distinct를 붙이지 않아도 가짜 데이터가 생성되지 않는다.
        EntityManager em = emf.createEntityManager();
        String query = "select c from SchoolClass c " +
                "inner join c.personList p " +
                "where p.name like :name ";
        List<SchoolClass> classList = em.createQuery(query, SchoolClass.class)
                .setParameter("name", "%반%")
                .getResultList(); // 사람 이름에 반 이 들어가면 조회 (모든 사람 이름에 반이 들어가므로 전체 조회)

        Assert.assertEquals(2, classList.size()); // 중복이 제거된 2개만 조회
        System.out.println("classListSize: " + classList.size());

        for(SchoolClass schoolClass : classList) {
            System.out.println(schoolClass.getName());
        }
    }
    @Test
    public void oneToManyCollectionInnerJoinExample() {
        // 컬렉션 조인은 distinct 만 붙여주면 된다.
        // 하지만 hibernate 6부터는 distinct를 붙여 컬렉션 조인하지 않아도 중복이 제거된다.
        EntityManager em = emf.createEntityManager();
        String query = "select distinct c from SchoolClass c " +
                "inner join c.personList p " +
                "where p.name like :name ";
        List<SchoolClass> classList = em.createQuery(query, SchoolClass.class)
                .setParameter("name", "%반%")
                .getResultList(); // 사람 이름에 반 이 들어가면 조회 (모든 사람 이름에 반이 들어가므로 전체 조회)

        Assert.assertEquals(2, classList.size()); // 호랑이반, 토끼반 2개 나온다.
        System.out.println("classListSize: " + classList.size());

        for(SchoolClass schoolClass : classList) {
            System.out.println(schoolClass.getPersonList().size());
        }
    }

    @Test
    public void joinOnExample() {
        // 기본적으로 조인 시 on을 쓰지 않아도 일반 sql에서 쓰듯이 pk fk로 on이 자동 설정된다.
        // JPA에서 on 절을 쓸 수 있는데 where과 on 의 차이는
        // where의 경우 조인을 "한 후"에 where 조건을 찾는거라면
        // on은 조인 "전"에 조인 대상 테이블에 and 로 조건을 걸어 추가적인 필터링을 하게된다.
        // where은 위에서 봤으니 join on을 보자

        EntityManager em = emf.createEntityManager();

        String query = "select p from Person p " +
                "inner join p.myClass c " +
                "on c.id < 2 "; // 토끼반은 id가 2이므로 join 전에 myClass 에서 토끼반이 미리 필터링됨

        List<Person> personList = em.createQuery(query, Person.class).getResultList();

        for(Person person : personList) {
            System.out.println(person.getMyClass().getName());
        }
    }
    @Test
    public void thetaJoinExample() {
        // 세타 조인을 통해 연관관계가 없는 엔티티간 조회가 가능하다
        // JPQL 도 객체지향 "쿼리언어" 이므로 물론 세타조인이 가능하다.
        // 사실 원래 세타 조인의 정의는 = 뿐만 아니라 <, > 등 다른 연산자로도 조건을 세울 수 있다.
        // 이 연산자들 중 = 연산자를 사용하는 세타조인을 특별히 동등 조인이라고 한다.
        // 더 좁은 범위로 말하면 JPQL 은 세타 조인을 지원하는데 그 중 = 연산자를 사용하는 동등조인만 지원한다고 할 수 있다.
        // 세타 조인을 하게 되면 각 행과 상대방 테이블의 행을 모두 조인하는 Cartesian product를 수행하게 된다.

        EntityManager em = emf.createEntityManager();
        List<Person> personList = em.createQuery("select p from Person p left join Person p2 on p.myClass = p2.myClass", Person.class)
                .getResultList();
        System.out.println(personList.size());
    }
}