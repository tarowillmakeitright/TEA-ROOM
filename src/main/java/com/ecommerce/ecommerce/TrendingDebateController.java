package com.ecommerce.ecommerce;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tea-room/debate")
public class TrendingDebateController {

    private final TrendingDebateService service;

    public TrendingDebateController(TrendingDebateService service) {
        this.service = service;
    }

    @PostMapping("/run-now")
    public ResponseEntity<Map<String, Object>> runNow() {
        List<String> ids = service.runOnceNow();
        return ResponseEntity.ok(Map.of("ok", true, "createdPosts", ids, "count", ids.size()));
    }
}
