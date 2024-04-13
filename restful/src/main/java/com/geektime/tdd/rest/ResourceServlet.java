package com.geektime.tdd.rest;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.RuntimeDelegate;

import java.io.IOException;

public class ResourceServlet extends HttpServlet {
    private Runtime runtime;

    public ResourceServlet(Runtime runtime) {
        this.runtime = runtime;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ResourceRouter router = runtime.getResourceRouter();
        //when(response.getStatus()).thenReturn(Response.Status.NOT_MODIFIED.getStatusCode());
        //就是在这里stub了，所以response.getStatus()就会返回Response.Status.NOT_MODIFIED.getStatusCode()，
        //这个时候/test这个接口是在req中能获取，但是，我们没有用到，只是用到了resp,所以，这个测试是可以通过的
        OutboundResponse response = router.dispatch(req, runtime.createResourceContext(req, resp));
        //if (sc <= 0) throw new IllegalArgumentException();
        resp.setStatus(response.getStatus());
        MultivaluedMap<String, Object> headers = response.getHeaders();
        //这个name就是Set-Cookie,目前测试数据是只有这个一个
        for (String name : headers.keySet()) {
            //这个value就是NewCookie的SESSION_ID和USERID，所以放的时候就是用NewCookie.class
            for (Object value : headers.get(name)) {
                RuntimeDelegate.HeaderDelegate headerDelegate = RuntimeDelegate.getInstance().createHeaderDelegate(value.getClass());
                resp.addHeader(name,headerDelegate.toString(value));
            }
        }
    }
}
