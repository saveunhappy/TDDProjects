package com.geektime.tdd.rest;

import jakarta.servlet.Servlet;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.RuntimeDelegate;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

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

    @Override
    protected Servlet getServlet() {
        runtime = mock(Runtime.class);
        router = mock(ResourceRouter.class);
        resourceContext = mock(ResourceContext.class);

        when(runtime.getResourceRouter()).thenReturn(router);
        //根据Request和Response去获取对应的@Path对象，我们现在不关心是啥，因为请求和响应还没进来
        when(runtime.createResourceContext(any(), any())).thenReturn(resourceContext);

        return new ResourceServlet(runtime);
    }

    //TODO: use status code as http status
    @Test
    public void should_use_status_from_response() throws Exception {
        OutboundResponse response = mock(OutboundResponse.class);
        //当返回的时候就返回30x的状态码，200正常，500是失败，但是这种状态码你不知道到底是成功了还是失败了。
        //所以30x状态不容易冲突，那么就用这个状态码
        when(response.getStatus()).thenReturn(Response.Status.NOT_MODIFIED.getStatusCode());
        when(router.dispatch(any(), eq(resourceContext))).thenReturn(response);
        // 这个get就是HttpRequest 发送的，然后得到HttpResponse
        HttpResponse<String> httpResponse = get("/test");
        assertEquals(Response.Status.NOT_MODIFIED.getStatusCode(), httpResponse.statusCode());

    }

    //TODO: use headers as http header
    @Test
    public void should_use_http_headers_from_response() throws Exception {
        /*
         * 这个时候在build的时候报错：Provider for jakarta.ws.rs.ext.RuntimeDelegate cannot be found
         * 因为构建的时候是用RuntimeDelegate这个类的，那么我们也给stub掉
         * */

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

        NewCookie sessionId = new NewCookie.Builder("SESSION_ID").value("session").build();
        NewCookie userId = new NewCookie.Builder("USER_ID").value("user").build();
        OutboundResponse response = mock(OutboundResponse.class);
        MultivaluedMap<String,Object> headers = new MultivaluedHashMap<>();
        // key是Set-Cookie，value是一个list,就是SESSION_ID和USER_ID

        headers.addAll("Set-Cookie", sessionId, userId);
        //不设置这个，那么status默认就是0，那么就不合规范
        when(response.getStatus()).thenReturn(Response.Status.NOT_MODIFIED.getStatusCode());
        when(response.getHeaders()).thenReturn(headers);
        when(router.dispatch(any(), eq(resourceContext))).thenReturn(response);
        // 这个get就是HttpRequest 发送的，然后得到HttpResponse
        HttpResponse<String> httpResponse = get("/test");

        assertArrayEquals(new String[]{"SESSION_ID=session","USER_ID=user"},
                httpResponse.headers().allValues("Set-Cookie").toArray(String[]::new));
    }

    //TODO: writer body using MessageBodyWriter
    //TODO: 500 if MessageBodyWriter not found
    //TODO: throw WebApplicationException with response,use response
    //TODO: throw WebApplicationException with response,use ExceptionMapper build response
    //TODO: throw other exception,use ExceptionMapper build response
}
