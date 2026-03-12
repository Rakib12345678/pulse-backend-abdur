package com.zendot.pulseLive.coin.dto;

import com.zendot.pulseLive.coin.model.CurrencyEnum;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateCoinRequest(
        @NotNull @Min(1) Integer noOfCoin,
        @NotNull BigDecimal price,
        @NotNull CurrencyEnum currency,
        @NotNull
        String bannerImage
) {}
