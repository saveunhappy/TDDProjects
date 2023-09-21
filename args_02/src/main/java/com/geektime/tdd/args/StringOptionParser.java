package com.geektime.tdd.args;

class StringOptionParser extends IntOptionParser {

    public static OptionParser createStringOptionParser() {
        return new IntOptionParser(String::valueOf);
    }
}
