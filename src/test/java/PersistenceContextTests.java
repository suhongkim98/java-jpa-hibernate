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

/**
 * 엔티티 상태 detach, remove 예시
 * persist, merge 예시
 */
public class PersistenceContextTests {

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
    public void 엔티티_상태_REMOVE() {
        //리무브된 엔티티는 엔티티 상태가 삭제 상태임, 조회는 가능하다.
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
        Assert.assertNotNull(person);

        em.remove(person); // 삭제
        Assert.assertNotNull(person); // null은 아님
        System.out.println(person.getName()); // 삭제 상태인 엔티티지만 조회 가능

        // delete 쿼리가 나감
        tx.commit();
    }

    /**
     * em.remove로 삭제 시도
     * SchoolClass는 삭제하고자한 Person 객체를 참조하고 영속성 컨텍스트에 의해 관리 중에 있으므로 삭제가 안 됨
     */
    @Test
    public void remove_동작_안_하는_상항() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        tx.begin();

        SchoolClass schoolClass1 = em.find(SchoolClass.class, 1L);
        Assert.assertNotNull(schoolClass1); // 조회 성공, 영속성 컨텍스트에 의해 관리 중

        // 반에서 사람 한 명 임의로 꺼내기
        Person person1 = em.find(Person.class, schoolClass1.getPersonList().get(0).getId());
        Assert.assertEquals(person1, schoolClass1.getPersonList().get(0)); // 같은 컨텍스트므로 주소가 동일함

        Assert.assertEquals(30, schoolClass1.getPersonList().size()); // 현재 반에서는 30명
        em.remove(person1); // 임의로 꺼낸 person에 대해 remove 시도, schoolClass에서는 참조 중인 person임
        //schoolClass1.removePerson(person1); // em.remove 대신 다음과 같이하여 연관관계를 끊고 orphan removal을 이용하면 삭제됨
        Assert.assertEquals(30, schoolClass1.getPersonList().size()); // schoolClass에서는 여전히 30명

        Assert.assertNotNull(person1);
        Assert.assertNotNull(schoolClass1.getPersonList().get(0));

        // delete 쿼리는 나가지 않았음
        tx.commit();
    }
}
