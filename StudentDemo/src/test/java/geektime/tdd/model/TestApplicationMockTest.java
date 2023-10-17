package geektime.tdd.model;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TestApplicationMockTest {
    private EntityManager manager;
    private StudentRepository repository;
    CriteriaBuilder builder;
    private Student john = new Student("john", "smith", "john.smith@email.com");

    @BeforeEach
    void before() {
        manager = Mockito.mock(EntityManager.class);
        builder = manager.getCriteriaBuilder();
        repository = new StudentRepository(manager);

    }

    @AfterEach
    void after() {
        manager.clear();
        manager.close();
    }

    @Test
    public void should_generate_id_for_saved_entity() throws Exception{
        repository.save(john);
        verify(manager).persist(john);
    }

    @Test
    public void should_be_able_to_load_saved_student_by_id() throws Exception{
        when(manager.find(any(),any())).thenReturn(john);
        assertEquals(john,repository.findById(1).get());

        verify(manager).find(Student.class,1L);
    }

    @Test
    public void should_be_able_to_load_saved_student_by_email() throws Exception{
        TypedQuery query = mock(TypedQuery.class);
        when(manager.createQuery(any(),any())).thenReturn(query);
        when(manager.getCriteriaBuilder()).thenReturn(builder);
        when(query.setParameter(any(String.class),any())).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList(john));
        assertEquals(john,repository.findByEmail("john.smith@email.com").get());

        verify(manager).createQuery("SELECT s from Student s where s.email = :email", Student.class);
        //这个对应的是repository.findByEmail("john.smith@email.com").get()，在调用findByEmail的时候把值给设置进去了
        verify(query).setParameter("email","john.smith@email.com");

    }


}