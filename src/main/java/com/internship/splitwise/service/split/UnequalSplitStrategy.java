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
public class UnequalSplitStrategy implements SplitStrategy {

    @Override
    public Map<UUID, BigDecimal> calculate(BigDecimal totalAmount, List<ExpenseSplitRequest> splits) {
        Map<UUID, BigDecimal> calculated = new HashMap<>();
        if (splits == null || splits.isEmpty()) {
            return calculated;
        }

        BigDecimal sumSplits = BigDecimal.ZERO;
        for (ExpenseSplitRequest s : splits) {
            if (s.getSplitValue() == null || s.getSplitValue().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Unequal split amounts must be non-negative");
            }
            sumSplits = sumSplits.add(s.getSplitValue());
        }

        BigDecimal roundedTotal = totalAmount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal roundedSum = sumSplits.setScale(2, RoundingMode.HALF_UP);

        if (roundedTotal.compareTo(roundedSum) != 0) {
            throw new IllegalArgumentException(
                    "The sum of unequal splits must equal the total amount exactly. " +
                    "Expected: " + roundedTotal + ", Actual: " + roundedSum
            );
        }

        for (ExpenseSplitRequest s : splits) {
            calculated.put(s.getUserId(), s.getSplitValue().setScale(2, RoundingMode.HALF_UP));
        }

        return calculated;
    }
}
