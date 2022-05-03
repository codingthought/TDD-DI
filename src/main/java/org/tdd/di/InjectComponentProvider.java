package org.tdd.di;

import jakarta.inject.Inject;
import org.tdd.di.exception.FinalFieldInjectException;
import org.tdd.di.exception.IllegalComponentException;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Stream;

class InjectComponentProvider<Type> implements ComponentProvider<Type> {
    private final Constructor<Type> constructor;
    private final List<Field> fields;
    private final List<Method> methods;

    InjectComponentProvider(Class<? extends Type> component) {
        if (Modifier.isAbstract(component.getModifiers())) {
            throw new IllegalComponentException();
        }
        constructor = getConstructor(component);
        fields = getFields(component);
        methods = getMethods(component);
    }

    @Override
    public Type getFrom(Container container) {
        try {
            Type instance = constructor.newInstance(Arrays.stream(constructor.getParameterTypes()).map(p -> container.get(p).get()).toArray());
            for (Field field : fields) {
                field.set(instance, container.get(field.getType()).get());
            }
            for (Method method : methods) {
                method.invoke(instance, Arrays.stream(method.getParameterTypes()).map(p -> container.get(p).get()).toArray());
            }
            return instance;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Class<?>> getDependencies() {
        return Stream.concat(Stream.concat(fields.stream().map(Field::getType), Arrays.stream(constructor.getParameterTypes())),
                methods.stream().map(Method::getParameterTypes).flatMap(Arrays::stream)).toList();
    }

    private static <Type> Constructor<Type> getConstructor(Class<? extends Type> component) {
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
            return (Constructor<Type>) filteredConstructors.get(0);
        } else {
            return (Constructor<Type>) declaredConstructors[0];
        }
    }

    private static List<Field> getFields(Class<?> component) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = component;
        while (current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (field.isAnnotationPresent(Inject.class)) {
                    if (Modifier.isFinal(field.getModifiers()))
                        throw new FinalFieldInjectException(field.getName(), current);
                    fields.add(field);
                }
            }
            current = current.getSuperclass();
        }
        return fields;
    }

    private static List<Method> getMethods(Class<?> component) {
        List<Method> methods = new ArrayList<>();
        for (Method method : component.getMethods()) {
            if (method.isAnnotationPresent(Inject.class)) {
                if (method.getTypeParameters().length > 0) {
                    throw new IllegalComponentException();
                }
                methods.add(method);
            }
        }
        Collections.reverse(methods);
        return methods;
    }
}
