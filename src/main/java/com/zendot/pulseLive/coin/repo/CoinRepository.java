package com.zendot.pulseLive.coin.repo;

import com.zendot.pulseLive.coin.model.CoinModel;
import com.zendot.pulseLive.coin.model.CurrencyEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoinRepository extends JpaRepository<CoinModel, Long> {
    Page<CoinModel> findByCurrency(
            CurrencyEnum currency,
            Pageable pageable
    );
}
