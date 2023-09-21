package com.geektime.tdd.args;

public class TooManyArgumentsException extends RuntimeException{

    String option;

    public TooManyArgumentsException(String option) {
        this.option = option;
    }

    public String getOption() {
        return option;
    }
}
