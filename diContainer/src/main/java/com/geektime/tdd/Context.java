package com.geektime.tdd;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

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
        Constructor<Implementation> injectConstructor = getInjectConstructor(implementation);
        providers.put(type, getTypeProvider(injectConstructor));
    }

    private <Type> Provider<Type> getTypeProvider(Constructor<Type> injectConstructor) {
        return () -> getImplementation(injectConstructor);
    }

    private <Type> Type getImplementation(Constructor<Type> injectConstructor) {
        try {

            Object[] dependencies = stream(injectConstructor.getParameters())
                    .map(p -> get(p.getType()).orElseThrow(DependencyNotFoundException::new))
                    .toArray(Object[]::new);
            return injectConstructor.newInstance(dependencies);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    class ConstructorInjectionProvider<T> implements Provider<T>{
        private Constructor<T> injectConstructor;

        public ConstructorInjectionProvider(Constructor<T> injectConstructor) {
            this.injectConstructor = injectConstructor;
        }


        @Override
        public T get() {
            return getImplementation(injectConstructor);
        }
    }

    private static <Type> Constructor<Type> getInjectConstructor(Class<Type> implementation)  {
        List<Constructor<?>> injectConstructors = stream(implementation.getConstructors())
                .filter(c -> c.isAnnotationPresent(Inject.class)).collect(Collectors.toList());
        if(injectConstructors.size() > 1) throw new IllegalComponentException();

        //找不到被@Inject标注的，并且找不到默认的构造函数
        return (Constructor<Type>) injectConstructors.stream().findFirst().orElseGet(() -> {
            try {
                return implementation.getConstructor();
            } catch (NoSuchMethodException e) {
                //这里之前是RuntimeException，改成我们的自定义异常了
                throw new IllegalComponentException();
            }
        });

    }

    public <Type> Optional<Type> get(Class<Type> type) {
        return Optional.ofNullable(providers.get(type)).map(provider -> (Type)provider.get());
    }
}
