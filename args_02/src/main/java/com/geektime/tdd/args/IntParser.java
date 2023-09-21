package com.geektime.tdd.args;

import java.util.List;

class IntParser implements OptionParser {
    @Override
    public Object parse(List<String> arguments, Option option) {
        int index = arguments.indexOf("-" + option.value());
        String value = arguments.get(index + 1);
        return parseValue(value);
    }

    private static Integer parseValue(String value) {
        return Integer.valueOf(value);
    }
}
