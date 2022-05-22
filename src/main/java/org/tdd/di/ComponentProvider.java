package org.tdd.di;

import org.tdd.di.ContainerBuilder.Ref;

import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Collectors;

public interface ComponentProvider<T> {
    T getFrom(Container container);

    default List<Type> getDependencies() {
        return getRefDependencies().stream().map(Ref::getType).collect(Collectors.toList());
    }
    default List<Ref<?>> getRefDependencies() {
        return List.of();
    }
}
