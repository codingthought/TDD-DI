package org.tdd.di;

import jakarta.inject.Inject;
import org.tdd.di.exception.FinalFieldInjectException;
import org.tdd.di.exception.IllegalComponentException;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class InjectComponentProvider<Type> implements ComponentProvider<Type> {
    private final Constructor<Type> constructor;
    private final List<Field> fields;
    private final List<Method> methods;

    InjectComponentProvider(Class<? extends Type> component) {
        if (Modifier.isAbstract(component.getModifiers())) {
            throw new IllegalComponentException();
        }
        constructor = (Constructor<Type>) getConstructor(component);
        fields = getFields(component);
        methods = getMethods(component);
    }

    @Override
    public Type getFrom(Container container) {
        try {
            Type instance = constructor.newInstance(toDependencies(constructor, container));
            for (Field field : fields)
                field.set(instance, toDependency(field, container));
            for (Method method : methods)
                method.invoke(instance, toDependencies(method, container));
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

    private static Constructor<?> getConstructor(Class<?> component) {
        List<Constructor<?>> injectableConstructors = injectable(component.getDeclaredConstructors());
        if (injectableConstructors.size() > 1) throw new IllegalComponentException();
        return injectableConstructors.stream().findFirst().orElseGet(() -> getDefaultConstructor(component));
    }

    private static Constructor<?> getDefaultConstructor(Class<?> component) {
        try {
            return component.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalComponentException();
        }
    }

    private static <T extends AnnotatedElement> List<T> injectable(T[] elements) {
        return Arrays.stream(elements).filter(e -> e.isAnnotationPresent(Inject.class)).toList();
    }

    private static List<Field> getFields(Class<?> component) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = component;
        while (current != Object.class) {
            for (Field field : injectable(current.getDeclaredFields())) {
                validate(current, field);
                fields.add(field);
            }
            current = current.getSuperclass();
        }
        return fields;
    }

    private static void validate(Class<?> current, Field field) {
        if (Modifier.isFinal(field.getModifiers()))
            throw new FinalFieldInjectException(field.getName(), current);
    }

    private static List<Method> getMethods(Class<?> component) {
        List<Method> methods = injectable(component.getMethods()).stream()
                .peek(InjectComponentProvider::validate).collect(Collectors.toList());
        Collections.reverse(methods);
        return methods;
    }

    private static void validate(Method method) {
        if (method.getTypeParameters().length > 0) {
            throw new IllegalComponentException();
        }
    }

    private static Object[] toDependencies(Executable executable, Container container) {
        return Arrays.stream(executable.getParameters()).map(Parameter::getParameterizedType).map(type -> {
            if (type instanceof ParameterizedType) {
                return container.get((ParameterizedType) type).get();
            }
            return container.get((Class<?>) type).get();
        }).toArray();
    }

    private Object toDependency(Field field, Container container) {
        if (field.getGenericType() instanceof ParameterizedType) {
            return container.get((ParameterizedType) field.getGenericType()).get();
        }
        return container.get((Class<?>) field.getGenericType()).get();
    }
}
