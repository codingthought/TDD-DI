package org.tdd.di.exception;

public class FinalFieldInjectException extends RuntimeException {
    private final String fieldName;
    private final Class<?> component;

    public FinalFieldInjectException(String fieldName, Class<?> component) {
        this.fieldName = fieldName;
        this.component = component;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Class<?> getComponent() {
        return component;
    }
}
