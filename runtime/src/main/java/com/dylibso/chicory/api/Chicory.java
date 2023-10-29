package com.dylibso.chicory.api;

import static java.util.Objects.requireNonNull;

// TODO to be discussed: move this to "api" module
public final class Chicory {

    public static <T> T proxy(Class<T> iface) {
        return ChicoryProxyFactory.DYNAMIC.createProxy(requireNonNull(iface));
    }
}
