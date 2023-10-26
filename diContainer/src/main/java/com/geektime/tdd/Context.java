package com.geektime.tdd;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Context {
    private Map<Class<?>, Object> components = new HashMap<>();

    //这个是泛型方法，如果再类上加上了<T> 方法public T sayHay(){}这个是普通方法，下卖弄这个才是泛型方法
    //测试通过，就是把一个类的Class绑定到它的实例上面去。
    public <ComponentType> void bind(Class<ComponentType> type, ComponentType instance) {
        components.put(type,instance);
    }

    //这个和    public static <T> T parse(Class<T> optionsClass, String... args) 一样的，只是泛型的名字变了。
    public <ComponentType> ComponentType get(Class<ComponentType> type) {
        return (ComponentType) components.get(type);
    }
}
