package com.geektime.tdd;

public class Context {
    //这个是泛型方法，如果再类上加上了<T> 方法public T sayHay(){}这个是普通方法，下卖弄这个才是泛型方法
    public <ComponentType> void bind(Class<ComponentType> componentClass, ComponentType instance) {

    }

    //这个和    public static <T> T parse(Class<T> optionsClass, String... args) 一样的，只是泛型的名字变了。
    public <ComponentType> ComponentType get(Class<ComponentType> componentClass) {
        return null;
    }
}
