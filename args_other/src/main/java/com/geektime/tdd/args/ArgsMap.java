package com.geektime.tdd.args;

import com.geektime.tdd.args.exception.IllegalOptionException;
import com.geektime.tdd.args.exception.UnsupportedOptionTypeException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.function.Function;

public class ArgsMap<T> {
    public static Map<String, String[]> toMap(String... args) {
        Map<String, String[]> result = new HashMap<>();
//        String option = null;
//        List<String> values = new ArrayList<>();
//        for (String arg : args) {
//            if (arg.matches("^-[a-zA-Z-]+$")) {
//                if (option != null) {
//                    result.put(option.substring(1), values.toArray(String[]::new));
//                }
//                option = arg;
//                values = new ArrayList<>();
//            } else {
//                values.add(arg);
//            }
//        }
//        result.put(option.substring(1), values.toArray(String[]::new));
        return result;
    }

    private Class<T> optionsClass;
    private Map<Class<?>, OptionMapParser> parsers;
    private Function<String[], Map<String, String[]>> optionParser;

    public ArgsMap(Class<T> optionsClass, Map<Class<?>, OptionMapParser> parsers, Function<String[], Map<String, String[]>> optionParser) {
        this.optionsClass = optionsClass;
        this.parsers = parsers;
        this.optionParser = optionParser;
    }

    public T parse(String... args) {
        try {
           // Map<String, String[]> options = toMap(args);
            Map<String, String[]> options = optionParser.apply(args);
            Constructor<?> constructor = optionsClass.getDeclaredConstructors()[0];
            Object[] values = Arrays.stream(constructor.getParameters()).map(it -> parseOption(options, it)).toArray();
            return (T) constructor.newInstance(values);
        } catch (IllegalOptionException | UnsupportedOptionTypeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Object parseOption(Map<String, String[]> options, Parameter parameter) {
        if (!parameter.isAnnotationPresent(Option.class)) throw new IllegalOptionException(parameter.getName());
        Option option = parameter.getAnnotation(Option.class);
        if (!parsers.containsKey(parameter.getType())) {
            throw new UnsupportedOptionTypeException(option.value(), parameter.getType());
        }
        //在这里，get的是参数的类型对应的parser，你record传进来的是int类型的，那么获取到的就是int类型的parser,然后
        //怎么去parse,key是int,value是一个interface，外面传过来的，是一个接口
        // public interface OptionMapParser<T> {
        //     T parse(String[] values);
        // }
        //这个不是mock的，这个是真实存在的，外面规定好了怎么去parse
        return parsers.get(parameter.getType()).parse(options.get(option.value()));
    }

}