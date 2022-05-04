package org.tdd.di.exception;

import java.util.List;

public class CycleDependencyNotAllowed extends RuntimeException {

    List<Class<?>> components;

    public CycleDependencyNotAllowed(List<Class<?>> classes) {
        components = classes;
    }

    public List<Class<?>> getComponents() {
        return components;
    }
}
