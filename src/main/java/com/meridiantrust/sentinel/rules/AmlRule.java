package com.meridiantrust.sentinel.rules;

/**
 * Strategy interface: every AML typology is one stateless, independently
 * unit-testable implementation of this. Add a new typology by adding a new
 * {@code @Component} - DetectionEngineService picks it up automatically via
 * Spring's collection-of-beans injection.
 */
public interface AmlRule {
    RuleResult evaluate(RuleContext context);
}
