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
    private final NewsletterService newsletterService;

    public SubscriberController(
            SubscriberRepository subscriberRepository,
            NewsletterService newsletterService
    ) {
        this.subscriberRepository = subscriberRepository;
        this.newsletterService = newsletterService;
    }

    @PostMapping("/api/subscribe")
    @Transactional
    public ResponseEntity<Map<String, String>> subscribe(@RequestBody Map<String, String> body) {
        String email = body.getOrDefault("email", "").trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return ResponseEntity.badRequest().body(Map.of("message", "유효하지 않은 이메일입니다."));
        }

        Optional<SubscriberEntity> existing = subscriberRepository.findByEmail(email);
        if (existing.isPresent()) {
            SubscriberEntity sub = existing.get();
            if (sub.isVerified() && sub.isActive()) {
                return ResponseEntity.ok(Map.of("message", "이미 구독 중입니다."));
            }
            // 미인증/해지 상태면 인증 메일 재발송
            sub.setActive(true);
            newsletterService.sendVerificationEmail(sub.getEmail(), sub.getUnsubscribeToken());
            return ResponseEntity.ok(Map.of("message", "인증 메일을 재발송했습니다. 메일함을 확인해주세요."));
        }

        SubscriberEntity sub = subscriberRepository.save(new SubscriberEntity(email));
        newsletterService.sendVerificationEmail(sub.getEmail(), sub.getUnsubscribeToken());
        return ResponseEntity.ok(Map.of("message", "인증 메일을 보냈습니다. 메일함을 확인해주세요."));
    }

    @GetMapping("/api/subscribe/verify")
    @Transactional
    public ResponseEntity<Map<String, Object>> verify(@RequestParam String token) {
        Optional<SubscriberEntity> subscriber = subscriberRepository.findByUnsubscribeToken(token);
        if (subscriber.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "유효하지 않은 토큰입니다."
            ));
        }
        SubscriberEntity sub = subscriber.get();
        if (sub.isVerified()) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "이미 인증된 이메일입니다."
            ));
        }
        sub.setVerified(true);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "구독이 완료됐습니다!"
        ));
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
