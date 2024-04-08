package com.geektime.tdd.rest;

import com.geektime.tdd.ComponentRef;
import com.geektime.tdd.Context;
import com.geektime.tdd.ContextConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.*;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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
        //刚开始这个TestApplication中是存储Controller，使用java自带的流去输出，现在要增加使用哪种方式去写出去
        //TestProviders里面有各种可以提供的，context的，messageBodyWriter的，还有Exception的，但是这些对象
        //放在哪呢?目前是放在TestApplication里面，所以new TestProviders(application)需要把application传送进去
        TestApplication application = new TestApplication();
        handler.addServlet(new ServletHolder(new ResourceServlet(application, new TestProviders(application))), "/");
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

        private Providers providers;

        public ResourceServlet(Application application, Providers providers) {
            this.application = application;
            this.providers = providers;
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            Stream<Class<?>> classStream = application.getClasses().stream().filter(c -> c.isAnnotationPresent(Path.class));
            Object result = dispatch(req, classStream);
//            String result = new TestResource().get();
            //这个providers里面已经通过application取出来所有的符合MessageBodyWriter，目前就只有一个
            // StringMessageBodyWriter，然后getMessageBodyReader是获取，现在其实也就是获取到只有的那一个
            //第一个参数就是要写的对象
            MessageBodyWriter<Object> writer = (MessageBodyWriter<Object>) providers.getMessageBodyWriter(result.getClass(), null, null, null);
            writer.writeTo(result, null, null, null, null, null, resp.getOutputStream());

//            resp.getWriter().write(result.toString());
//            resp.getWriter().flush();
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
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class TestApplication extends Application {
        @Override
        public Set<Class<?>> getClasses() {
            return Set.of(TestResource.class, StringMessageBodyWriter.class);
        }
    }

    static class TestProviders implements Providers {
        private List<MessageBodyWriter> writers;
        private Application application;

        public TestProviders(Application application) {
            this.application = application;

            ContextConfig config = new ContextConfig();

            //表示判断类是否是 MessageBodyWriter 接口的子类或实现类,如果是，则保留在流中。
            //注意，这里是要强转的，比如规定泛型是MessageBodyWriter，否则泛型就是?没有泛型，下面就调用不了w.isWriteable方法
            List<Class<?>> writerClasses = this.application.getClasses().stream().filter(MessageBodyWriter.class::isAssignableFrom).toList();
            for (Class writerClass : writerClasses) {
                //component是newinstance,instance就是绑定实例，from就是要有Qualifier了
                config.component(writerClass,writerClass);
            }
            Context context = config.getContext();

            writers = (List<MessageBodyWriter>) writerClasses.stream().map(c -> context.get(ComponentRef.of(c)).get()).collect(Collectors.toList());

        }

        @Override
        public <T> MessageBodyReader<T> getMessageBodyReader(Class<T> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return null;
        }

        @Override
        public <T> MessageBodyWriter<T> getMessageBodyWriter(Class<T> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            //就找到一个就行了，我们目前也是先找到一个
            return writers.stream().filter(w -> w.isWriteable(type, genericType, annotations, mediaType)).findFirst().get();
        }

        @Override
        public <T extends Throwable> ExceptionMapper<T> getExceptionMapper(Class<T> type) {
            return null;
        }

        @Override
        public <T> ContextResolver<T> getContextResolver(Class<T> contextType, MediaType mediaType) {
            return null;
        }
    }

    static class StringMessageBodyWriter implements MessageBodyWriter<String> {
        public StringMessageBodyWriter() {
        }

        @Override
        public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return type == String.class;
        }

        @Override
        public void writeTo(String s, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
            //entityStream: 用于写入消息体的输出流。
            PrintWriter writer = new PrintWriter(entityStream);
            //s: 要写入响应消息体的对象实例。
            writer.write(s);
            writer.flush();
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
