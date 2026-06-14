package com.internship.splitwise.service.split;

import com.internship.splitwise.dto.ExpenseSplitRequest;
import com.internship.splitwise.model.SplitType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class SplitStrategyTest {

    private EqualSplitStrategy equalStrategy;
    private PercentageSplitStrategy percentageStrategy;
    private SharesSplitStrategy sharesStrategy;
    private UnequalSplitStrategy unequalStrategy;
    private SplitStrategyFactory factory;

    private UUID user1;
    private UUID user2;
    private UUID user3;

    @BeforeEach
    public void setUp() {
        equalStrategy = new EqualSplitStrategy();
        percentageStrategy = new PercentageSplitStrategy();
        sharesStrategy = new SharesSplitStrategy();
        unequalStrategy = new UnequalSplitStrategy();
        
        factory = new SplitStrategyFactory(
                equalStrategy,
                percentageStrategy,
                sharesStrategy,
                unequalStrategy
        );

        user1 = UUID.randomUUID();
        user2 = UUID.randomUUID();
        user3 = UUID.randomUUID();
    }

    @Test
    public void testEqualSplitWithRoundingRemainder() {
        // Splitting 10.00 among 3 users
        ExpenseSplitRequest req1 = new ExpenseSplitRequest();
        req1.setUserId(user1);
        
        ExpenseSplitRequest req2 = new ExpenseSplitRequest();
        req2.setUserId(user2);

        ExpenseSplitRequest req3 = new ExpenseSplitRequest();
        req3.setUserId(user3);

        List<ExpenseSplitRequest> splits = Arrays.asList(req1, req2, req3);
        Map<UUID, BigDecimal> result = equalStrategy.calculate(BigDecimal.valueOf(10.00), splits);

        // Expect 10.00 / 3 = 3.33 with a remaining 0.01 added to the first user
        assertEquals(3, result.size());
        assertEquals(BigDecimal.valueOf(3.34), result.get(user1));
        assertEquals(BigDecimal.valueOf(3.33), result.get(user2));
        assertEquals(BigDecimal.valueOf(3.33), result.get(user3));

        // Verify total matches
        BigDecimal total = result.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(BigDecimal.valueOf(10.00).setScale(2, RoundingMode.HALF_UP), total);
    }

    @Test
    public void testPercentageSplitValidationAndMath() {
        // Splitting 1000.00 by 50%, 30%, 20%
        ExpenseSplitRequest req1 = new ExpenseSplitRequest();
        req1.setUserId(user1);
        req1.setSplitValue(BigDecimal.valueOf(50.0));

        ExpenseSplitRequest req2 = new ExpenseSplitRequest();
        req2.setUserId(user2);
        req2.setSplitValue(BigDecimal.valueOf(30.0));

        ExpenseSplitRequest req3 = new ExpenseSplitRequest();
        req3.setUserId(user3);
        req3.setSplitValue(BigDecimal.valueOf(20.0));

        List<ExpenseSplitRequest> splits = Arrays.asList(req1, req2, req3);
        Map<UUID, BigDecimal> result = percentageStrategy.calculate(BigDecimal.valueOf(1000.00), splits);

        assertEquals(BigDecimal.valueOf(500.00).setScale(2), result.get(user1));
        assertEquals(BigDecimal.valueOf(300.00).setScale(2), result.get(user2));
        assertEquals(BigDecimal.valueOf(200.00).setScale(2), result.get(user3));

        // Test validation failure: sum != 100%
        req3.setSplitValue(BigDecimal.valueOf(25.0)); // Total 105%
        assertThrows(IllegalArgumentException.class, () -> {
            percentageStrategy.calculate(BigDecimal.valueOf(1000.00), splits);
        });
    }

    @Test
    public void testSharesSplitProportionalMath() {
        // Splitting 600.00 with shares 3, 2, 1 (total shares = 6)
        ExpenseSplitRequest req1 = new ExpenseSplitRequest();
        req1.setUserId(user1);
        req1.setSplitValue(BigDecimal.valueOf(3));

        ExpenseSplitRequest req2 = new ExpenseSplitRequest();
        req2.setUserId(user2);
        req2.setSplitValue(BigDecimal.valueOf(2));

        ExpenseSplitRequest req3 = new ExpenseSplitRequest();
        req3.setUserId(user3);
        req3.setSplitValue(BigDecimal.valueOf(1));

        List<ExpenseSplitRequest> splits = Arrays.asList(req1, req2, req3);
        Map<UUID, BigDecimal> result = sharesStrategy.calculate(BigDecimal.valueOf(600.00), splits);

        assertEquals(BigDecimal.valueOf(300.00).setScale(2), result.get(user1));
        assertEquals(BigDecimal.valueOf(200.00).setScale(2), result.get(user2));
        assertEquals(BigDecimal.valueOf(100.00).setScale(2), result.get(user3));
    }

    @Test
    public void testUnequalSplitSumValidation() {
        // Splitting 500.00 with absolute amounts 250.00, 150.00, 100.00 (sum = 500)
        ExpenseSplitRequest req1 = new ExpenseSplitRequest();
        req1.setUserId(user1);
        req1.setSplitValue(BigDecimal.valueOf(250.00));

        ExpenseSplitRequest req2 = new ExpenseSplitRequest();
        req2.setUserId(user2);
        req2.setSplitValue(BigDecimal.valueOf(150.00));

        ExpenseSplitRequest req3 = new ExpenseSplitRequest();
        req3.setUserId(user3);
        req3.setSplitValue(BigDecimal.valueOf(100.00));

        List<ExpenseSplitRequest> splits = Arrays.asList(req1, req2, req3);
        Map<UUID, BigDecimal> result = unequalStrategy.calculate(BigDecimal.valueOf(500.00), splits);

        assertEquals(BigDecimal.valueOf(250.00).setScale(2), result.get(user1));
        assertEquals(BigDecimal.valueOf(150.00).setScale(2), result.get(user2));
        assertEquals(BigDecimal.valueOf(100.00).setScale(2), result.get(user3));

        // Test validation failure: sum != total
        req3.setSplitValue(BigDecimal.valueOf(120.00)); // Sum = 520
        assertThrows(IllegalArgumentException.class, () -> {
            unequalStrategy.calculate(BigDecimal.valueOf(500.00), splits);
        });
    }

    @Test
    public void testFactoryLookup() {
        assertEquals(equalStrategy, factory.getStrategy(SplitType.EQUAL));
        assertEquals(percentageStrategy, factory.getStrategy(SplitType.PERCENTAGE));
        assertEquals(sharesStrategy, factory.getStrategy(SplitType.SHARE));
        assertEquals(unequalStrategy, factory.getStrategy(SplitType.UNEQUAL));
    }
}
