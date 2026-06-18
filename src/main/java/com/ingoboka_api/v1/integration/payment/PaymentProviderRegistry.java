package com.ingoboka_api.v1.integration.payment;

import com.ingoboka_api.v1.common.exception.BusinessException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class PaymentProviderRegistry {

    private final Map<String, PaymentProviderAdapter> adapters;

    public PaymentProviderRegistry(List<PaymentProviderAdapter> adapterList) {
        this.adapters = adapterList.stream()
                .collect(Collectors.toMap(PaymentProviderAdapter::providerCode, Function.identity()));
    }

    public PaymentProviderAdapter require(String providerCode) {
        PaymentProviderAdapter adapter = adapters.get(providerCode);
        if (adapter == null) {
            throw new BusinessException("Unsupported payment provider: " + providerCode);
        }
        return adapter;
    }
}
