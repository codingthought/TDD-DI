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
            if (ref.getContainer() != Provider.class)
                throw new UnsupportedTypeException(ref.getContainer());
            return (Optional<T>) Optional.ofNullable(componentProviders.get(ref.getComponent()))
                    .<Provider<?>>map(provider -> () -> (T) provider.getFrom(this));
        }
        return Optional.ofNullable(componentProviders.get(ref.getComponent())).map(provider -> (T) provider.getFrom(this));
    }
}
