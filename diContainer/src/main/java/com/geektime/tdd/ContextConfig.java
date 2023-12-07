package com.geektime.tdd;

import jakarta.inject.Provider;

import java.lang.reflect.Type;
import java.util.*;

import static java.util.Arrays.stream;

public class ContextConfig {
    private final Map<Class<?>, ComponentProvider<?>> providers = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {
        providers.put(type, context -> instance);
    }

    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation) {
        providers.put(type, new InjectionProvider<>(implementation));
    }

    public Context getContext() {
        //这个dependencies中就是记录了所有的，还有你的参数中有的依赖，也去给你put进去，
        for (Class<?> component : providers.keySet()) {
            checkDependencies(component, new Stack<>());
        }
        return new Context() {
            @Override
            public Optional<?> get(Ref ref) {
                if (ref.isContainer()) {
                    if (ref.getContainer() != Provider.class) return Optional.empty();
                    return Optional.ofNullable(providers.get(ref.getComponent()))
                            .map(provider -> (Provider<Object>) () -> provider.get(this));
                }
                return Optional.ofNullable(providers.get(ref.getComponent())).
                        map(provider -> provider.get(this));
            }

        };
    }

    private void checkDependencies(Class<?> component, Stack<Class<?>> visiting) {
        /*注意原来的实现，dependencies.get(component)获取的是什么？是一个List，就是所有的依赖，然后接下来就是去判断
         * containsKey,如果没有Bind过，那么当然没有啊*/
        //这个是去找的所有bind过的依赖，然后把所有的key的依赖都放到一个栈中去，这里是找的所有的依赖，如果之前有添加过
        //那就说明有环了，就是有循环依赖
        Class<?> componentType = component;
        for (Type dependency : providers.get(componentType).getDependencies()) {
            Ref ref = Ref.of(dependency);
            if (!providers.containsKey(ref.getComponent())) throw new DependencyNotFoundException(componentType, ref.getComponent());
            if (!ref.isContainer()) {
                if (visiting.contains(ref.getComponent())) throw new CyclicDependenciesFoundException(visiting);
                visiting.push(ref.getComponent());
                checkDependencies(ref.getComponent(), visiting);
                visiting.pop();
            }

        }
    }

}
