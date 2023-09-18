package geektime.tdd.model;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import static org.junit.jupiter.api.Assertions.*;

class TestApplicationTest {

    private EntityManagerFactory factory;
    private EntityManager manager;

    private StudentRepository repository;

    @BeforeEach
    void before() {
        factory = Persistence.createEntityManagerFactory("student");
        manager = factory.createEntityManager();
        repository = new StudentRepository(manager);
    }

    @AfterEach
    void after() {
        manager.clear();
        manager.close();
        factory.close();
    }

    @Test
    public void should_generate_id_for_saved_entity() throws Exception{
        manager.getTransaction().begin();
        Student john = repository.save(new Student("john", "smith", "john.smith@email.com"));
        manager.getTransaction().commit();
        assertNotEquals(0,john.getId());
    }
}