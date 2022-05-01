package org.tdd.di;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class Container {
    private final Map<Class<?>, Supplier<?>> MAP;

    public Container(Map<Class<?>, Supplier<?>> map) {
        MAP = map;
    }

    public <Type> Optional<Type> get(Class<Type> type) {
        return Optional.ofNullable(MAP.get(type)).map(p -> (Type) p.get());
    }
}
