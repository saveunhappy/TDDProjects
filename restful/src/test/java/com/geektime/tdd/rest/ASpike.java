package com.geektime.tdd.rest;

import com.geektime.tdd.ComponentRef;
import com.geektime.tdd.Config;
import com.geektime.tdd.Context;
import com.geektime.tdd.ContextConfig;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.*;
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
import java.util.*;
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
        assertEquals("prefixprefixtest", response.body());
    }

    static class ResourceServlet extends HttpServlet {
        private final Context context;
        private TestApplication application;

        private Providers providers;

        public ResourceServlet(TestApplication application, Providers providers) {
            this.application = application;
            this.providers = providers;
            context = application.getContext();

        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            //目前Application里面存储了所有我们目前需要的Class，然后我们要去根据Class去创建对象，放到容器里面去。
            Stream<Class<?>> classStream = application.getClasses().stream().filter(c -> c.isAnnotationPresent(Path.class));
            ResourceContext rc = application.createResourceContext(req, resp);
            OutboundResponse result = dispatch(req, classStream, rc);
            GenericEntity entity = result.getGenericEntity();
//            String result = new TestResource().get();
            //这个providers里面已经通过application取出来所有的符合MessageBodyWriter，目前就只有一个
            // StringMessageBodyWriter，然后getMessageBodyReader是获取，现在其实也就是获取到只有的那一个
            //第一个参数就是要写的对象
            MessageBodyWriter<Object> writer = (MessageBodyWriter<Object>) providers.getMessageBodyWriter(entity.getRawType(), entity.getType(), result.getAnnotations(), result.getMediaType());
            writer.writeTo(result, entity.getRawType(), entity.getType(), result.getAnnotations(), result.getMediaType(), result.getHeaders(), resp.getOutputStream());
//            resp.getWriter().write(result.toString());
//            resp.getWriter().flush();
        }

        OutboundResponse dispatch(HttpServletRequest req, Stream<Class<?>> classStream, ResourceContext rc) {
            try {
                //获取到刚才的Controller，就是TestResource
                Class<?> rootClass = classStream.findFirst().get();
                //每次请求进来就把这个Controller给获取到，因为之前已经bind了啊，所以这里拿到，注册到
                //这个ResourceContext中去，每次初始化一个，其他地方也就能拿到
                Object rootResource = rc.initResource(context.get(ComponentRef.of(rootClass)).get());
//                Object rootResource = rootClass.getConstructor().newInstance();
                //找到要执行的方法，就是@GetMapping
                Method method = Arrays.stream(rootClass.getMethods()).filter(m -> m.isAnnotationPresent(GET.class)).findFirst().get();
                //执行方法，那有返回值，就是返回的String
                Object result = method.invoke(rootResource);
                //Response- code,header,media type,body
                //pojo,void,GenericType
                GenericEntity entity = new GenericEntity(result, method.getGenericReturnType());

                //如果是Response，直接返回，如果是那三种情况，那么封装成Response返回
                return new OutboundResponse() {
                    @Override
                    GenericEntity getGenericEntity() {
                        return entity;
                    }

                    @Override
                    Annotation[] getAnnotations() {
                        return new Annotation[0];
                    }

                    @Override
                    public int getStatus() {
                        return 0;
                    }

                    @Override
                    public StatusType getStatusInfo() {
                        return null;
                    }

                    @Override
                    public Object getEntity() {
                        return entity;
                    }

                    @Override
                    public <T> T readEntity(Class<T> entityType) {
                        return null;
                    }

                    @Override
                    public <T> T readEntity(GenericType<T> entityType) {
                        return null;
                    }

                    @Override
                    public <T> T readEntity(Class<T> entityType, Annotation[] annotations) {
                        return null;
                    }

                    @Override
                    public <T> T readEntity(GenericType<T> entityType, Annotation[] annotations) {
                        return null;
                    }

                    @Override
                    public boolean hasEntity() {
                        return false;
                    }

                    @Override
                    public boolean bufferEntity() {
                        return false;
                    }

                    @Override
                    public void close() {

                    }

                    @Override
                    public MediaType getMediaType() {
                        return null;
                    }

                    @Override
                    public Locale getLanguage() {
                        return null;
                    }

                    @Override
                    public int getLength() {
                        return 0;
                    }

                    @Override
                    public Set<String> getAllowedMethods() {
                        return null;
                    }

                    @Override
                    public Map<String, NewCookie> getCookies() {
                        return null;
                    }

                    @Override
                    public EntityTag getEntityTag() {
                        return null;
                    }

                    @Override
                    public Date getDate() {
                        return null;
                    }

                    @Override
                    public Date getLastModified() {
                        return null;
                    }

                    @Override
                    public URI getLocation() {
                        return null;
                    }

                    @Override
                    public Set<Link> getLinks() {
                        return null;
                    }

                    @Override
                    public boolean hasLink(String relation) {
                        return false;
                    }

                    @Override
                    public Link getLink(String relation) {
                        return null;
                    }

                    @Override
                    public Link.Builder getLinkBuilder(String relation) {
                        return null;
                    }

                    @Override
                    public MultivaluedMap<String, Object> getMetadata() {
                        return null;
                    }

                    @Override
                    public MultivaluedMap<String, String> getStringHeaders() {
                        return null;
                    }

                    @Override
                    public String getHeaderString(String name) {
                        return null;
                    }
                };
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class TestApplication extends Application {

        private final Context context;

        @Override
        public Set<Class<?>> getClasses() {
            return Set.of(TestResource.class, StringMessageBodyWriter.class);
        }

        public Config getConfig() {
            return new Config() {
                @Named("prefix")
                public String name = "prefix";
            };
        }

        public Context getContext() {
            return context;
        }

        public TestApplication() {
            ContextConfig config = new ContextConfig();
            config.from(getConfig());
            List<Class<?>> writerClasses = this.getClasses().stream().filter(MessageBodyWriter.class::isAssignableFrom).toList();
            for (Class writerClass : writerClasses) {
                //component是newInstance,instance就是绑定实例，from就是要有Qualifier了
                config.component(writerClass, writerClass);
            }
            List<Class<?>> rootResources = this.getClasses().stream().filter(c -> c.isAnnotationPresent(Path.class)).toList();
            for (Class rootResource : rootResources) {
                config.component(rootResource, rootResource);
            }
            context = config.getContext();
        }

        //因为你每次一个请求都是新的，所以每次dispatch的时候都要创建一个新的ResourceContext
        public ResourceContext createResourceContext(HttpServletRequest request, HttpServletResponse response) {
            return new ResourceContext() {
                @Override
                public <T> T getResource(Class<T> resourceClass) {
                    return null;
                }

                //先返回自己，测试是通过的，这里就相当于传了个东西，又返回回来了，就是相当于啥都没干，测试能通过
                @Override
                public <T> T initResource(T resource) {
                    return resource;
                }
            };
        }
    }

    static class TestProviders implements Providers {
        private List<MessageBodyWriter> writers;
        private TestApplication application;

        public TestProviders(TestApplication application) {
            this.application = application;
            //表示判断类是否是 MessageBodyWriter 接口的子类或实现类,如果是，则保留在流中。
            //注意，这里是要强转的，比如规定泛型是MessageBodyWriter，否则泛型就是?没有泛型，下面就调用不了w.isWriteable方法
            List<Class<?>> writerClasses = this.application.getClasses().stream().filter(MessageBodyWriter.class::isAssignableFrom).toList();
            Context context = application.getContext();
            //bind了，然后要获取，获取的时候肯定也是context.get(xxx.class),然后这里要的是list，那就通过Stream的方式去
            //获取到bind到容器中的对象，这个writers就是在getMessageBodyWriter里面有用
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
        @Inject
        @Named("prefix")
        String prefix;

        public StringMessageBodyWriter() {
        }

        @Override
        public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return type == String.class;
        }

        @Override//List<Person>,List<Order>，得知道选哪个MessageBodyWriter,肯定不能选一个
        public void writeTo(String s, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
            //entityStream: 用于写入消息体的输出流。
            PrintWriter writer = new PrintWriter(entityStream);
            //s: 要写入响应消息体的对象实例。
            writer.write(prefix);
            writer.write(s);
            writer.flush();
        }
    }

    @Path("/test")
    static class TestResource {
        @Inject
        @Named("prefix")
        String prefix;

        public TestResource() {
        }

        //现在我们是通过反射创建对象，然后调用这个方法，返回，但是最终要写回到Response中去的，所以在Response中我们还是需要
        //有其他的东西的，status code,media type,headers(content-type,...),body
        @GET
        @Produces(MediaType.TEXT_PLAIN)//这个就是返回文本信息
        public String get() {
            return prefix + "test";
        }

        @GET
        @Path("/with-headers")
        public Response withHeaders() {
            return Response.ok().header("Set-Cookie", new NewCookie.Builder("SESSION_ID").value("SID").build())
                    //这个Annotation就是public void writeTo(String s, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
                    //这个方法，就是你要write的时候传过去的
                    .entity("string", new Annotation[0])
                    .build();
        }

        @GET
        @Path("/generic")
        public GenericEntity<List<String>> generic() {
            //这个就是要提取泛型类型，之前的ParameterizedType里面有学过，这个GenericEntity是protected,
            //没法直接new，还是得写成这种形式
            return new GenericEntity<>(List.of("abc", "def")) {

            };
        }


        @GET
        @Path("/pojo-generic")
        public List<String> pojoGeneric() {
            //这个就是要提取泛型类型，之前的ParameterizedType里面有学过，这个GenericEntity是protected,
            //没法直接new，还是得写成这种形式
            return List.of("abc", "def");
        }

        @PUT//204
        public void update() {

        }
    }

}
