package com.geektime.tdd;

import jakarta.inject.Provider;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Context {
    private Map<Class<?>, Class<?>> componentImplementations = new HashMap<>();
    private Map<Class<?>, Provider<?>> providers = new HashMap<>();

    //这个是泛型方法，如果再类上加上了<T> 方法public T sayHay(){}这个是普通方法，下卖弄这个才是泛型方法
    //测试通过，就是把一个类的Class绑定到它的实例上面去。
    public <ComponentType> void bind(Class<ComponentType> type, ComponentType instance) {
        providers.put(type, (Provider<ComponentType>) () -> instance);
    }

    //这个和    public static <T> T parse(Class<T> optionsClass, String... args) 一样的，只是泛型的名字变了。


    public <ComponentType,ComponentImplementation extends ComponentType>
    void bind(Class<ComponentType> type, Class<ComponentImplementation> implementation) {
        componentImplementations.put(type,implementation);
    }

    public <ComponentType> ComponentType get(Class<ComponentType> type) {
        if(providers.containsKey(type)) return (ComponentType) providers.get(type).get();
        try {
            return (ComponentType) componentImplementations.get(type).getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
