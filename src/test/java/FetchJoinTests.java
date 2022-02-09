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
import java.util.List;

/*
N + 1 예시
페치 조인 예시
컬렉션 페치 조인 예시
컬렉션 페치 조인 시 페이징 주의점 및 해결법
 */
public class FetchJoinTests {
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

    // N + 1이란 조회 시 1개의 쿼리를 생각하고 설계를 했으나 엔티티 조회 시 1회, 그리고 해당 엔티티의 연관관계 필드를 조회하는 과정에서 예상치 못한 쿼리가 추가로 N 회 나가 N + 1회 쿼리가 나가는 것을 의미
    // N+1은 언제 발생하나요? 1. 지연로딩에 의한 프록시 객체 메서드 호출 시 2. 일반적으로 JPQL 사용 시
    @Test
    public void 모든_엔티티가_지연_로딩_일_때_N_PLUS_1_예제() {
        EntityManager em = emf.createEntityManager();
        // 관련된 모든 엔티티의 모든 연관관계 필드가 lazy loading이라고 해보자. 이 상황은 가장 기본적인 N + 1 문제이다.

        // 이 상황에서 엔티티 조회 시 1회 쿼리가 나가고 lazy 설정된 연관 엔티티들은 프록시 객체 상태이다.
        // 해당 프록시 객체의 메서드가 사용될 때 N회 쿼리가 추가로 나간다.
        SchoolClass class1 = em.find(SchoolClass.class, 1L); // query 1회 발생, 연관된 엔티티는 조회하지않고 lazy 정책에 따라 프록시 객체상태
        Assert.assertNotNull(class1);
        List<Person> personList = class1.getPersonList(); // 리스트의 경우 리스트 사이즈를 얻기 위해 쿼리를 날려야하므로 이때 1회 발생
        Assert.assertNotNull(personList);

        // 모든 엔티티의 모든 연관관계 필드가 즉시로딩 일 때는 위의 코드가 한방쿼리로 나간다.
        // 왜냐면 em.find(spring data JPA에서는 findById) 사용 시 @id키를 기준으로 질의한다.
        // em.find는 해당 엔티티의 연관관계 필드가 즉시로딩인지 지연로딩인지 미리 알고 @id key를 기준으로 질의하기 때문에 JPA 내부에서 한방쿼리 최적화가 가능하다.
        // 즉시 로딩인 엔티티 필드를 만날 경우 타고타고 들어가 쿼리 최적화를 한다.(즉 타고타고 가다가 해당 엔티티의 연관관계 필드가 지연로딩이면 그건 타지않음)


        // 하지만 즉시로딩이든 지연로딩이든 JPQL에서는 N + 1 발생한다.
        // 위를 주석처리하고 personList 필드부분을 EAGER 로 바꾼 후 아래 JPQL로 호출 해보자
        // JPQL에선 입력 받은 query string이 그대로 SQL로 변환되어 쿼리가 바로 나가므로 JPA 내부에서 쿼리 최적화를 하지 못해 N + 1 발생한다.
        // 쿼리를 날려서 가져와서 분석해보니 personList가 EAGER이네? 즉시로딩인 필드를 질의하는 형태가 반복되며 N + 1 발생(똑같이 1 + 1 질의)

        //SchoolClass class1 = em.createQuery("select c from SchoolClass c where name = '호랑이반'", SchoolClass.class).getSingleResult();

        // class1.getPersonList()는 이미 한번 호출해서 디비에 질의하였으므로 또 디비에 쿼리가 나가지는 않는다.
        for (Person person : class1.getPersonList()) {
            System.out.println(person.getMyClass().getName());
        }

        // 반을 조회하며 쿼리가 1회 나가고 연관관계 엔티티를 출력하는 과정에서 1회 쿼리가 더 나가 총 2회 쿼리가 나가게 됐다.
    }

    @Test
    public void 지연_로딩_즉시_로딩_섞일_때_N_PLUS_1_예제() {
        EntityManager em = emf.createEntityManager();
        // School의 SchoolClass 연관관계 필드는 즉시로딩, SchoolClass의 PersonList 필드는 지연로딩인 상태
        // 마찬가지로 JPQL에서는 지연로딩, 즉시로딩 섞여있든 어떻든 N + 1 발생
        // em.find 일 때는 마찬가지로 위 테스트케이스에서 설명한 사유로 인해 즉시(EAGER) 로딩인 엔티티필드는 JPA 내부에서 쿼리 최적화 발생

        //옵션1 em.find 사용
        School school = em.find(School.class, 1L); // SchoolClassList 연관관계 필드는 즉시로딩이므로 쿼리 최적화로 한방에 가져온다. 쿼리 1회 발생

        //옵션2 JPQL 사용 // 이 시점에 1회 + 즉시로딩 조회 1회해서 쿼리 2회 발생
        //School school = em.createQuery("select s from School s where name = '떡잎유치원'", School.class).getSingleResult();

        for(SchoolClass schoolClass : school.getClassList()) {
            System.out.println(schoolClass.getPersonList()); // personList 연관관계 필드는 지연로딩이므로 사용시점에 쿼리 추가 발생(2회)
        }
        // 즉 em.find 사용 시 쿼리 1+2해서 3회 발생
        // jpql 사용 시 쿼리 2+2해서 4회 발생
    }

    @Test
    public void 쿼리최적화_fetchJoinExample() {
        // em.find는 즉시로딩인 필드를 쿼리최적화해 한방 쿼리로 보낸다. 그럼 JPQL을 사용할 땐 한방쿼리를 어떻게 보내지?
        // 현업에서는 모든 필드는 지연로딩 설정하고 필요 시 JPQL로 fetch join를 사용해 필요한 부분만 조인해 가져온다.

        // fetch join는 연관관계 필드에 대해서 쿼리 최적화를 해 한번에 가져올 수 있게끔 한 구문으로 일반적인 sql join 과는 전혀 다른 다른의미이다.
        // 일반 조인에 fetch 만 붙여주면 된다.

        // 다대일 조회
        EntityManager em = emf.createEntityManager();

        String query = "select p from Person p " +
                "inner join fetch p.myClass c ";

        //fetch join 에 의해 쿼리 최적화로 한방쿼리가 나간다.
        List<Person> personList = em.createQuery(query, Person.class)
                .setFirstResult(0)
                .setMaxResults(5)
                .getResultList();

        for(Person person : personList) {
            System.out.println(person.getMyClass().getName());
        }
    }

    @Test
    public void collectionFetchJoinExample() {
        // 일대다 조회 시 사용, 반은 실제로 2개인데 가짜데이터 생성되어 사람 개수만큼 60개반 생성 확인, 해결 위해서는 컬렉션 조인 해야한다. (distinct)
        // JPQL Join 에서도 내부 조인, 외부 조인, 컬렉션 조인을 알아보았듯이 1 : N 의 관계인 컬렉션을 페치 조인한것을 컬렉션 페치 조인이라고 한다.(fetch join)
        EntityManager em = emf.createEntityManager();
        String query = "select distinct c from SchoolClass c " +
                "inner join fetch c.personList p " +
                "where p.name like :name ";

        // fetch join 으로 쿼리 최적화를 해서 한방쿼리가 나간다.
        List<SchoolClass> classList = em.createQuery(query, SchoolClass.class)
                .setParameter("name", "%반%")
                .getResultList(); // 사람 이름에 반 이 들어가면 조회 (모든 사람 이름에 반이 들어가므로 전체 조회)

        Assert.assertEquals(classList.size(), 2); // 반 2개 조회
        System.out.println("classListSize: " + classList.size());

        for(SchoolClass schoolClass : classList) {
            System.out.println(schoolClass.getPersonList().size());
        }
    }
    @Test
    public void collectionJoinPagingExample() {
        // JPA 로 개발을 할때, 컬렉션 조인을 한방쿼리에 조회하려고 Fetch Join 하였다.
        // 근데 이를 페이징 조회 하려하면 메모리에서 페이징 처리하는 문제가 있다.
        // 처음에 DB질의 시 페이징 하지 않고 데이터를 모두 불러온 후 메모리 상에서 페이징 하기 때문에 데이터가 엄청 많으면 Out Of Memory 문제가 발생할 수 있다.
        // 컬렉션 페치 조인 예제에 페이징 걸어서 워링 확인해보자
        // HHH000104: firstResult/maxResults specified with collection fetch; applying in memory! 라는 워링 창이 뜰 것이다.

        // 컬렉션 조인후 "페이징"하고자 할 때는 쿼리최적화를 위해 fetch join 을 하지말고 대신 @BatchSize 설정으로 N + 1 문제를 해결하는 방법으로 페이징한다.
        // @BatchSize 설정은 N + 1 문제를 해결하는 또다른 방법으로 IN절을 이용해 batchSize 만큼 묶어 보내어 쿼리를 보낸다.
        // @BatchSize 설정은 한방쿼리는 안되지만 쿼리 날리는 횟수가 크게 줄어든다.

        EntityManager em = emf.createEntityManager();
        String query = "select distinct c from SchoolClass c " +
                "inner join c.personList p " +
                "where p.name like :name ";

        // 일반적인 컬렉션 조인에 페이징을 한다.
        List<SchoolClass> classList = em.createQuery(query, SchoolClass.class)
                .setParameter("name", "%반%")
                .setFirstResult(0)
                .setMaxResults(2) // 반 2개만 조회
                .getResultList();

        Assert.assertEquals(classList.size(), 2); // 반 2개 조회
        System.out.println("classListSize: " + classList.size());

        for(SchoolClass schoolClass : classList) { // @BatchSize 설정을 안하면 지연로딩에 의해 호출시점에 쿼리가 나가 2번 발생
            System.out.println(schoolClass.getPersonList().size());
        }
        // 일반 컬렉션 조인에 페이징을 하면 쿼리가 1 + 2 해서 총 3번 나간다.
        // SchoolClass의 PersonList 필드 위에 @BatchSize(size = 100) 을 해보자
        // 해당 연관관계 필드는 최대 사이즈만큼 묶어 보내기 때문에 1 + 1해서 2버 나간다.
    }
}
