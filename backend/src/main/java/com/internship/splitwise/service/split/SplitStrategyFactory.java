package com.internship.splitwise.service.split;

import com.internship.splitwise.model.SplitType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
public class SplitStrategyFactory {

    private final Map<SplitType, SplitStrategy> strategies = new EnumMap<>(SplitType.class);

    public SplitStrategyFactory(
            EqualSplitStrategy equalStrategy,
            PercentageSplitStrategy percentageStrategy,
            SharesSplitStrategy sharesStrategy,
            UnequalSplitStrategy unequalStrategy) {
        
        strategies.put(SplitType.EQUAL, equalStrategy);
        strategies.put(SplitType.PERCENTAGE, percentageStrategy);
        strategies.put(SplitType.SHARE, sharesStrategy);
        strategies.put(SplitType.UNEQUAL, unequalStrategy);
    }

    public SplitStrategy getStrategy(SplitType splitType) {
        SplitStrategy strategy = strategies.get(splitType);
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported split type: " + splitType);
        }
        return strategy;
    }
}
