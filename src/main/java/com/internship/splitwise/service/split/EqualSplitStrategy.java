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
public class EqualSplitStrategy implements SplitStrategy {

    @Override
    public Map<UUID, BigDecimal> calculate(BigDecimal totalAmount, List<ExpenseSplitRequest> splits) {
        Map<UUID, BigDecimal> calculated = new HashMap<>();
        if (splits == null || splits.isEmpty()) {
            return calculated;
        }

        int count = splits.size();
        BigDecimal baseShare = totalAmount.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
        
        BigDecimal accumulated = BigDecimal.ZERO;
        for (int i = 0; i < count; i++) {
            UUID userId = splits.get(i).getUserId();
            calculated.put(userId, baseShare);
            accumulated = accumulated.add(baseShare);
        }

        // Handle penny splits remainder: totalAmount - accumulated
        BigDecimal remainder = totalAmount.subtract(accumulated);
        if (remainder.compareTo(BigDecimal.ZERO) != 0) {
            UUID firstUserId = splits.get(0).getUserId();
            calculated.put(firstUserId, calculated.get(firstUserId).add(remainder));
        }

        return calculated;
    }
}
