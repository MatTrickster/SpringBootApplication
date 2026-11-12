package com.example.springboot.dto;

public record PortfolioSummary(Integer userId, long sharesBought, long sharesSold, long netShares) {
}
