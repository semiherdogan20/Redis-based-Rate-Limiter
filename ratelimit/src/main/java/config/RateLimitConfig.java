package config;


import model.RateLimitRule;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RateLimitConfig {

    private final RateLimitRule defaultRule;
    private final Map<String, RateLimitRule> endpointRules = new HashMap<>();

    public RateLimitConfig() {
        // default kural
        this.defaultRule = new RateLimitRule(60, 1.0);

        // endpointlere özel kurallar
        this.endpointRules.put("/login", new RateLimitRule(5, 0.2));  // 5 saniyede 1 jeton
        this.endpointRules.put("/hello", new RateLimitRule(20, 5.0)); // Saniyede 5 jeton
    }

    // gelen adrese uygun kuralı döner
    public RateLimitRule getRuleForPath(String requestPath) {
        return endpointRules.getOrDefault(requestPath, defaultRule);
    }
}