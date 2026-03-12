package com.zendot.pulseLive.coin.dto;

import com.zendot.pulseLive.coin.model.CurrencyEnum;
import java.math.BigDecimal;

public record CoinResponse(
        Long id,
        Integer noOfCoin,
        BigDecimal price,
        CurrencyEnum currency,
        String bannerImage
) {}
