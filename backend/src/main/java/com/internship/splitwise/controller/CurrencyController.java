package com.internship.splitwise.controller;

import com.internship.splitwise.dto.ExchangeRateRequest;
import com.internship.splitwise.model.ExchangeRate;
import com.internship.splitwise.repository.ExchangeRateRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/currencies")
@RequiredArgsConstructor
public class CurrencyController {

    private final ExchangeRateRepository exchangeRateRepository;

    @GetMapping("/rates")
    public ResponseEntity<List<ExchangeRate>> getAllRates() {
        List<ExchangeRate> rates = exchangeRateRepository.findAll();
        return ResponseEntity.ok(rates);
    }

    @PostMapping("/rates")
    public ResponseEntity<ExchangeRate> createOrUpdateRate(@Valid @RequestBody ExchangeRateRequest request) {
        String fromCur = request.getFromCurrency().toUpperCase().trim();
        String toCur = request.getToCurrency().toUpperCase().trim();

        Optional<ExchangeRate> existing = exchangeRateRepository.findByFromCurrencyAndToCurrency(fromCur, toCur);
        ExchangeRate rate;
        if (existing.isPresent()) {
            rate = existing.get();
            rate.setRate(request.getRate());
        } else {
            rate = ExchangeRate.builder()
                    .fromCurrency(fromCur)
                    .toCurrency(toCur)
                    .rate(request.getRate())
                    .build();
        }

        rate = exchangeRateRepository.save(rate);
        return ResponseEntity.ok(rate);
    }
}
