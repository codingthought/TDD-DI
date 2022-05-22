package org.tdd.di;

import jakarta.inject.Inject;
import org.tdd.di.ContainerBuilder.Ref;
import org.tdd.di.exception.FinalFieldInjectException;
import org.tdd.di.exception.IllegalComponentException;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class InjectComponentProvider<T> implements ComponentProvider<T> {
    private final Constructor<T> constructor;
    private final List<Field> fields;
    private final List<Method> methods;

    InjectComponentProvider(Class<? extends T> component) {
        if (Modifier.isAbstract(component.getModifiers())) {
            throw new IllegalComponentException();
        }
        constructor = (Constructor<T>) getConstructor(component);
        fields = getFields(component);
        methods = getMethods(component);
    }

    @Override
    public T getFrom(Container container) {
        try {
            T instance = constructor.newInstance(toDependencies(constructor, container));
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
    public List<Ref<?>> getDependencies() {
        return Stream.concat(Stream.concat(fields.stream().map(Field::getGenericType),
                                Arrays.stream(constructor.getParameters()).map(Parameter::getParameterizedType)),
                        methods.stream().map(Method::getParameters).flatMap(Arrays::stream).map(Parameter::getParameterizedType))
                .map(Ref::of).collect(Collectors.toList());
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
        return Arrays.stream(executable.getParameters())
                .map(Parameter::getParameterizedType)
                .map(type -> container.get(Ref.of(type)))
                .map(Optional::get).toArray();
    }

    private Object toDependency(Field field, Container container) {
        return container.get(Ref.of(field.getGenericType())).get();
    }
}
