package com.geektime.tdd.rest;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Response;

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
        resp.setStatus(response.getStatus());
    }
}
