package com.meridiantrust.sentinel.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

/**
 * All AML rule thresholds live here, sourced from application.yml (or env
 * var overrides). This is the "configurable without redeployment" knob
 * required by the business rules - tune a threshold, restart the app
 * (or wire in a refresh endpoint later), no code change needed.
 */
@Configuration
@ConfigurationProperties(prefix = "aml")
@Getter
@Setter
public class AmlProperties {

    private Currency currency = new Currency();
    private Rules rules = new Rules();

    @Getter
    @Setter
    public static class Currency {
        private String base = "INR";
        private Map<String, Double> rates = Map.of("INR", 1.0);
    }

    @Getter
    @Setter
    public static class Rules {
        private LargeSingleTxn largeSingleTxn = new LargeSingleTxn();
        private Structuring structuring = new Structuring();
        private RapidMovement rapidMovement = new RapidMovement();
        private HighRiskJurisdiction highRiskJurisdiction = new HighRiskJurisdiction();
        private BehavioralDeviation behavioralDeviation = new BehavioralDeviation();
    }

    @Getter
    @Setter
    public static class LargeSingleTxn {
        private boolean enabled = true;
        private double thresholdBase = 10000;
        private int weight = 60;
    }

    @Getter
    @Setter
    public static class Structuring {
        private boolean enabled = true;
        private double minAmount = 9000;
        private double maxAmount = 9999;
        private int minCount = 3;
        private int windowHours = 24;
        private int weight = 60;
    }

    @Getter
    @Setter
    public static class RapidMovement {
        private boolean enabled = true;
        private double minPercentOut = 0.8;
        private int windowHours = 48;
        private int weight = 70;
    }

    @Getter
    @Setter
    public static class HighRiskJurisdiction {
        private boolean enabled = true;
        private int weight = 80;
        private List<String> jurisdictions = List.of();
    }

    @Getter
    @Setter
    public static class BehavioralDeviation {
        private boolean enabled = true;
        private double multiplier = 3.0;
        private int lookbackDays = 90;
        private int weight = 50;
    }
}
