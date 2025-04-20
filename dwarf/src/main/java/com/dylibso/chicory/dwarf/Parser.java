package com.dylibso.chicory.dwarf;

import java.io.InputStream;
import java.util.function.Function;

public interface Parser extends Function<InputStream, DebugInfo> {}
