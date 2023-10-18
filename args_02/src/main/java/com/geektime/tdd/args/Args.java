package com.geektime.tdd.args;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public class Args {

    public static <T> T parse(Class<T> optionsClass, String... args) {
        return parse(PARSER, optionsClass, args);
    }

    private static Map<Class<?>, OptionParser> PARSER = Map.of(
            boolean.class, OptionParsers.bool(),
            int.class, OptionParsers.unary(0, Integer::parseInt),
            String.class, OptionParsers.unary("", String::valueOf),
            String[].class, OptionParsers.list(String::valueOf, String[]::new),
            Integer[].class, OptionParsers.list(Integer::parseInt, Integer[]::new)
    );

    public static <T> T parse(Map<Class<?>, OptionParser> parsers, Class<T> optionsClass, String[] args) {
        try {
            List<String> arguments = Arrays.asList(args);
            Constructor<?> constructor = optionsClass.getDeclaredConstructors()[0];
            Object[] values = Arrays.stream(constructor.getParameters())
                    .map(it -> parseOption(arguments, it, parsers)).toArray();

            return (T) constructor.newInstance(values);
        } catch (IllegalOptionException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Object parseOption(List<String> arguments, Parameter parameter, Map<Class<?>, OptionParser> parsers) {
        if (!parameter.isAnnotationPresent(Option.class)) throw new IllegalOptionException(parameter.getName());
        Option option = parameter.getAnnotation(Option.class);
        //这个就是l,p,d,传的参数是-l,-p,-d,
        Class<?> type = parameter.getType();
        if (!parsers.containsKey(parameter.getType())) {
            throw new UnsupportedOptionTypeException(option.value(), parameter.getType());
        }
        return parsers.get(type).parse(arguments, parameter.getAnnotation(Option.class));
    }
}
