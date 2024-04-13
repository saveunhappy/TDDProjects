package com.geektime.tdd.rest;

import jakarta.servlet.Servlet;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.Providers;
import jakarta.ws.rs.ext.RuntimeDelegate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
        response(Response.Status.NOT_MODIFIED, new MultivaluedHashMap<>(), new GenericEntity<>("entity", String.class), new Annotation[0], MediaType.TEXT_PLAIN_TYPE);
        // 这个get就是HttpRequest 发送的，然后得到HttpResponse
        HttpResponse<String> httpResponse = get("/test");
        assertEquals(Response.Status.NOT_MODIFIED.getStatusCode(), httpResponse.statusCode());

    }

    @Test
    public void should_use_http_headers_from_response() throws Exception {


        NewCookie sessionId = new NewCookie.Builder("SESSION_ID").value("session").build();
        NewCookie userId = new NewCookie.Builder("USER_ID").value("user").build();
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        // key是Set-Cookie，value是一个list,就是SESSION_ID和USER_ID

        headers.addAll("Set-Cookie", sessionId, userId);
        Response.Status status = Response.Status.NOT_MODIFIED;

        response(status, headers, new GenericEntity<>("entity", String.class), new Annotation[0], MediaType.TEXT_PLAIN_TYPE);
        // 这个get就是HttpRequest 发送的，然后得到HttpResponse
        HttpResponse<String> httpResponse = get("/test");

        assertArrayEquals(new String[]{"SESSION_ID=session", "USER_ID=user"},
                httpResponse.headers().allValues("Set-Cookie").toArray(String[]::new));
    }


    //TODO: writer body using MessageBodyWriter
    @Test
    public void should_write_entity_to_http_response_using_message_body_writer() {
        GenericEntity<Object> entity = new GenericEntity<>("entity", String.class);
        Annotation[] annotations = new Annotation[0];
        MediaType mediaType = MediaType.TEXT_PLAIN_TYPE;
        //为什么这里就不能用304了？因为现在要使用MessageBodyWriter写东西了，30x状态码没办法携带Body啊，所以，要使用200
        response(Response.Status.OK,new MultivaluedHashMap<>(),entity,annotations,mediaType);
    }

    //TODO: 500 if MessageBodyWriter not found
    //TODO: throw WebApplicationException with response,use response
    //TODO: throw WebApplicationException with response,use ExceptionMapper build response
    //TODO: throw other exception,use ExceptionMapper build response
    private void response(Response.Status status, MultivaluedMap<String, Object> headers, GenericEntity<Object> entity, Annotation[] annotations, MediaType mediaType) {
        OutboundResponse response = mock(OutboundResponse.class);
        //不设置这个，那么status默认就是0，那么就不合规范
        when(response.getStatus()).thenReturn(status.getStatusCode());
        when(response.getHeaders()).thenReturn(headers);
        when(router.dispatch(any(), eq(resourceContext))).thenReturn(response);
    }
}
