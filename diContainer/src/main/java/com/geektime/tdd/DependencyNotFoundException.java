package com.geektime.tdd;

public class DependencyNotFoundException extends RuntimeException{
    private Class<?> component;
    private Class<?> dependency;

    public DependencyNotFoundException(Component componentComponent, Component dependencyComponent) {
        this.componentComponent = componentComponent;
        this.dependencyComponent = dependencyComponent;
    }

    private Component componentComponent;
    private Component dependencyComponent;

    public Class<?> getDependency() {
        return dependencyComponent.type();
    }

    public Class<?> getComponent() {
        return componentComponent.type();
    }

    public Component getDependencyComponent() {
        return dependencyComponent;
    }

    public Component getComponentComponent() {
        return componentComponent;
    }
}
