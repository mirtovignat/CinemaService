package com.example.demo.repository.projection;

import java.math.BigDecimal;

public interface CartSummaryProjection {
    Long getCount();

    BigDecimal getTotal();
}