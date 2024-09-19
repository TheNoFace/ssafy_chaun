package com.ssafy.health.domain.coin;

import lombok.Getter;

@Getter
public enum CoinCost {
    CREATE_CREW(300),
    ;

    private final int amount;

    CoinCost(int amount) {
        this.amount = amount;
    }
}
