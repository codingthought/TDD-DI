package org.tdd.di;

import jakarta.inject.Provider;
import org.tdd.di.ContainerBuilder.Ref;
import org.tdd.di.exception.UnsupportedTypeException;

import java.lang.reflect.ParameterizedType;
import java.util.Map;
import java.util.Optional;

public class Container {
    private final Map<Class<?>, ComponentProvider<?>> componentProviders;

    public Container(Map<Class<?>, ComponentProvider<?>> componentProviders) {
        this.componentProviders = componentProviders;
    }

    private  <T> Optional<T> getBy(Class<T> type) {
        return Optional.ofNullable(componentProviders.get(type)).map(provider -> (T) provider.getFrom(this));
    }

    private Optional<Provider<?>> getBy(ParameterizedType parameterizedType) {
        if (parameterizedType.getRawType() != Provider.class)
            throw new UnsupportedTypeException(parameterizedType.getRawType());
        Class<?> actualType = (Class<?>) parameterizedType.getActualTypeArguments()[0];
        return Optional.ofNullable(componentProviders.get(actualType))
                .map(provider -> () -> provider.getFrom(this));
    }

    public <T> Optional<T> get(Ref<T> ref) {
        if (ref.isContainer()) {
            return (Optional<T>) getBy(ref.getContainerType());
        }
        return getBy(ref.getComponentType());
    }
}
