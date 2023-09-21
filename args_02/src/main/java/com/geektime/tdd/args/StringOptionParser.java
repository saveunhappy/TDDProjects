package com.geektime.tdd.args;

import java.util.List;

class StringOptionParser extends IntOptionParser {

    protected Object parseValue(String value) {
        return String.valueOf(value);
    }
}
