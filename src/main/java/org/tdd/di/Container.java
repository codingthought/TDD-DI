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

    public <T> Optional<T> get(Class<T> type) {
        return Optional.ofNullable(componentProviders.get(type)).map(provider -> (T) provider.getFrom(this));
    }

    public Optional<Provider<?>> get(ParameterizedType parameterizedType) {
        if (parameterizedType.getRawType() != Provider.class) throw new UnsupportedTypeException(parameterizedType.getRawType());
        Class<?> actualType = (Class<?>) parameterizedType.getActualTypeArguments()[0];
        return Optional.ofNullable(componentProviders.get(actualType))
                .map(provider -> () -> provider.getFrom(this));
    }
}
