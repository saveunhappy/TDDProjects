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
        OutboundResponse response;
        try {
            //看下这个结构，我们是先声明了一个OutboundResponse，然后不管是正常还是异常，都是返回了一个response，
            // 然后最后再去调用那个方法，那么其实可以替换成每个都去调用，就不用最后处理了，在重构那本书中这个是个坏味道，
            // 但是在这里是个好方法，因为我们这里有递归，重构的案例是个普通的方法
//            respond(resp, response);
//            respond(resp, router.dispatch(req, runtime.createResourceContext(req, resp)));
            response = router.dispatch(req, runtime.createResourceContext(req, resp));
        } catch (WebApplicationException exception) {
            //注意，异常的构造器就是接收一个response，而且是在所有stub之后的，所以状态码什么的已经设置过了
            response = (OutboundResponse) exception.getResponse();
        } catch (Throwable throwable) {
            try{
                ExceptionMapper exceptionMapper = providers.getExceptionMapper(throwable.getClass());
                response = (OutboundResponse) exceptionMapper.toResponse(throwable);
            }catch (WebApplicationException exception){
                response = (OutboundResponse) exception.getResponse();
            }catch (Throwable throwable1){
                ExceptionMapper exceptionMapper = providers.getExceptionMapper(throwable1.getClass());
                response = (OutboundResponse) exceptionMapper.toResponse(throwable1);
            }

        }
        respond(resp, response);

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
