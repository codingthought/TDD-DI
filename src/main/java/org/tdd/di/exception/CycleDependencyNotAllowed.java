package org.tdd.di.exception;

import java.util.ArrayList;
import java.util.List;

public class CycleDependencyNotAllowed extends RuntimeException {

    List<Class<?>> components = new ArrayList<>();

    public <Type> CycleDependencyNotAllowed(Class<Type> component) {
        components.add(component);
    }

    public <Type> CycleDependencyNotAllowed(CycleDependencyNotAllowed e, Class<Type> component) {
        if (e.getComponents().contains(component)) {
            components = e.getComponents();
        } else {
            components.add(component);
            components.addAll(e.getComponents());
        }
    }

    public CycleDependencyNotAllowed(List<Class<?>> classes) {
        components = classes;
    }

    public List<Class<?>> getComponents() {
        return components;
    }
}
