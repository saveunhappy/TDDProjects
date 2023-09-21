package com.geektime.tdd.args;

import java.util.List;

class BooleanOptionParser implements OptionParser {
    @Override
    public Object parse(List<String> arguments, Option option) {
        int index = arguments.indexOf("-" + option.value());
        if (!arguments.get(index + 1).startsWith("-")) throw new TooManyArgumentsException(option.value());
        return index != -1;
    }
}
