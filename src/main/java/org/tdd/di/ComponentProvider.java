package org.tdd.di;

import java.util.List;

public interface ComponentProvider<Type> {
    Type getFrom(Container container);

    default List<Class<?>> getDependencies() {
        return List.of();
    }
}
