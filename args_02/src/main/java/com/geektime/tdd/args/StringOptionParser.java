package com.geektime.tdd.args;

import java.util.List;

class StringOptionParser extends IntOptionParser {

    public StringOptionParser() {
        super(String::valueOf);
    }

//    protected Object parseValue(String value) {
//        return String.valueOf(value);
//    }
}
