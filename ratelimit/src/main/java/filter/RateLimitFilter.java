package filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import model.RateLimitResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import service.ClientIdentifierService;
import service.TockenBucketService;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;
//Her HTTP isteğini yakalayan Filter Class'ı

@Component
@Slf4j
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {
    private final TockenBucketService tockenService;
    private final ClientIdentifierService identifierService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String identifier = identifierService.extractIdentifier(request);
        RateLimitResult result = tockenService.tryRateLimit(path, identifier);

        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.getRemainingTokens()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(result.getRetryAfterSeconds()));


        if(result.isAllowed())
            filterChain.doFilter(request,response);
        else
            sendExceedResponse(response, result);

    }

    private void sendExceedResponse(
            HttpServletResponse response,
            RateLimitResult result) throws IOException {

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value()); // 429

        Map<String, Object> body = Map.of(
                "error", "Too Many Requests",
                "message", "Rate limit exceeded",
                "status", 429
                // rate limit hata kodu: 429
        );

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }


}
