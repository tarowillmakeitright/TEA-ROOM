package com.ecommerce.ecommerce;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TrendingDebateScheduler {
    private static final Logger log = LoggerFactory.getLogger(TrendingDebateScheduler.class);

    private final TrendingDebateService service;

    public TrendingDebateScheduler(TrendingDebateService service) {
        this.service = service;
    }

    @Scheduled(cron = "0 0 */4 * * *", zone = "Asia/Tokyo")
    public void runEveryFourHours() {
        log.info("Running scheduled Coffeehouse news generation");
        service.runOnceNow();
    }
}
