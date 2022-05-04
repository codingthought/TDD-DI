package org.tdd.di;

import jakarta.inject.Provider;
import org.tdd.di.exception.UnsupportedTypeException;

import java.lang.reflect.ParameterizedType;
import java.util.Map;
import java.util.Optional;

public class Container {
    private final Map<Class<?>, ComponentProvider<?>> componentProviders;

    public Container(Map<Class<?>, ComponentProvider<?>> componentProviders) {
        this.componentProviders = componentProviders;
    }

    public <Type> Optional<Type> get(Class<Type> type) {
        return Optional.ofNullable(componentProviders.get(type)).map(provider -> (Type) provider.getFrom(this));
    }

    public Optional<Object> get(ParameterizedType parameterizedType) {
        if (parameterizedType.getRawType() != Provider.class) throw new UnsupportedTypeException();
        Class<?> actualType = (Class<?>) parameterizedType.getActualTypeArguments()[0];
        return Optional.of(componentProviders.get(actualType))
                .map(provider -> (Provider<?>) () -> provider.getFrom(this));
    }
}
