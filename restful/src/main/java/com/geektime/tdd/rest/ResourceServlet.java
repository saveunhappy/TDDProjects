package com.geektime.tdd.rest;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Providers;
import jakarta.ws.rs.ext.RuntimeDelegate;

import java.io.IOException;
import java.util.function.Supplier;

public class ResourceServlet extends HttpServlet {
    private Runtime runtime;
    private Providers providers;

    public ResourceServlet(Runtime runtime) {
        this.runtime = runtime;
        this.providers = runtime.getProviders();

    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ResourceRouter router = runtime.getResourceRouter();
        //这个是个lambda，为什么是延迟执行，因为这里并不是立即执行router.dispatch(req, runtime.createResourceContext(req, resp));
        //这个方法，而是去创建了一个Supplier对象，但是什么时候执行呢?是在respond(resp, supplier.get());这里，这个supplier.get()
        //就是代表router.dispatch(req, runtime.createResourceContext(req, resp));这个时候才去执行，因为平时你就是
        //传过去的时候那个时候就已经经过evaluate了，但是这个没有，为什么?还是刚开始说的，传过去的是一个Supplier对象啊，
        //又不是一个立即执行的方法
        respond(resp, () -> router.dispatch(req, runtime.createResourceContext(req, resp)));

    }

    private void respond(HttpServletResponse resp, Supplier<OutboundResponse> supplier) throws IOException {
        try {
            respond(resp, supplier.get());
        } catch (WebApplicationException exception) {
            respond(resp, () -> (OutboundResponse) exception.getResponse());
        } catch (Throwable throwable) {
            respond(resp, () -> from(throwable));
        }
    }

    private OutboundResponse from(Throwable throwable) {
        ExceptionMapper exceptionMapper = providers.getExceptionMapper(throwable.getClass());
        return (OutboundResponse) exceptionMapper.toResponse(throwable);
    }

    private void respond(HttpServletResponse resp, OutboundResponse response) throws IOException {
        //if (sc <= 0) throw new IllegalArgumentException();
        resp.setStatus(response.getStatus());
        MultivaluedMap<String, Object> headers = response.getHeaders();
        //这个name就是Set-Cookie,目前测试数据是只有这个一个
        for (String name : headers.keySet()) {
            //这个value就是NewCookie的SESSION_ID和USERID，所以放的时候就是用NewCookie.class
            for (Object value : headers.get(name)) {
                RuntimeDelegate.HeaderDelegate headerDelegate = RuntimeDelegate.getInstance().createHeaderDelegate(value.getClass());
                resp.addHeader(name, headerDelegate.toString(value));
            }
        }
        GenericEntity entity = response.getGenericEntity();
        if (entity != null) {
            MessageBodyWriter writer = providers.getMessageBodyWriter(entity.getRawType(), entity.getType(), response.getAnnotations(), response.getMediaType());
            writer.writeTo(entity.getEntity(), entity.getRawType(), entity.getType(), response.getAnnotations(), response.getMediaType(),
                    response.getHeaders(), resp.getOutputStream());
        }
    }
}
