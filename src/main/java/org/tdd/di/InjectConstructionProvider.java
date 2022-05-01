package org.tdd.di;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

record InjectConstructionProvider<Type>(Constructor<Type> constructor) implements ComponentProvider<Type> {

    @Override
    public Type getFrom(Container container) {
        try {
            return constructor.newInstance(Arrays.stream(constructor.getParameterTypes())
                    .map(p -> container.get(p).get())
                    .toArray());
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Class<?>> getDependencies() {
        return Arrays.stream(constructor.getParameterTypes()).toList();
    }
}
