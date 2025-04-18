package com.dylibso.chicory.observable.runtime;

import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;

public class OtelAutoconfiguredSDK {

    private OtelAutoconfiguredSDK() {}

    public static OpenTelemetrySdk autoconfiguredSdk() {
        return AutoConfiguredOpenTelemetrySdk.initialize().getOpenTelemetrySdk();
    }
}
