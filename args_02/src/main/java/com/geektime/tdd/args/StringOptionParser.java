package com.geektime.tdd.args;

class StringOptionParser extends IntOptionParser {

    private StringOptionParser() {
        super(String::valueOf);
    }

    public static StringOptionParser createStringOptionParser() {
        return new StringOptionParser();
    }
}
