package com.gstcompliance.util;

import org.springframework.stereotype.Component;

@Component
public class TaxRateCalculator {
    public double calculateTax(double amount, double rate) {
        return amount * rate / 100;
    }
}
