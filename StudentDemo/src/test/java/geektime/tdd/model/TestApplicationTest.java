package geektime.tdd.model;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TestApplicationTest {

    private EntityManagerFactory factory;
    private EntityManager manager;

    private StudentRepository repository;
    private Student john;

    @BeforeEach
    void before() {
        factory = Persistence.createEntityManagerFactory("student");
        manager = factory.createEntityManager();
        repository = new StudentRepository(manager);
        manager.getTransaction().begin();
        john = repository.save(new Student("john", "smith", "john.smith@email.com"));
        manager.getTransaction().commit();
    }

    @AfterEach
    void after() {
        manager.clear();
        manager.close();
        factory.close();
    }

    @Test
    public void should_generate_id_for_saved_entity() throws Exception{

        assertNotEquals(0,john.getId());
    }

    @Test
    public void should_be_able_to_load_saved_student_by_id() throws Exception{
        Optional<Student> loaded = repository.findById(john.getId());
        assertTrue(loaded.isPresent());
        assertEquals(john.getFirstName(),loaded.get().getFirstName());
        assertEquals(john.getLastName(),loaded.get().getLastName());
        assertEquals(john.getEmail(),loaded.get().getEmail());
        assertEquals(john.getId(),loaded.get().getId());
    }

    @Test
    public void should_be_able_to_load_saved_student_by_email() throws Exception{
        Optional<Student> loaded = repository.findByEmail(john.getEmail());
        assertTrue(loaded.isPresent());
        assertEquals(john.getFirstName(),loaded.get().getFirstName());
        assertEquals(john.getLastName(),loaded.get().getLastName());
        assertEquals(john.getEmail(),loaded.get().getEmail());
        assertEquals(john.getId(),loaded.get().getId());
    }

    @Test
    public void should_return_404_if_no_student_found() throws Exception{
    }
}