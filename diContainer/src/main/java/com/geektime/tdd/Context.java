package com.geektime.tdd;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Arrays.stream;

public class Context {
    private Map<Class<?>, Provider<?>> providers = new HashMap<>();

    //这个是泛型方法，如果再类上加上了<T> 方法public T sayHay(){}这个是普通方法，下卖弄这个才是泛型方法
    //测试通过，就是把一个类的Class绑定到它的实例上面去。
    public <Type> void bind(Class<Type> type, Type instance) {
        providers.put(type, (Provider<Type>) () -> instance);
    }

    //这个和    public static <T> T parse(Class<T> optionsClass, String... args) 一样的，只是泛型的名字变了。


    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation) {
        Constructor<?>[] injectConstructors = stream(implementation.getConstructors())
                .filter(c -> c.isAnnotationPresent(Inject.class))
                .toArray(Constructor<?>[]::new);
        if(injectConstructors.length > 1) throw new IllegalComponentException();
        //找不到被@Inject标注的，并且找不到默认的构造函数
        if(injectConstructors.length == 0 && stream(implementation.getConstructors())
                .filter(c->c.getParameters().length == 0).findFirst().map(c->false).orElse(true))
            throw new IllegalComponentException();
        Constructor<Implementation> injectConstructor = getInjectConstructor(implementation);

        providers.put(type, (Provider<Type>) () -> {
            try {
                Object[] dependencies = stream(injectConstructor.getParameters()).map(p -> get(p.getType()))
                        .toArray(Object[]::new);
                return (Type) injectConstructor.newInstance(dependencies);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static <Type> Constructor<Type> getInjectConstructor(Class<Type> implementation)  {
        Stream<Constructor<?>> injectConstructor = stream(implementation.getConstructors())
                .filter(c -> c.isAnnotationPresent(Inject.class));
        //找不到被@Inject标注的，并且找不到默认的构造函数
        return (Constructor<Type>) injectConstructor.findFirst().orElseGet(() -> {
            try {
                return implementation.getConstructor();
            } catch (NoSuchMethodException e) {
                throw new IllegalComponentException();
            }
        });

    }

    public <Type> Type get(Class<Type> type) {
        return (Type) providers.get(type).get();
    }

}
