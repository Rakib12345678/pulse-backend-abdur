package com.zendot.pulseLive.coin.service.impl;

import com.zendot.pulseLive.coin.dto.CoinResponse;
import com.zendot.pulseLive.coin.dto.CreateCoinRequest;
import com.zendot.pulseLive.coin.dto.UpdateCoinRequest;
import com.zendot.pulseLive.coin.model.CoinModel;
import com.zendot.pulseLive.coin.model.CurrencyEnum;
import com.zendot.pulseLive.coin.repo.CoinRepository;
import com.zendot.pulseLive.coin.service.CoinService;
import com.zendot.pulseLive.common.exception.AppException;
import com.zendot.pulseLive.media.util.RelativePathUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CoinServiceImpl implements CoinService {

    private final CoinRepository coinRepository;
    private final RelativePathUtils relativePathUtils;

    public CoinServiceImpl(CoinRepository coinRepository, RelativePathUtils relativePathUtils) {
        this.coinRepository = coinRepository;
        this.relativePathUtils = relativePathUtils;
    }

    @Override
    public CoinResponse create(CreateCoinRequest request) {
        CoinModel model = new CoinModel();
        apply(model, request.noOfCoin(), request.price(), request.currency(),
                toKey(request.bannerImage()));

        CoinModel saved = coinRepository.save(model);
        return toResponse(saved);
    }

    @Override
    public List<CoinResponse> list(CurrencyEnum currency, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        Page<CoinModel> pageResult = currency == null
                ? coinRepository.findAll(pageable)
                : coinRepository.findByCurrency(currency, pageable);

        return pageResult.getContent().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public CoinResponse getById(Long id) {
        CoinModel model = coinRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Coin package not found"));
        return toResponse(model);
    }

    @Override
    public CoinResponse update(Long id, UpdateCoinRequest request) {
        CoinModel model = coinRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Coin package not found"));

        apply(model, request.noOfCoin(), request.price(), request.currency(),
                toKey(request.bannerImage()));

        CoinModel saved = coinRepository.save(model);
        return toResponse(saved);
    }

    @Override
    public void delete(Long id) {
        if (!coinRepository.existsById(id)) {
            throw new AppException(HttpStatus.NOT_FOUND, "Coin package not found");
        }
        coinRepository.deleteById(id);
    }

    private void apply(CoinModel model, Integer noOfCoin, BigDecimal price,
                       CurrencyEnum currency, String bannerImagePath) {
        model.setNoOfCoin(noOfCoin);
        model.setPrice(price);
        model.setCurrency(currency);
        model.setBannerImagePath(bannerImagePath);
    }

    private CoinResponse toResponse(CoinModel model) {
        return new CoinResponse(
                model.getId(),
                model.getNoOfCoin(),
                model.getPrice(),
                model.getCurrency(),
                toCdnUrl(model.getBannerImagePath())
        );
    }

    private String toKey(String input) {
        if (input == null || input.isBlank()) return null;
        return relativePathUtils.extractKey(input);
    }

    private String toCdnUrl(String key) {
        return relativePathUtils.buildCdnUrl(key);
    }
}
