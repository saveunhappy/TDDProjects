package com.geektime.tdd;

import jakarta.inject.Provider;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class GenericExample {

    static Type getParameterizedType() {
        TypeLiteral<Provider<Integer>> collection = new TypeLiteral<>(){};
        Type type = collection.getClass().getGenericSuperclass();
        return type;
    }

    public static void main(String[] args) {
        Type type = getParameterizedType();

        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;

//            System.out.println("Raw Type: " + parameterizedType.getRawType());

//            Type[] typeArguments = parameterizedType.getActualTypeArguments();
//            for (Type argument : typeArguments) {
//                System.out.println("Type Argument: " + argument);
//            }
            ParameterizedType actualTypeArgument = (ParameterizedType) parameterizedType.getActualTypeArguments()[0];
            System.out.println("Raw Type: " + actualTypeArgument.getRawType());
            System.out.println("Type Argument: " + actualTypeArgument.getActualTypeArguments()[0]);

        }
    }

    static class TypeLiteral<T> {
        public ParameterizedType getType() {
            return (ParameterizedType) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        }
    }
}
