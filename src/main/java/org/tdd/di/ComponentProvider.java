package org.tdd.di;

import java.lang.reflect.Type;
import java.util.List;

public interface ComponentProvider<T> {
    T getFrom(Container container);

    default List<Type> getDependencies() {
        return List.of();
    }
}
