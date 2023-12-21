package com.geektime.tdd;

import jakarta.inject.Provider;
import jakarta.inject.Qualifier;
import jakarta.inject.Scope;

import java.lang.annotation.Annotation;
import java.util.*;

import static java.util.Arrays.stream;

public class ContextConfig {
    private final Map<Component, ComponentProvider<?>> components = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {
        components.put(new Component(type, null), context -> instance);
    }

    public <Type> void bind(Class<Type> type, Type instance, Annotation... qualifiers) {
        if (stream(qualifiers).anyMatch(q -> !q.annotationType().isAnnotationPresent(Qualifier.class))) {
            throw new IllegalComponentException();
        }
        for (Annotation qualifier : qualifiers) {
            components.put(new Component(type, qualifier), context -> instance);
        }
    }

    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation) {
        components.put(new Component(type, null), new InjectionProvider<>(implementation));
    }

    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation, Annotation... annotations) {
        if (stream(annotations).map(Annotation::annotationType)
                .anyMatch(t -> !t.isAnnotationPresent(Qualifier.class) && !t.isAnnotationPresent(Scope.class))) {
            throw new IllegalComponentException();
        }
//        List<? extends Class<? extends Annotation>> qualifiers = stream(annotations).map(Annotation::annotationType).filter(a -> a.isAnnotationPresent(Qualifier.class)).toList();
        List<Annotation> qualifiers = stream(annotations).filter(a -> a.annotationType().isAnnotationPresent(Qualifier.class)).toList();

        for (Annotation qualifier : qualifiers) {
            components.put(new Component(type, qualifier), new InjectionProvider<>(implementation));
        }
    }

    public Context getContext() {
        //这个dependencies中就是记录了所有的，还有你的参数中有的依赖，也去给你put进去，

        for (Component component : components.keySet()) {
            checkDependencies(component, new Stack<>());
        }
        return new Context() {
            @Override
            public <ComponentType> Optional<ComponentType> get(ComponentRef<ComponentType> ref) {
                if (ref.isContainer()) {
                    if (ref.getContainer() != Provider.class) return Optional.empty();
                    return (Optional<ComponentType>) Optional.ofNullable(getComponent(ref))
                            .map(provider -> (Provider<Object>) () -> provider.get(this));
                }
                return Optional.ofNullable(getComponent(ref)).
                        map(provider -> (ComponentType) provider.get(this));
            }

        };
    }

    private <ComponentType> ComponentProvider<?> getComponent(ComponentRef<ComponentType> ref) {
        return components.get(ref.component());
    }

    private void checkDependencies(Component component, Stack<Component> visiting) {
        /*注意原来的实现，dependencies.get(component)获取的是什么？是一个List，就是所有的依赖，然后接下来就是去判断
         * containsKey,如果没有Bind过，那么当然没有啊*/
        //这个是去找的所有bind过的依赖，然后把所有的key的依赖都放到一个栈中去，这里是找的所有的依赖，如果之前有添加过
        //那就说明有环了，就是有循环依赖
        for (ComponentRef dependency : components.get(component).getDependencies()) {
            //这个dependency.component()就是指的Dependency，其实就是dependency本身，但是有其他的属性，所以就
            //把属性封装到了component()这个方法中去，
            if (!components.containsKey(dependency.component())) {
                throw new DependencyNotFoundException(component, dependency.component());
            }
            if (!dependency.isContainer()) {
                if (visiting.contains(dependency.component())) throw new CyclicDependenciesFoundException(visiting);
                visiting.push(dependency.component());
                checkDependencies(dependency.component(), visiting);
//                checkDependencies(dependency.getComponent(), visiting);
                visiting.pop();
            }

        }
    }

}
