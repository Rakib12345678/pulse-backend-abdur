package com.zendot.pulseLive.coin.service;

import com.zendot.pulseLive.coin.dto.CoinResponse;
import com.zendot.pulseLive.coin.dto.CreateCoinRequest;
import com.zendot.pulseLive.coin.dto.UpdateCoinRequest;
import com.zendot.pulseLive.coin.model.CurrencyEnum;

import java.util.List;

public interface CoinService {

    CoinResponse create(CreateCoinRequest request);

    List<CoinResponse> list(
            CurrencyEnum currency,
            int page,
            int size
    );

    CoinResponse getById(Long id);

    CoinResponse update(Long id, UpdateCoinRequest request);

    void delete(Long id);
}
