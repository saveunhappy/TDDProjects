package com.geektime.tdd.rest;

import com.geektime.tdd.Context;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.ext.Providers;
//这个RUNTIME时期就是对应的Application，因为JAX-RS认为你的实现就是JAX-RS这样一个运行时
public interface Runtime {
    Providers getProviders();

    ResourceContext createResourceContext(HttpServletRequest request, HttpServletResponse response);

    Context getApplicationContext();

    ResourceRouter getResourceRouter();

}
