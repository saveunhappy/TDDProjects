package com.geektime.tdd.args;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

class ExampleClass {
    public void method1(int a, String b) {}
    public void method2(double c, boolean d) {}
}

public class Main {
    public static void main(String[] args) throws NoSuchMethodException {
        // 获取 ExampleClass 的所有方法
        Class<ExampleClass> exampleClass = ExampleClass.class;
        Method method1 = exampleClass.getMethod("method1", int.class, String.class);
        Method method2 = exampleClass.getMethod("method2", double.class, boolean.class);
        List<Method> injectMethods = new ArrayList<>();
        injectMethods.add(method1);
        injectMethods.add(method2);

        // 使用 map 方法
        Stream<Class<?>[]> mapResult = injectMethods.stream().map(Method::getParameterTypes);
        System.out.println("使用 map 方法的结果：");
        mapResult.forEach(arr -> System.out.println(Arrays.toString(arr)));

        // 使用 flatMap 方法
        List<Class<?>> flatMapResult = injectMethods.stream()
                .flatMap(m -> Arrays.stream(m.getParameterTypes()))
                .toList();
        System.out.println("\n使用 flatMap 方法的结果：");
        flatMapResult.forEach(System.out::println);
        System.out.println("\n使用 flatMap222 方法的结果：");

        Stream<Class<?>> classStream = injectMethods.stream()
                .flatMap(m -> Arrays.stream(m.getParameterTypes()));
        classStream.forEach(System.out::println);


    }
}
