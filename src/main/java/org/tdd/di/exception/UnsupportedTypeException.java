package org.tdd.di.exception;

import java.lang.reflect.Type;

public class UnsupportedTypeException extends RuntimeException {
    public UnsupportedTypeException(Type type) {
        super(type.getTypeName());
    }
}
