package controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import service.ClientIdentifierService;
import service.TockenBucketService;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ApiController {

    private final TockenBucketService tokenBucketService;
    private final ClientIdentifierService identifierService;


    @GetMapping("/api/public/hello")
    public ResponseEntity<Map<String, String>> hello() {
        return ResponseEntity.ok(Map.of(
                "message", "Dakikada 30 isteğe izin var.",
                "status", "success"
        ));
    }

    @PostMapping("/api/auth/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> body) {
        // Gerçek login logic burada olurdu
        return ResponseEntity.ok(Map.of(
                "token", "demo-jwt-token",
                "message", "Login başarılı"
        ));
    }


    @GetMapping("/api/admin/rate-limit/status")
    public ResponseEntity<Map<String, Object>> getStatus(
            HttpServletRequest request,
            @RequestParam(defaultValue = "/api/public/hello") String endpoint) {

        String identifier = identifierService.extractIdentifier(request);
        long remaining = tokenBucketService.getRemainingTokens(identifier, endpoint);

        return ResponseEntity.ok(Map.of(
                "identifier", identifier,
                "endpoint", endpoint,
                "remainingTokens", remaining
        ));
    }

    @DeleteMapping("/api/admin/rate-limit/reset")
    public ResponseEntity<Map<String, String>> reset(
            HttpServletRequest request,
            @RequestParam(defaultValue = "/api/public/hello") String endpoint) {

        String identifier = identifierService.extractIdentifier(request);
        tokenBucketService.reset(identifier, endpoint);

        return ResponseEntity.ok(Map.of(
                "message", "Rate limit sıfırlandı",
                "identifier", identifier,
                "endpoint", endpoint
        ));
    }
}
