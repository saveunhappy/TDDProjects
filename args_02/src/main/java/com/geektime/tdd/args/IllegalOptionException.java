package com.geektime.tdd.args;

public class IllegalOptionException  extends RuntimeException {

    String parameter;

    public IllegalOptionException(String parameter) {
        this.parameter = parameter;
    }

    public String getParameter() {
        return parameter;
    }
}
