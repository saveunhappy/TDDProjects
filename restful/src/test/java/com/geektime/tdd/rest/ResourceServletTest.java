package com.geektime.tdd.rest;

import jakarta.servlet.Servlet;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Providers;
import jakarta.ws.rs.ext.RuntimeDelegate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.http.HttpResponse;
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
    private OutboundResponseBuilder builder;

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
        builder = new OutboundResponseBuilder();

        //先是mock掉，其实和创建一个子类没啥区别
        RuntimeDelegate delegate = mock(RuntimeDelegate.class);
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
        builder.status(Response.Status.NOT_MODIFIED).returnFrom(router);
        // 这个get就是HttpRequest 发送的，然后得到HttpResponse
        HttpResponse<String> httpResponse = get("/test");
        assertEquals(Response.Status.NOT_MODIFIED.getStatusCode(), httpResponse.statusCode());

    }

    @Test
    public void should_use_http_headers_from_response() throws Exception {

        NewCookie sessionId = new NewCookie.Builder("SESSION_ID").value("session").build();
        NewCookie userId = new NewCookie.Builder("USER_ID").value("user").build();
        builder.headers("Set-Cookie", sessionId, userId).returnFrom(router);
        // 这个get就是HttpRequest 发送的，然后得到HttpResponse
        HttpResponse<String> httpResponse = get("/test");

        assertArrayEquals(new String[]{"SESSION_ID=session", "USER_ID=user"},
                httpResponse.headers().allValues("Set-Cookie").toArray(String[]::new));
    }


    //TODO: writer body using MessageBodyWriter
    @Test
    public void should_write_entity_to_http_response_using_message_body_writer() throws Exception {
        builder.entity(new GenericEntity<>("entity", String.class), new Annotation[0]).returnFrom(router);
        HttpResponse<String> httpResponse = get("/test");
        assertEquals("entity", httpResponse.body());
    }

    //TODO: throw WebApplicationException with response,use response
    @Test
    public void should_use_response_from_web_application_exception() {

    }

    //TODO: throw WebApplicationException with response,use ExceptionMapper build response
    //TODO: throw other exception,use ExceptionMapper build response

    //TODO: 500 if MessageBodyWriter not found

    private void response(Response.Status status, MultivaluedMap<String, Object> headers, GenericEntity<Object> entity, Annotation[] annotations, MediaType mediaType) {
        OutboundResponse response = mock(OutboundResponse.class);
        when(response.getStatus()).thenReturn(status.getStatusCode());
        when(response.getHeaders()).thenReturn(headers);
        when(response.getGenericEntity()).thenReturn(entity);
        when(response.getAnnotations()).thenReturn(annotations);
        when(response.getMediaType()).thenReturn(mediaType);
        when(router.dispatch(any(), eq(resourceContext))).thenReturn(response);
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

        void build(Consumer<OutboundResponse> consumer) {
            OutboundResponse response = mock(OutboundResponse.class);
            when(response.getStatus()).thenReturn(status.getStatusCode());
            when(response.getHeaders()).thenReturn(headers);
            when(response.getGenericEntity()).thenReturn(entity);
            when(response.getAnnotations()).thenReturn(annotations);
            when(response.getMediaType()).thenReturn(mediaType);
            consumer.accept(response);
//            when(router.dispatch(any(), eq(resourceContext))).thenReturn(response);

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
