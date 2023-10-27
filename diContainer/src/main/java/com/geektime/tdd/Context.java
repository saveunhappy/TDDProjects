package com.geektime.tdd;

import jakarta.inject.Provider;

import java.util.HashMap;
import java.util.Map;

public class Context {
    private Map<Class<?>, Provider<?>> providers = new HashMap<>();

    //这个是泛型方法，如果再类上加上了<T> 方法public T sayHay(){}这个是普通方法，下卖弄这个才是泛型方法
    //测试通过，就是把一个类的Class绑定到它的实例上面去。
    public <Type> void bind(Class<Type> type, Type instance) {
        providers.put(type, (Provider<Type>) () -> instance);
    }

    //这个和    public static <T> T parse(Class<T> optionsClass, String... args) 一样的，只是泛型的名字变了。


    public <Type,Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation) {
        providers.put(type, (Provider<Type>) () -> {
            try {
                return (Type) ((Class<?>) implementation).getConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public <Type> Type get(Class<Type> type) {
        return (Type) providers.get(type).get();
    }

}
