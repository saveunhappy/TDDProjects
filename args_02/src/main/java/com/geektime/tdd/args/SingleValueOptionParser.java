package com.geektime.tdd.args;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;

class SingleValueOptionParser<T> implements OptionParser<T> {
    Function<String, T> valueParser;

    T defaultValue;


    private SingleValueOptionParser(T defaultValue, Function<String, T> valueParser) {
        this.defaultValue = defaultValue;
        this.valueParser = valueParser;
    }

    public static OptionParser<Boolean> bool() {
        return (arguments, option) -> values(arguments, option, 0).isPresent();
    }

    public static <T> OptionParser<T> createSingleValueOptionParser(T defaultValue, Function<String, T> valueParser) {
        return (arguments, option) -> values(arguments, option, 1).map(it -> parseValue(it.get(0), valueParser)).orElse(defaultValue);
    }

    @Override
    public T parse(List<String> arguments, Option option) {
        return values(arguments, option, 1).map(it -> parseValue(it.get(0), valueParser)).orElse(defaultValue);
    }

    private static Optional<List<String>> values(List<String> arguments, Option option, int expectedSize) {
        int index = arguments.indexOf("-" + option.value());
        //如果没有找到，就先赋值为空，到最后再处理
        if (index == -1) return Optional.empty();
        //这个返回的是下一个-l/-p/-d的索引，因为IntStream返回的就是Int值
        List<String> values = values(arguments, index);
        if (values.size() < expectedSize) throw new InsufficientException(option.value());
        if (values.size() > expectedSize) throw new TooManyArgumentsException(option.value());
        return Optional.of(values);
    }

    private static <T> T parseValue(String value, Function<String, T> valueParser) {
        return valueParser.apply(value);
    }

    private static List<String> values(List<String> arguments, int index) {
        int followingFlag = IntStream.range(index + 1, arguments.size())
                .filter(it -> arguments.get(it).startsWith("-"))
                .findFirst().orElse(arguments.size());
        //如果找到了刚开始的那个flag，然后找到接下来的那个flag，如果存在，比如size等于1，那么就是一个参数，是正常的
        //如果第一个标志后面紧跟着一个标志，那么就是返回的参数的个数，第一个flag的位置是0，index + 1就是1，如果中间没有参数，
        // 比如-p -l，并且起始位置是从Index + 1开始的，index是flag的位置，加1就是下一个的位置，
        // 那么.filter(it -> arguments.get(it).startsWith("-"))获取到的下一个的位置就是 -l的位置，所以following的位置就是1
        //这个就是从1到1的区间，那就是0，小于1，就是参数不够，如果是-p,那么找不到arguments.get(it).startsWith("-")，
        // 还是返回个数，1个，那还是一样的，所以，这个就是参数不够
        //如果是 -p 8080 8081 -d /usr/log,范围是从1-3,那么就是两个参数，那么就是多参数了
        return arguments.subList(index + 1, followingFlag);
    }

}
