package org.tdd.di;

public interface ComponentProvider<Type> {
    Type getFrom(Container container);
}
