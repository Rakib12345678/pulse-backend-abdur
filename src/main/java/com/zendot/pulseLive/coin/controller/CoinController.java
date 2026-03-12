package com.zendot.pulseLive.coin.controller;

import com.zendot.pulseLive.coin.dto.CoinResponse;
import com.zendot.pulseLive.coin.dto.CreateCoinRequest;
import com.zendot.pulseLive.coin.dto.UpdateCoinRequest;
import com.zendot.pulseLive.coin.model.CurrencyEnum;
import com.zendot.pulseLive.coin.service.CoinService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coins")
public class CoinController {

    private final CoinService coinService;

    public CoinController(CoinService coinService) {
        this.coinService = coinService;
    }

    @PostMapping
    @Operation(summary = "Create coin entity", description = "Create a new coin entity with coin count, price, currency, and  banner image URL.")
    public CoinResponse create(@Valid @RequestBody CreateCoinRequest request) {
        return coinService.create(request);
    }

    @GetMapping
    @Operation(summary = "List coin entity", description = "List coin entity with optional currency filter and offset pagination (page/size).")
    public List<CoinResponse> list(
            @RequestParam(required = false) CurrencyEnum currency,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return coinService.list(currency, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get coin entity by id", description = "Fetch a single coin entity by id.")
    public CoinResponse getById(@PathVariable Long id) {
        return coinService.getById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update coin entity", description = "Update an existing coin entity by id.")
    public CoinResponse update(@PathVariable Long id, @Valid @RequestBody UpdateCoinRequest request) {
        return coinService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete coin entity", description = "Delete a coin entity by id.")
    public void delete(@PathVariable Long id) {
        coinService.delete(id);
    }
}
