package org.tdd.di;

import org.tdd.di.ContainerBuilder.Ref;

import java.util.List;

public interface ComponentProvider<T> {
    T getFrom(Container container);

    default List<Ref<?>> getDependencies() {
        return List.of();
    }
}
