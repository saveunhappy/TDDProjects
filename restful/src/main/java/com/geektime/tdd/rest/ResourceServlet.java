package com.geektime.tdd.rest;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Providers;
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
        Providers providers = runtime.getProviders();
        //when(response.getStatus()).thenReturn(Response.Status.NOT_MODIFIED.getStatusCode());
        //就是在这里stub了，所以response.getStatus()就会返回Response.Status.NOT_MODIFIED.getStatusCode()，
        //这个时候/test这个接口是在req中能获取，但是，我们没有用到，只是用到了resp,所以，这个测试是可以通过的

        /*
        *       WebApplicationException exception = new WebApplicationException(response);
                when(router.dispatch(any(), eq(resourceContext))).thenThrow(exception);
        *       stub的就是这样的，所以执行到这就会抛出异常
        * */
        OutboundResponse response;
        try {
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
        respond(resp, providers, response);

    }

    private static void respond(HttpServletResponse resp, Providers providers, OutboundResponse response) throws IOException {
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
