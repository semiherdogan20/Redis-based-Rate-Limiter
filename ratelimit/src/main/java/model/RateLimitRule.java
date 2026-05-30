package model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@AllArgsConstructor
@Builder
public class RateLimitRule {
    private int capacity;
    private double refillRatePerSecond;

    public double getRefillRatePerMilli() {
        return this.refillRatePerSecond / 1000.0;
    }

    public long calculateResetDurationMillis(long missingTokens) {
        double refillRatePerMilli = getRefillRatePerMilli();

        // Sıfıra bölünme hatasını önlemek için
        if (refillRatePerMilli <= 0) {
            return 0;
        }

        return (long) (missingTokens / refillRatePerMilli);
    }
}
