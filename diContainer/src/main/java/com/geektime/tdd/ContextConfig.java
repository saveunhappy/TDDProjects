package com.geektime.tdd;

import jakarta.inject.Provider;

import java.lang.annotation.Annotation;
import java.util.*;

import static java.util.Arrays.stream;

public class ContextConfig {
    private final Map<Class<?>, ComponentProvider<?>> providers = new HashMap<>();
    private final Map<Component, ComponentProvider<?>> components = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {
        providers.put(type, context -> instance);
    }

    public <Type> void bind(Class<Type> type, Type instance, Annotation qualifier) {
        components.put(new Component(type, qualifier), context -> instance);
    }

    record Component(Class<?> type, Annotation qualifier) {

    }

    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation) {
        providers.put(type, new InjectionProvider<>(implementation));
    }

    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation,Annotation qualifier) {
        components.put(new Component(type, qualifier), new InjectionProvider<>(implementation));
    }
    public Context getContext() {
        //这个dependencies中就是记录了所有的，还有你的参数中有的依赖，也去给你put进去，
        for (Class<?> component : providers.keySet()) {
            checkDependencies(component, new Stack<>());
        }
        return new Context() {
            @Override
            public <ComponentType> Optional<ComponentType> get(Ref<ComponentType> ref) {
                if(ref.getQualifier() != null){
                    return Optional.ofNullable(components.get(
                            new Component(ref.getComponent(), ref.getQualifier())
                            )).
                            map(provider ->(ComponentType) provider.get(this));
                }
                if (ref.isContainer()) {
                    if (ref.getContainer() != Provider.class) return Optional.empty();
                    return (Optional<ComponentType>) Optional.ofNullable(providers.get(ref.getComponent()))
                            .map(provider -> (Provider<Object>) () -> provider.get(this));
                }
                return Optional.ofNullable(providers.get(ref.getComponent())).
                        map(provider ->(ComponentType) provider.get(this));
            }

        };
    }

    private void checkDependencies(Class<?> component, Stack<Class<?>> visiting) {
        /*注意原来的实现，dependencies.get(component)获取的是什么？是一个List，就是所有的依赖，然后接下来就是去判断
         * containsKey,如果没有Bind过，那么当然没有啊*/
        //这个是去找的所有bind过的依赖，然后把所有的key的依赖都放到一个栈中去，这里是找的所有的依赖，如果之前有添加过
        //那就说明有环了，就是有循环依赖
        Class<?> componentType = component;
        for (Context.Ref dependency : providers.get(componentType).getDependencies()) {
            if (!providers.containsKey(dependency.getComponent()))
                throw new DependencyNotFoundException(componentType, dependency.getComponent());
            if (!dependency.isContainer()) {
                if (visiting.contains(dependency.getComponent())) throw new CyclicDependenciesFoundException(visiting);
                visiting.push(dependency.getComponent());
                checkDependencies(dependency.getComponent(), visiting);
                visiting.pop();
            }

        }
    }

}
