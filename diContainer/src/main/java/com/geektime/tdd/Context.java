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
        providers.put(type, (Provider<Type>) () -> {
            try {
                Constructor<Implementation> injectConstructor = getInjectConstructor(implementation);
                Object[] dependencies = stream(injectConstructor.getParameters()).map(p -> get(p.getType()))
                        .toArray(Object[]::new);
                return (Type) injectConstructor.newInstance(dependencies);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static <Type> Constructor<Type> getInjectConstructor(Class<Type> implementation) throws NoSuchMethodException {
        Stream<Constructor<?>> injectConstructor = stream(implementation.getConstructors())
                .filter(c -> c.isAnnotationPresent(Inject.class));
        return (Constructor<Type>) injectConstructor.findFirst().orElseGet(() -> {
            try {
                return implementation.getConstructor();
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        });
        //为什么这段代码会报错？因为这个OrElse不管你是不是空，它是一定会执行的，就是当你为空的时候，
        //他就返回implementation.getConstructor()这个没问题，
        //但是他需要提前把这个值给计算出，到时候直接给你返回，相当于饿汉式，
        //但是，我们这个方法是只有一个带参数的构造器，根本就没有无参的构造函数
        //他直接执行了可不就报错了么，但是OrElseGet不是，他是懒汉式，只有你为空的时候，他才去调用
        //implementation.getConstructor();这个方法，所以这里要使用OrElseGet
//        return (Constructor<Type>) injectConstructor.findFirst()
//                .orElse(
//                        implementation.getConstructor()
//                );
    }

    public <Type> Type get(Class<Type> type) {
        return (Type) providers.get(type).get();
    }

}
