package com.geektime.tdd.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.RuntimeDelegate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResourceDispatcherTest {
    private RuntimeDelegate delegate;

    @Test
    public void should(){

        HttpServletRequest request = mock(HttpServletRequest.class);
        ResourceContext context = mock(ResourceContext.class);

        when(request.getServletPath()).thenReturn("/users");
        when(context.getResource(eq(Users.class))).thenReturn(new Users());
        Router router = new Router(Users.class);
        OutboundResponse response = router.dispatch(request, context);
        GenericEntity<String> entity = (GenericEntity<String>) response.getEntity();
        assertEquals("all",entity.getEntity());

    }
    @BeforeEach
    public void before() {
        //先是mock掉，其实和创建一个子类没啥区别
        delegate = mock(RuntimeDelegate.class);
        //然后设置全局的
        RuntimeDelegate.setInstance(delegate);
        //这个时候就是使用mock的对象，设置他对应的行为。
        when(delegate.createResponseBuilder()).thenReturn(new Response.ResponseBuilder() {
            private Object entity;
            private int status;

            @Override
            public Response build() {
                OutboundResponse response = mock(OutboundResponse.class);
                when(response.getEntity()).thenReturn(entity);
                return response;
            }

            @Override
            public Response.ResponseBuilder clone() {
                return null;
            }

            @Override
            public Response.ResponseBuilder status(int status) {
                return null;
            }

            @Override
            public Response.ResponseBuilder status(int status, String reasonPhrase) {
                this.status = status;
                return this;
            }

            @Override
            public Response.ResponseBuilder entity(Object entity) {
                this.entity = entity;
                return this;
            }

            @Override
            public Response.ResponseBuilder entity(Object entity, Annotation[] annotations) {
                return null;
            }

            @Override
            public Response.ResponseBuilder allow(String... methods) {
                return null;
            }

            @Override
            public Response.ResponseBuilder allow(Set<String> methods) {
                return null;
            }

            @Override
            public Response.ResponseBuilder cacheControl(CacheControl cacheControl) {
                return null;
            }

            @Override
            public Response.ResponseBuilder encoding(String encoding) {
                return null;
            }

            @Override
            public Response.ResponseBuilder header(String name, Object value) {
                return null;
            }

            @Override
            public Response.ResponseBuilder replaceAll(MultivaluedMap<String, Object> headers) {
                return null;
            }

            @Override
            public Response.ResponseBuilder language(String language) {
                return null;
            }

            @Override
            public Response.ResponseBuilder language(Locale language) {
                return null;
            }

            @Override
            public Response.ResponseBuilder type(MediaType type) {
                return null;
            }

            @Override
            public Response.ResponseBuilder type(String type) {
                return null;
            }

            @Override
            public Response.ResponseBuilder variant(Variant variant) {
                return null;
            }

            @Override
            public Response.ResponseBuilder contentLocation(URI location) {
                return null;
            }

            @Override
            public Response.ResponseBuilder cookie(NewCookie... cookies) {
                return null;
            }

            @Override
            public Response.ResponseBuilder expires(Date expires) {
                return null;
            }

            @Override
            public Response.ResponseBuilder lastModified(Date lastModified) {
                return null;
            }

            @Override
            public Response.ResponseBuilder location(URI location) {
                return null;
            }

            @Override
            public Response.ResponseBuilder tag(EntityTag tag) {
                return null;
            }

            @Override
            public Response.ResponseBuilder tag(String tag) {
                return null;
            }

            @Override
            public Response.ResponseBuilder variants(Variant... variants) {
                return null;
            }

            @Override
            public Response.ResponseBuilder variants(List<Variant> variants) {
                return null;
            }

            @Override
            public Response.ResponseBuilder links(Link... links) {
                return null;
            }

            @Override
            public Response.ResponseBuilder link(URI uri, String rel) {
                return null;
            }

            @Override
            public Response.ResponseBuilder link(String uri, String rel) {
                return null;
            }
        });

    }
    static class Router implements ResourceRouter{
        private Map<Pattern,Class<?>> routerTable = new HashMap<>();
        public Router(Class<Users> rootResource) {
            Path path = rootResource.getAnnotation(Path.class);
            /*
            * 它匹配一个以“/”开始的字符串。
              然后匹配任意数量的字符（除了换行符），这些字符跟随在“/”之后。
              整个模式是可选的，这意味着如果字符串中没有以“/”开始的部分，正则表达式仍然会匹配成功，只是不会捕获任何内容。
              "/hello/world"：这里会匹配整个字符串，并捕获 "/hello/world"（不包括最前面的“/”）。
              "/"：这里会匹配字符串，并捕获一个空字符串（因为“/”后面没有其他字符）。
              ""（空字符串）：这里正则表达式的捕获组部分不会匹配，但整个表达式仍然会匹配成功，因为没有要求必须匹配捕获组
            * */
            routerTable.put(Pattern.compile(path.value()+"(/.*)?"),rootResource);
        }

        @Override
        public OutboundResponse dispatch(HttpServletRequest request, ResourceContext resourceContext) {
            String path = request.getServletPath();
            //注意，这个keySet就是只要key去做事，因为这里的key就是Pattern,不需要value,找到了就返回那个Pattern
            Pattern matched = routerTable.keySet().stream().filter(pattern -> pattern.matcher(path).matches()).findFirst().get();
            //找到了那么就是找到了path对象的那个对象
            Class<?> resource = routerTable.get(matched);
            Method method = Arrays.stream(resource.getMethods()).filter(m -> m.isAnnotationPresent(GET.class)).findFirst().get();
            Object object = resourceContext.getResource(resource);

            try {
                Object result = method.invoke(object);
                GenericEntity entity = new GenericEntity(result,method.getGenericReturnType());
                //这个Response.ok(entity)进行stub了，所以放进去的是GenericEntity，在测试类中取出来也是GenericEntity
                return (OutboundResponse) Response.ok(entity).build();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Path("/users")
    static class Users {

        @GET
        public String get(){
            return "all";
        }

    }
}
