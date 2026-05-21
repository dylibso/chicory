package com.dylibso.chicory.component.types;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Represents a variant (tagged union) WIT type.
 */
public class VariantType implements WitType {
    private final String variantName;
    private final List<Case> cases;

    public VariantType(String variantName) {
        this.variantName = variantName;
        this.cases = new ArrayList<>();
    }

    @Override
    public String displayName() {
        return variantName;
    }

    public void addCase(String caseName, Optional<WitType> caseType) {
        cases.add(new Case(caseName, caseType));
    }

    public List<Case> cases() {
        return Collections.unmodifiableList(cases);
    }

    public Case caseByName(String name) {
        return cases.stream()
                .filter(c -> c.name.equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Case not found: " + name));
    }

    public static class Case {
        public final String name;
        public final Optional<WitType> type;

        public Case(String name, Optional<WitType> type) {
            this.name = name;
            this.type = type;
        }
    }
}
