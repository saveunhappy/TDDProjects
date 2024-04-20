package com.geektime.tdd.rest;

import jakarta.servlet.Servlet;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Providers;
import jakarta.ws.rs.ext.RuntimeDelegate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.function.Executable;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResourceServletTest extends ServletTest {
    //这个就是相当于一个全局的仓库
    private Runtime runtime;
    //这个就是每次请求过来，去路由
    private ResourceRouter router;
    //@Path，@Controller
    private ResourceContext resourceContext;
    private Providers providers;
    private OutboundResponseBuilder response;

    private RuntimeDelegate delegate;

    @Override
    protected Servlet getServlet() {
        runtime = mock(Runtime.class);
        router = mock(ResourceRouter.class);
        resourceContext = mock(ResourceContext.class);
        providers = mock(Providers.class);
        when(runtime.getResourceRouter()).thenReturn(router);
        //根据Request和Response去获取对应的@Path对象，我们现在不关心是啥，因为请求和响应还没进来
        when(runtime.createResourceContext(any(), any())).thenReturn(resourceContext);
        when(runtime.getProviders()).thenReturn(providers);
        return new ResourceServlet(runtime);
    }

    @BeforeEach
    public void before() {
        response = new OutboundResponseBuilder();

        //先是mock掉，其实和创建一个子类没啥区别
        delegate = mock(RuntimeDelegate.class);
        //然后设置全局的
        RuntimeDelegate.setInstance(delegate);
        //这个时候就是使用mock的对象，设置他对应的行为。
        when(delegate.createHeaderDelegate(NewCookie.class)).thenReturn(new RuntimeDelegate.HeaderDelegate<>() {
            //这个相当于set，如果这个value是个json的字符串呢，你是可以设置转换成对象的，不再是之前的只能是String
            @Override
            public NewCookie fromString(String value) {
                return null;
            }

            //这个就是get
            @Override
            public String toString(NewCookie value) {
                return value.getName() + "=" + value.getValue();
            }
        });


    }

    @Test
    public void should_use_status_from_response() throws Exception {
        response.status(Response.Status.NOT_MODIFIED).returnFrom(router);
        // 这个get就是HttpRequest 发送的，然后得到HttpResponse
        HttpResponse<String> httpResponse = get("/test");
        assertEquals(Response.Status.NOT_MODIFIED.getStatusCode(), httpResponse.statusCode());

    }

    @Test
    public void should_use_http_headers_from_response() throws Exception {

        NewCookie sessionId = new NewCookie.Builder("SESSION_ID").value("session").build();
        NewCookie userId = new NewCookie.Builder("USER_ID").value("user").build();
        response.headers("Set-Cookie", sessionId, userId).returnFrom(router);
        // 这个get就是HttpRequest 发送的，然后得到HttpResponse
        HttpResponse<String> httpResponse = get("/test");

        assertArrayEquals(new String[]{"SESSION_ID=session", "USER_ID=user"},
                httpResponse.headers().allValues("Set-Cookie").toArray(String[]::new));
    }


    @Test
    public void should_write_entity_to_http_response_using_message_body_writer() throws Exception {
        response.entity(new GenericEntity<>("entity", String.class), new Annotation[0]).returnFrom(router);
        HttpResponse<String> httpResponse = get("/test");
        assertEquals("entity", httpResponse.body());
    }

    @Test
    public void should_use_response_from_web_application_exception() throws Exception {
        response.status(Response.Status.FORBIDDEN)
                .headers(HttpHeaders.SET_COOKIE, new NewCookie.Builder("SESSION_ID").value("sessionId").build())
                .entity(new GenericEntity<>("error", String.class), new Annotation[0])
                .throwFrom(router);
        HttpResponse<String> httpResponse = get("/test");
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), httpResponse.statusCode());
        assertArrayEquals(new String[]{"SESSION_ID=sessionId"}, httpResponse.headers().allValues(HttpHeaders.SET_COOKIE).toArray(String[]::new));
        assertEquals("error", httpResponse.body());
    }

    @Test
    public void should_build_response_by_exception_mapper_if_null_response_from_web_application_exception() throws Exception {

        when(router.dispatch(any(), eq(resourceContext))).thenThrow(RuntimeException.class);
        when(providers.getExceptionMapper(eq(RuntimeException.class))).thenReturn(exception -> {
            //抽取处理就是为了这里，能返回回来
            return response.status(Response.Status.FORBIDDEN).build();
        });

        HttpResponse<String> httpResponse = get("/test");
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), httpResponse.statusCode());
    }

    @Test
    public void should_not_call_message_body_writer_if_entity_is_null() throws Exception {
        response.entity(null, new Annotation[0]).returnFrom(router);

        HttpResponse<String> httpResponse = get("/test");

        assertEquals(Response.Status.OK.getStatusCode(), httpResponse.statusCode());
        assertEquals("", httpResponse.body());
    }

    //TODO: 500 if MessageBodyWriter not found
    //TODO: 500 if header delegate
    //TODO: 500 if exception mapper
    //TODO: exception mapper
    @Test
    public void should_use_response_from_web_application_exception_thrown_by_exception_mapper() throws Exception {

        when(router.dispatch(any(), eq(resourceContext))).thenThrow(RuntimeException.class);
        when(providers.getExceptionMapper(eq(RuntimeException.class))).thenReturn(exception -> {
            //抽取处理就是为了这里，能返回回来
            throw new WebApplicationException(response.status(Response.Status.FORBIDDEN).build());
        });

        HttpResponse<String> httpResponse = get("/test");
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), httpResponse.statusCode());
    }

    @Test
    public void should_map_exception_thrown_by_exception_mapper() throws Exception {

        when(router.dispatch(any(), eq(resourceContext))).thenThrow(RuntimeException.class);
        //这不是重复，这个是如果catch的Throwable throwable的实际类型是RuntimeException，那么就抛出
        //IllegalArgumentException，
        when(providers.getExceptionMapper(eq(RuntimeException.class))).thenReturn(exception -> {
            throw new IllegalArgumentException();
        });
        //这个是如果如果catch的Throwable throwable的实际类型是IllegalArgumentException,那么设置一下状态码，403
        //因为IllegalArgumentException和WebApplicationException不是一个，所以是可以的
        when(providers.getExceptionMapper(eq(IllegalArgumentException.class))).thenReturn(exception ->
                response.status(Response.Status.FORBIDDEN).build()
        );
        HttpResponse<String> httpResponse = get("/test");
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), httpResponse.statusCode());
    }

    @Test
    public void should_map_exception_thrown_by_header_delegate_when_create_header_delegate() {
        RuntimeException exception = new IllegalArgumentException();

        response().headers(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_TYPE).returnFrom(router);
        when(delegate.createHeaderDelegate(eq(MediaType.class))).thenReturn(new RuntimeDelegate.HeaderDelegate<MediaType>() {
            @Override
            public MediaType fromString(String value) {
                return null;
            }

            @Override
            public String toString(MediaType value) {
                throw exception;
            }
        });
        when(providers.getExceptionMapper(eq(IllegalArgumentException.class))).thenReturn(e ->
                response().status(Response.Status.FORBIDDEN).build()
        );
        HttpResponse<String> httpResponse = get("/test");
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), httpResponse.statusCode());

    }

    @TestFactory
    public List<DynamicTest> should_respond_based_on_exception_thrown() {
        List<DynamicTest> tests = new ArrayList<>();
        //这个本身就是Consumer的，所以又包装了一层Consumer
        Map<String, Consumer<Consumer<RuntimeException>>> exceptions = Map.of("Other Exception",
                this::otherExceptionThrownFrom, "WebApplication ExceptionThrown",
                this::webApplicationExceptionThrownFrom);

        Map<String, Consumer<RuntimeException>> callers = getCallers();

        //callers就是要stub的messageBodyWriter返回的异常，这个是otherExceptionThrownFrom中的一部分， 所以是作为
        //Consumer传过去，就是不同的异常
        for (Map.Entry<String, Consumer<RuntimeException>> caller : callers.entrySet()) {
            //那这里就是去执行不同的异常了，这个Consumer<Consumer<RuntimeException>>中的泛型就是Consumer<RuntimeException>
            //也就是stub的那两个MessageBodyWriter
            for (Map.Entry<String, Consumer<Consumer<RuntimeException>>> exceptionThrownFrom : exceptions.entrySet()) {
                tests.add(DynamicTest.dynamicTest(caller.getKey() + "throws" + exceptionThrownFrom.getKey(), () -> {
                    //这个exceptionThrownFrom就是接受Consumer接口，但是没有调用caller.accept(exception)啊
                    //其实是传过去之后，这个caller在exceptionThrownFrom这个内部自己调用了，所以这里不用调用
                    //然后这个caller执行的就是把exception给stub一下，那么这个exception是从哪传入的？其实是在
                    //exceptionThrownFrom这个内部自己创建的，传给了caller，然后caller自己调用了accept
                    exceptionThrownFrom.getValue().accept(caller.getValue());
                }));
            }
        }
        return tests;
    }


    @Retention(RetentionPolicy.RUNTIME)
    @interface ExceptionThrownFrom {

    }

    private void webApplicationExceptionThrownFrom(Consumer<RuntimeException> caller) {
        RuntimeException exception = new WebApplicationException(response()
                .status(Response.Status.FORBIDDEN).build());
        caller.accept(exception);

        HttpResponse<String> httpResponse = get("/test");
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), httpResponse.statusCode());
    }

    private void otherExceptionThrownFrom(Consumer<RuntimeException> caller) {
        RuntimeException exception = new IllegalArgumentException();
        caller.accept(exception);
        when(providers.getExceptionMapper(eq(IllegalArgumentException.class))).thenReturn(e ->
                response().status(Response.Status.FORBIDDEN).build()
        );
        HttpResponse<String> httpResponse = get("/test");
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), httpResponse.statusCode());
    }

    private Map<String, Consumer<RuntimeException>> getCallers() {
        Map<String, Consumer<RuntimeException>> callers = new HashMap<>();
        for (Method method : Arrays.stream(this.getClass().getDeclaredMethods()).
                filter(m -> m.isAnnotationPresent(ExceptionThrownFrom.class)).toList()) {
            String name = method.getName();
            String callerName = name.substring(0, 1).toUpperCase() + name.substring(1).replace('_', '.');
            callers.put(callerName, e -> {
                try {
                    /*
                    *     private void otherExceptionThrownFrom(Consumer<RuntimeException> caller) {
                                RuntimeException exception = new IllegalArgumentException();
                                caller.accept(exception);
                          这个exception是哪来的呢？不用管，你这个就是定义了方法，我接受一个RuntimeException，
                          你调用这个方法的时候，你把Exception传过来就能执行，所以，这个就是函数式结构
                    * */
                    method.invoke(ResourceServletTest.this, e);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });
        }

        return callers;
    }

    @ExceptionThrownFrom
    private void providers_getMessageBodyWriter(RuntimeException exception) {
        response.entity(new GenericEntity<>(2.5, Double.class), new Annotation[0]).returnFrom(router);
        when(providers.getMessageBodyWriter(eq(Double.class), eq(Double.class),
                eq(new Annotation[0]), eq(MediaType.TEXT_PLAIN_TYPE))).thenThrow(exception);
    }

    @ExceptionThrownFrom
    private void messageBodyWriter_writeTo(RuntimeException exception) {
        response().entity(new GenericEntity<>(2.5, Double.class), new Annotation[0]).returnFrom(router);
        when(providers.getMessageBodyWriter(eq(Double.class), eq(Double.class), eq(new Annotation[0]), eq(MediaType.TEXT_PLAIN_TYPE)))
                .thenReturn(new MessageBodyWriter<>() {
                    @Override
                    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
                        return false;
                    }

                    @Override
                    public void writeTo(Double aDouble, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws WebApplicationException {
                        throw exception;
                    }
                });
    }

    private OutboundResponseBuilder response() {
        return new OutboundResponseBuilder();
    }

    class OutboundResponseBuilder {
        Response.Status status = Response.Status.OK;
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        GenericEntity<Object> entity = new GenericEntity<>("entity", String.class);
        Annotation[] annotations = new Annotation[0];
        MediaType mediaType = MediaType.TEXT_PLAIN_TYPE;

        public OutboundResponseBuilder status(Response.Status status) {
            this.status = status;
            return this;
        }

        public OutboundResponseBuilder headers(String name, Object... values) {
            headers.addAll(name, values);
            return this;
        }

        public OutboundResponseBuilder entity(GenericEntity<Object> entity, Annotation[] annotations) {
            this.entity = entity;
            this.annotations = annotations;
            return this;
        }

        void returnFrom(ResourceRouter router) {
            build(response -> when(router.dispatch(any(), eq(resourceContext))).thenReturn(response));
        }

        void throwFrom(ResourceRouter router) {
            build(response -> {
                //WebApplicationException创建对象的时候会调用构造器，构造器中会调用computeExceptionMessage这个方法
                //这个方法如下：
                /*
                 *    private static String computeExceptionMessage(final Response response) {
                        final Response.StatusType statusInfo;
                        if (response != null) {
                         statusInfo = response.getStatusInfo();
                        } else {
                         statusInfo = Response.Status.INTERNAL_SERVER_ERROR;
                    }
                     return "HTTP " + statusInfo.getStatusCode() + ' ' + statusInfo.getReasonPhrase();
                    }
                 *
                 * */
                //statusInfo.getStatusCode()是空
                WebApplicationException exception = new WebApplicationException(response);
                when(router.dispatch(any(), eq(resourceContext))).thenThrow(exception);
            });
        }

        void build(Consumer<OutboundResponse> consumer) {
            OutboundResponse response = build();
            consumer.accept(response);
//            when(router.dispatch(any(), eq(resourceContext))).thenReturn(response);


        }

        OutboundResponse build() {
            OutboundResponse response = mock(OutboundResponse.class);
            when(response.getStatus()).thenReturn(status.getStatusCode());
            when(response.getStatusInfo()).thenReturn(status);
            when(response.getHeaders()).thenReturn(headers);
            when(response.getGenericEntity()).thenReturn(entity);
            when(response.getAnnotations()).thenReturn(annotations);
            when(response.getMediaType()).thenReturn(mediaType);
            stubMessageBodyWriter();
            return response;
        }

        private void stubMessageBodyWriter() {
            when(providers.getMessageBodyWriter(eq(String.class), eq(String.class), same(annotations), eq(mediaType))).thenReturn(
                    new MessageBodyWriter<>() {
                        @Override
                        public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
                            return false;
                        }

                        @Override
                        public void writeTo(String s, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
                            PrintWriter writer = new PrintWriter(entityStream);
                            writer.write(s);
                            writer.flush();
                        }
                    });
        }

    }

}
