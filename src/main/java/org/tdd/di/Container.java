package org.tdd.di;

import java.util.Map;
import java.util.Optional;

public class Container {
    private final Map<Class<?>, ComponentProvider<?>> MAP;

    public Container(Map<Class<?>, ComponentProvider<?>> map) {
        MAP = map;
    }

    public <Type> Optional<Type> get(Class<Type> type) {
        return Optional.ofNullable(MAP.get(type)).map(p -> (Type) p.getFrom(this));
    }
}
