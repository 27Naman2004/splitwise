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
public class SharesSplitStrategy implements SplitStrategy {

    @Override
    public Map<UUID, BigDecimal> calculate(BigDecimal totalAmount, List<ExpenseSplitRequest> splits) {
        Map<UUID, BigDecimal> calculated = new HashMap<>();
        if (splits == null || splits.isEmpty()) {
            return calculated;
        }

        BigDecimal totalShares = BigDecimal.ZERO;
        for (ExpenseSplitRequest s : splits) {
            if (s.getSplitValue() == null || s.getSplitValue().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Share values must be non-negative");
            }
            totalShares = totalShares.add(s.getSplitValue());
        }

        if (totalShares.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Total shares cannot be zero");
        }

        BigDecimal accumulated = BigDecimal.ZERO;
        int count = splits.size();
        for (int i = 0; i < count; i++) {
            ExpenseSplitRequest s = splits.get(i);
            BigDecimal share = totalAmount.multiply(s.getSplitValue())
                    .divide(totalShares, 2, RoundingMode.HALF_UP);
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
