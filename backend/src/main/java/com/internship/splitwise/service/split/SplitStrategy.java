package com.internship.splitwise.service.split;

import com.internship.splitwise.dto.ExpenseSplitRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface SplitStrategy {
    Map<UUID, BigDecimal> calculate(BigDecimal totalAmount, List<ExpenseSplitRequest> splits);
}
