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
    private OutboundResponseBuilder response;

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


    //TODO: writer body using MessageBodyWriter
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

    //TODO: throw WebApplicationException with response,use ExceptionMapper build response
    @Test
    public void should_build_response_by_exception_mapper_if_null_response_from_web_application_exception() throws Exception {
        WebApplicationException exception = new WebApplicationException("error", (Response) null);
        when(router.dispatch(any(), eq(resourceContext))).thenThrow(exception);
        when(providers.getExceptionMapper(eq(WebApplicationException.class))).thenReturn(new ExceptionMapper<WebApplicationException>() {
            @Override
            public Response toResponse(WebApplicationException exception) {
                //抽取处理就是为了这里，能返回回来
                return response.build();
            }
        });
        HttpResponse<String> httpResponse = get("/test");
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), httpResponse.statusCode());
    }
    //TODO: throw other exception,use ExceptionMapper build response

    //TODO: 500 if MessageBodyWriter not found

    //TODO entity is null, ignore messageBodyWriter
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

        OutboundResponse build() {
            OutboundResponse response = mock(OutboundResponse.class);
            when(response.getStatus()).thenReturn(status.getStatusCode());
            when(response.getStatusInfo()).thenReturn(status);
            when(response.getHeaders()).thenReturn(headers);
            when(response.getGenericEntity()).thenReturn(entity);
            when(response.getAnnotations()).thenReturn(annotations);
            when(response.getMediaType()).thenReturn(mediaType);
            return response;
        }

    }

}
