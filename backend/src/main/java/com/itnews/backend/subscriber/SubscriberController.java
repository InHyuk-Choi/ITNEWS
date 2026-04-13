package com.itnews.backend.subscriber;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@RestController
public class SubscriberController {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final SubscriberRepository subscriberRepository;

    public SubscriberController(SubscriberRepository subscriberRepository) {
        this.subscriberRepository = subscriberRepository;
    }

    @PostMapping("/api/subscribe")
    @Transactional
    public ResponseEntity<Map<String, String>> subscribe(@RequestBody Map<String, String> body) {
        String email = body.getOrDefault("email", "").trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return ResponseEntity.badRequest().body(Map.of("message", "유효하지 않은 이메일입니다."));
        }
        if (subscriberRepository.existsByEmail(email)) {
            return ResponseEntity.ok(Map.of("message", "이미 구독 중입니다."));
        }
        subscriberRepository.save(new SubscriberEntity(email));
        return ResponseEntity.ok(Map.of("message", "구독이 완료됐습니다!"));
    }

    @GetMapping("/api/unsubscribe")
    @Transactional
    public ResponseEntity<Map<String, String>> unsubscribe(@RequestParam String token) {
        Optional<SubscriberEntity> subscriber = subscriberRepository.findByUnsubscribeToken(token);
        if (subscriber.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "유효하지 않은 토큰입니다."));
        }
        subscriber.get().setActive(false);
        return ResponseEntity.ok(Map.of("message", "수신거부가 완료됐습니다."));
    }
}
