package com.geektime.tdd.args.exception;

public class InsufficientArgumentException extends RuntimeException {
    private final String option;

    public InsufficientArgumentException(String option) {
        super(option);
        this.option = option;
    }

    public String getOption() {
        return option;
    }
}