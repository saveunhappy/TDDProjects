package com.geektime.tdd.rest;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ASpike {
    Server server;

    @BeforeEach
    public void start() throws Exception {

        server = new Server(8080);
        Connector connector = new ServerConnector(server);
        server.addConnector(connector);

        ServletContextHandler handler = new ServletContextHandler(server, "/");
        handler.addServlet(new ServletHolder(new ResourceServlet(new TestApplication())), "/");
        server.setHandler(handler);
        server.start();
    }

    @AfterEach
    public void stop() throws Exception {
        server.stop();
    }

    @Test
    public void should() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(new URI("http://localhost:8080/")).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.body());
        assertEquals("test", response.body());
    }

    static class ResourceServlet extends HttpServlet {
        private Application application;

        public ResourceServlet(Application application) {
            this.application = application;
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            Stream<Class<?>> classStream = application.getClasses().stream().filter(c -> c.isAnnotationPresent(Path.class));
            Object result = dispatch(req, classStream);
//            String result = new TestResource().get();
            resp.getWriter().write(result.toString());
            resp.getWriter().flush();
        }

        Object dispatch(HttpServletRequest req, Stream<Class<?>> classStream) {
            try {
                //获取到刚才的Controller，就是TestResource
                Class<?> rootClass = classStream.findFirst().get();
                //创建对象
                Object rootResource = rootClass.getConstructor().newInstance();
                //找到要执行的方法，就是@GetMapping
                Method method = Arrays.stream(rootClass.getMethods()).filter(m -> m.isAnnotationPresent(GET.class)).findFirst().get();
                //执行方法，那有返回值，就是返回的String
                return method.invoke(rootResource);
            }  catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class TestApplication extends Application {
        @Override
        public Set<Class<?>> getClasses() {
            return Set.of(TestResource.class);
        }
    }

    @Path("/test")
    static class TestResource {
        public TestResource() {
        }

        @GET
        public String get() {
            return "test";
        }
    }

}
