package com.geektime.tdd;

import jakarta.inject.Inject;
import jakarta.inject.Qualifier;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.stream.Stream.concat;

class InjectionProvider<T> implements ComponentProvider<T> {

    private final Constructor<T> injectConstructor;

    private final List<Field> injectFields;

    private final List<Method> injectMethods;

    private final List<ComponentRef> dependencies;

    private Injectable<Constructor<T>> injectableConstructor;
    public InjectionProvider(Class<T> component) {
        if (Modifier.isAbstract(component.getModifiers())) throw new IllegalComponentException();
        Constructor<T> constructor = getInjectConstructor(component);
        ComponentRef<?>[] require = stream(constructor.getParameters()).map(InjectionProvider::toComponentRef).toArray(ComponentRef<?>[]::new);
        this.injectableConstructor = new Injectable<>(constructor,require);

        this.injectConstructor = constructor;
        this.injectFields = getInjectFields(component);
        this.injectMethods = getInjectMethods(component);
        if (injectFields.stream().anyMatch(f -> Modifier.isFinal(f.getModifiers())))
            throw new IllegalComponentException();
        if (injectMethods.stream().anyMatch(m -> m.getTypeParameters().length != 0))
            throw new IllegalComponentException();
        dependencies = getDependencies();
    }


    @Override
    public T get(Context context) {
        try {
            T instance = injectableConstructor.element().newInstance(toDependencies(context, injectableConstructor.element()));
            for (Field field : injectFields) {
                field.set(instance, toDependency(context, field));
            }
            for (Method method : injectMethods) {
                method.invoke(instance, toDependencies(context, method));
            }
            return instance;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<ComponentRef> getDependencies() {
        return concat(concat(stream(injectableConstructor.require()),
                        injectFields.stream().map(InjectionProvider::toComponentRef)),
                //因为Constructor直接就是可以获取数组，所以不用flatMap,然后InjectMethod是List，所以要使用Stream
                //那为什么Map不行呢？因为后面的m.getParameterTypes()返回的还是数组，你要把它变成一维的，所以要使用flatMap
                injectMethods.stream().flatMap(p -> stream(p.getParameters()).map(InjectionProvider::toComponentRef)))
                .toList();
    }

    record Injectable<Element extends AccessibleObject>(Element element,ComponentRef<?>[] require){

    }

    private static Annotation getQualifier(AnnotatedElement field) {
        List<Annotation> qualifiers = stream(field.getAnnotations()).filter(a -> a.annotationType().isAnnotationPresent(Qualifier.class)).collect(Collectors.toList());
        if (qualifiers.size() > 1) throw new IllegalComponentException();
        return qualifiers.stream().findFirst().orElse(null);
    }


    private static <T> List<Method> getInjectMethods(Class<T> component) {

        List<Method> injectMethods = traverse(component, (methods, current) -> injectable(current.getDeclaredMethods())
                .filter(m -> isOverrideByInjectMethod(methods, m))
                //这个Component就是你去调用这个Component的方法，他的install方法没有标注@Inject，所以没有标注进来
                //但是一直在网上找，如果没有这句代码，标注了@Inject的install方法就被放进来了，但是你不应该放进来
                //所以如果你发现你的install方法标注了@Inject，你的子类没有标注，那就去掉它
                .filter(m -> isOverrideByNoInjectMethod(component, m))
                .toList());
        Collections.reverse(injectMethods);
        return injectMethods;
    }

    private static <Type> Constructor<Type> getInjectConstructor(Class<Type> implementation) {
        List<Constructor<?>> injectConstructors = injectable(implementation.getConstructors()).toList();
        if (injectConstructors.size() > 1) throw new IllegalComponentException();
        //找不到被@Inject标注的，并且找不到默认的构造函数
        return (Constructor<Type>) injectConstructors.stream().findFirst().orElseGet(() -> defaultConstructor(implementation));
    }

    private static <T> List<T> traverse(Class<?> component, BiFunction<List<T>, Class<?>, List<T>> finder) {
        List<T> members = new ArrayList<>();
        Class<?> current = component;
        while (current != Object.class) {
            members.addAll(finder.apply(members, current));
            current = current.getSuperclass();
        }
        return members;
    }

    private static <Type> Constructor<Type> defaultConstructor(Class<Type> implementation) {
        try {
            return implementation.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            //这里之前是RuntimeException，改成我们的自定义异常了
            throw new IllegalComponentException();
        }
    }

    private static <T extends AnnotatedElement> Stream<T> injectable(T[] declaredFields) {
        return stream(declaredFields).filter(f -> f.isAnnotationPresent(Inject.class));
    }

    private static boolean isOverride(Method m, Method o) {
        return o.getName().equals(m.getName()) && Arrays.equals(o.getParameterTypes(), m.getParameterTypes());
    }

    private static <T> boolean isOverrideByNoInjectMethod(Class<T> component, Method m) {
        return stream(component.getDeclaredMethods()).
                filter(m1 -> !m1.isAnnotationPresent(Inject.class))
                .noneMatch(o -> isOverride(m, o));
    }

    private static boolean isOverrideByInjectMethod(List<Method> injectMethods, Method m) {
        return injectMethods.stream().noneMatch(o -> isOverride(m, o));
    }

    private static Object[] toDependencies(Context context, Executable executable) {
        return stream(executable.getParameters())
                .map(p -> toDependency(context, toComponentRef(p))
                ).toArray();
    }

    private static Object toDependency(Context context, Field field) {
        return toDependency(context, toComponentRef(field));
    }

    private static Object toDependency(Context context, ComponentRef of) {
        return context.get(of).get();
    }

    private static ComponentRef toComponentRef(Field field) {
        return ComponentRef.of(field.getGenericType(), getQualifier(field));
    }
    private static ComponentRef<?> toComponentRef(Parameter parameter) {
        return ComponentRef.of(parameter.getParameterizedType(), getQualifier(parameter));
    }

    private static <T> List<Field> getInjectFields(Class<T> component) {
        return traverse(component, (fields, current) -> injectable(current.getDeclaredFields()).toList());
    }

}
