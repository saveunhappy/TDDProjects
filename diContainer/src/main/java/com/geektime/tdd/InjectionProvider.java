package com.geektime.tdd;

import jakarta.inject.Inject;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.stream.Stream.concat;

class InjectionProvider<T> implements ComponentProvider<T> {

    private Constructor<T> injectConstructor;

    private List<Field> injectFields;

    private List<Method> injectMethods;

    public InjectionProvider(Class<T> component) {
        if (Modifier.isAbstract(component.getModifiers())) throw new IllegalComponentException();
        this.injectConstructor = getInjectConstructor(component);
        this.injectFields = getInjectFields(component);
        this.injectMethods = getInjectMethods(component);
        if (injectFields.stream().anyMatch(f -> Modifier.isFinal(f.getModifiers())))
            throw new IllegalComponentException();
        if (injectMethods.stream().anyMatch(m -> m.getTypeParameters().length != 0))
            throw new IllegalComponentException();
    }


    @Override
    public T get(Context context) {
        try {
            T instance = injectConstructor.newInstance(toDependencies(context, injectConstructor));
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
    public List<Class<?>> getDependency() {
        return concat(concat(stream(injectConstructor.getParameters()).map(Parameter::getType), injectFields.stream().map(Field::getType)),
                //是要取所有method的所有参数，method有多个，一个method又有多个参数，所以使用flatMap
                injectMethods.stream().flatMap(m -> stream(m.getParameterTypes()))).toList();
    }

    private static <T> List<Field> getInjectFields(Class<T> component) {
        BiFunction<List<Field>, Class<?>, List<Field>> function = InjectionProvider::getC;
        return traverse(component, function);
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

    private static List<Field> getC(List<Field> fields, Class<?> current) {
        return injectable(current.getDeclaredFields()).toList();
    }

    private static <T> List<Method> getInjectMethods(Class<T> component) {

        BiFunction<List<Method>, Class<?>, List<Method>> function = (methods, current) -> getC(component, methods, current);
        List<Method> injectMethods = traverse(component, function);
        Collections.reverse(injectMethods);
        return injectMethods;
    }

    private static <T> List<Method> traverse1(Class<T> component, BiFunction<List<Method>, Class<?>, List<Method>> function) {
        List<Method> injectMethods = new ArrayList<>();
        Class<?> current = component;
        while (current != Object.class) {
            injectMethods.addAll(function.apply(injectMethods,current));
            current = current.getSuperclass();
        }
        return injectMethods;
    }

    private static <T> List<Method> getC(Class<T> component, List<Method> injectMethods, Class<?> current) {
        return injectable(current.getDeclaredMethods())
                .filter(m -> isOverrideByInjectMethod(injectMethods, m))
                .filter(m -> isOverrideByNoInjectMethod(component, m))
                .toList();
    }

    private static <Type> Constructor<Type> getInjectConstructor(Class<Type> implementation) {
        List<Constructor<?>> injectConstructors = injectable(implementation.getConstructors()).toList();
        if (injectConstructors.size() > 1) throw new IllegalComponentException();
        //找不到被@Inject标注的，并且找不到默认的构造函数
        return (Constructor<Type>) injectConstructors.stream().findFirst().orElseGet(() -> defaultConstructor(implementation));
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
        return stream(executable.getParameterTypes())
                .map(t -> context.get(t).get())
                .toArray();
    }

    private static Object toDependency(Context context, Field field) {
        return context.get(field.getType()).get();
    }
}
