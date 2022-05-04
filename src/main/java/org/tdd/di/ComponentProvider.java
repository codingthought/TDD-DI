package org.tdd.di;

import java.lang.reflect.Type;
import java.util.List;

public interface ComponentProvider<T> {
    T getFrom(Container container);

    default List<Class<?>> getDependencies() {
        return List.of();
    }

    default List<Type> getTypeDependencies() {
        return List.of();
    }
}
