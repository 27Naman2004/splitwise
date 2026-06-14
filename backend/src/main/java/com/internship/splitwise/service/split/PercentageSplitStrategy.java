package com.internship.splitwise.service.split;

import com.internship.splitwise.dto.ExpenseSplitRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class PercentageSplitStrategy implements SplitStrategy {

    @Override
    public Map<UUID, BigDecimal> calculate(BigDecimal totalAmount, List<ExpenseSplitRequest> splits) {
        Map<UUID, BigDecimal> calculated = new HashMap<>();
        if (splits == null || splits.isEmpty()) {
            return calculated;
        }

        // Validate percentage sum equals 100%
        BigDecimal sumPct = BigDecimal.ZERO;
        for (ExpenseSplitRequest s : splits) {
            if (s.getSplitValue() == null) {
                throw new IllegalArgumentException("Split percentage value cannot be null");
            }
            sumPct = sumPct.add(s.getSplitValue());
        }

        if (sumPct.setScale(2, RoundingMode.HALF_UP).compareTo(BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_UP)) != 0) {
            throw new IllegalArgumentException("Percentages must sum to exactly 100%. Sum = " + sumPct);
        }

        BigDecimal accumulated = BigDecimal.ZERO;
        int count = splits.size();
        for (int i = 0; i < count; i++) {
            ExpenseSplitRequest s = splits.get(i);
            BigDecimal share = totalAmount.multiply(s.getSplitValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            calculated.put(s.getUserId(), share);
            accumulated = accumulated.add(share);
        }

        // Adjust penny remainder
        BigDecimal remainder = totalAmount.subtract(accumulated);
        if (remainder.compareTo(BigDecimal.ZERO) != 0) {
            UUID firstUserId = splits.get(0).getUserId();
            calculated.put(firstUserId, calculated.get(firstUserId).add(remainder));
        }

        return calculated;
    }
}
