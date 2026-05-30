package model;

import lombok.*;

@Data
@Getter
@Setter
@AllArgsConstructor
public class RateLimitResult {
    private boolean allowed;
    private long remainingTokens;
    private long resetAfterMillis;

    public static RateLimitResult allow(long remainingTokens, long resetAfterMillis) {
        return new RateLimitResult(true, remainingTokens, resetAfterMillis);
    }

    public static RateLimitResult deny(long resetAfterMillis) {
        return new RateLimitResult(false, 0, resetAfterMillis);
    }


    public long getRetryAfterSeconds() {
        return (long) Math.ceil(this.resetAfterMillis / 1000.0);
    }

    public boolean isRateLimited() {
        return !this.allowed;
    }

}
