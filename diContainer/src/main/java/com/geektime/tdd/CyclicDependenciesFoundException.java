package com.geektime.tdd;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

public class CyclicDependenciesFoundException extends RuntimeException{
    private Set<Component> components = new HashSet<>();

    public CyclicDependenciesFoundException(List<Component> visiting) {
        components.addAll(visiting);
    }

    public Class<?>[] getComponents() {
        return components.stream().map(c->c.type()).toArray(Class[]::new);
        //为什么这里还是返回一个Class，不返回Component，因为返回Component的话，就不存在循环依赖了，都有了Qualifier，就
        //不是循环依赖了，这个是给之前的没有Qualifier使用的，
//        return components;
    }
}
