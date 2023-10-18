package com.geektime.tdd.args;

import java.util.Map;

@SuppressWarnings("unchecked")
public class Args {

    public static <T> T parse(Class<T> optionsClass, String... args) {
        OptionClass<T> tOptionClass = new OptionClass<T>(optionsClass, PARSER);
        return OptionClass.getT(tOptionClass.parsers, tOptionClass.optionsClass, args);
    }

    private static Map<Class<?>, OptionParser> PARSER = Map.of(
            boolean.class, OptionParsers.bool(),
            int.class, OptionParsers.unary(0, Integer::parseInt),
            String.class, OptionParsers.unary("", String::valueOf),
            String[].class, OptionParsers.list(String::valueOf, String[]::new),
            Integer[].class, OptionParsers.list(Integer::parseInt, Integer[]::new)
    );

}
