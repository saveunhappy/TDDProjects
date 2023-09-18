package geektime.tdd.resources;

import geektime.tdd.model.Student;
import geektime.tdd.model.StudentRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/students")
public class StudentsResource {
    private StudentRepository repository;

    @Inject
    public StudentsResource(StudentRepository repository) {
        this.repository = repository;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Student> all() {
        return repository.all();
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response findById(@PathParam("id") long id) {
        return repository.findById(id).map(Response::ok)
                .orElse(Response.status(Response.Status.NOT_FOUND)).build();
    }

}
