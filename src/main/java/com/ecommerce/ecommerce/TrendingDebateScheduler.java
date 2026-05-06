package com.ecommerce.ecommerce;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TrendingDebateScheduler {
    private final TrendingDebateService service;

    public TrendingDebateScheduler(TrendingDebateService service) {
        this.service = service;
    }

    @Scheduled(cron = "0 0 */4 * * *", zone = "Asia/Tokyo")
    public void runEveryFourHours() {
        service.runOnceNow();
    }
}
