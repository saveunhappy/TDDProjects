package com.geektime.tdd.args;

public class InsufficientException extends RuntimeException {

    String option;

    public InsufficientException(String option) {
        this.option = option;
    }

    public String getOption() {
        return option;
    }
}
