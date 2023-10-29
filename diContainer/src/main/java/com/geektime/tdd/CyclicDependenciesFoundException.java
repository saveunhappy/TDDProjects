package com.geektime.tdd;

public class CyclicDependenciesFoundException extends RuntimeException{
    public Class<?>[] getComponents() {
        return new Class[0];
    }
}
