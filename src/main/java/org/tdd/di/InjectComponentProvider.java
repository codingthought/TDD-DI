package org.tdd.di;

import jakarta.inject.Inject;
import org.tdd.di.exception.IllegalComponentException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

class InjectComponentProvider<Type> implements ComponentProvider<Type> {
    private final Constructor<Type> constructor;
    private final List<Field> fields;

    InjectComponentProvider(Class<? extends Type> component) {
        Constructor<?>[] declaredConstructors = component.getDeclaredConstructors();
        List<Constructor<?>> filteredConstructors = Arrays.stream(declaredConstructors)
                .filter(c -> Objects.nonNull(c.getAnnotation(Inject.class))).toList();
        if (filteredConstructors.size() > 1) {
            throw new IllegalComponentException();
        }
        if (filteredConstructors.size() == 0 &&
                Arrays.stream(declaredConstructors).allMatch(d -> d.getParameterTypes().length > 0)) {
            throw new IllegalComponentException();
        }
        if (filteredConstructors.size() == 1) {
            constructor = (Constructor<Type>) filteredConstructors.get(0);
        } else {
            constructor = (Constructor<Type>) declaredConstructors[0];
        }
        fields = Arrays.stream(component.getDeclaredFields()).filter(field -> field.isAnnotationPresent(Inject.class)).toList();
    }

    @Override
    public Type getFrom(Container container) {
        try {
            Type instance = constructor.newInstance(Arrays.stream(constructor.getParameterTypes()).map(p -> container.get(p).get()).toArray());
            for (Field field : fields) {
                field.set(instance, container.get(field.getType()).get());
            }
            return instance;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Class<?>> getDependencies() {
        return Stream.concat(fields.stream().map(Field::getType), Arrays.stream(constructor.getParameterTypes())).toList();
    }
}
