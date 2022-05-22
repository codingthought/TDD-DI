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
        Optional<? extends ComponentProvider<?>> providerOptional = Optional.ofNullable(componentProviders.get(ref.getComponent()));
        if (ref.isContainer()) {
            if (ref.getContainer() != Provider.class)
                throw new UnsupportedTypeException(ref.getContainer());
            return providerOptional.map(provider -> (T) (Provider<?>) () -> provider.getFrom(this));
        }
        return providerOptional.map(provider -> (T) provider.getFrom(this));
    }

}
