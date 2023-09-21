package com.geektime.tdd.args;

import java.util.List;
import java.util.function.Function;

class IntOptionParser implements OptionParser {
    Function<String, Object> valueParser = Integer::parseInt;

    private IntOptionParser() {

    }

    public IntOptionParser(Function<String, Object> valueParser) {
        this.valueParser = valueParser;
    }

    public static OptionParser createIntOptionParser() {
        return new IntOptionParser(Integer::parseInt);
    }

    @Override
    public Object parse(List<String> arguments, Option option) {
        int index = arguments.indexOf("-" + option.value());
        String value = arguments.get(index + 1);
        return valueParser.apply(value);
    }

}
