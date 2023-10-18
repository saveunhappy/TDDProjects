package com.geektime.tdd.args;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

class OptionClass<T> {
    private Class<T> optionsClass;
    private Map<Class<?>, OptionParser> parsers;

    public OptionClass(Class<T> optionsClass, Map<Class<?>, OptionParser> parsers) {
        this.optionsClass = optionsClass;
        this.parsers = parsers;
    }

    public T parse(String[] args) {
        try {
            List<String> arguments = Arrays.asList(args);
            Constructor<?> constructor = this.optionsClass.getDeclaredConstructors()[0];
            Object[] values = Arrays.stream(constructor.getParameters())
                    .map(it -> parseOption(arguments, it)).toArray();

            return (T) constructor.newInstance(values);
        } catch (IllegalOptionException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Object parseOption(List<String> arguments, Parameter parameter) {
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
