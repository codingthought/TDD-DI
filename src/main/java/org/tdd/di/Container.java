package org.tdd.di;

import java.lang.reflect.ParameterizedType;
import java.util.Map;
import java.util.Optional;

public class Container {
    private final Map<Class<?>, ComponentProvider<?>> componentProviders;

    public Container(Map<Class<?>, ComponentProvider<?>> componentProviders) {
        this.componentProviders = componentProviders;
    }

    public <Type> Optional<Type> get(Class<Type> type) {
        return Optional.ofNullable(componentProviders.get(type)).map(p -> (Type) p.getFrom(this));
    }

    public Optional<?> get(ParameterizedType parameterizedType) {
        return Optional.empty();
    }
}
