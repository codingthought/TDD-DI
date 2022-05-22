package org.tdd.di;

import jakarta.inject.Provider;
import org.tdd.di.ContainerBuilder.Ref;
import org.tdd.di.exception.UnsupportedTypeException;

import java.util.Map;
import java.util.Optional;

public class Container {
    private final Map<Class<?>, ComponentProvider<?>> componentProviders;

    public Container(Map<Class<?>, ComponentProvider<?>> componentProviders) {
        this.componentProviders = componentProviders;
    }

    public <T> Optional<T> get(Ref<T> ref) {
        if (ref.isContainer()) {
            if (ref.getContainerType().getRawType() != Provider.class)
                throw new UnsupportedTypeException(ref.getContainerType().getRawType());
            return (Optional<T>) Optional.ofNullable(componentProviders.get(ref.getComponentType()))
                    .<Provider<?>>map(provider -> () -> (T) provider.getFrom(this));
        }
        return Optional.ofNullable(componentProviders.get(ref.getComponentType())).map(provider -> (T) provider.getFrom(this));
    }
}
