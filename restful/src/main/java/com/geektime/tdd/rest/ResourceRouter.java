package com.geektime.tdd.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ResourceContext;

interface ResourceRouter {
    //这个就是根据这个请求的url，然后去ResourceContext中取得Controller。
    OutboundResponse dispatch(HttpServletRequest request, ResourceContext resourceContext);
}
