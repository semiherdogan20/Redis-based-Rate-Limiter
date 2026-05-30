package service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

/**
 * IP adresini doğru şekilde çıkarır.
 *
 * Neden bu kadar karmaşık? Çünkü uygulama çoğunlukla bir reverse proxy
 * (Nginx, AWS ALB) arkasında çalışır. Bu durumda request.getRemoteAddr()
 * proxy'nin IP'sini döner, kullanıcının değil.
 *
 * X-Forwarded-For header'ı gerçek IP'yi taşır.
 * Birden fazla proxy varsa: "client, proxy1, proxy2" formatında gelir.
 */
@Service
public class ClientIdentifierService {

    private static final String[] IP_HEADER_CANDIDATES = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "REMOTE_ADDR"
    };

    public String extractIdentifier(HttpServletRequest request) {
        // auth varsa onu kullan
        if(request.getUserPrincipal().getName() != null){
            return "user:" + request.getUserPrincipal().getName();
        }

        String apiKey = request.getHeader("X-API-Key");
        // auth yoksa endpointi kullan
        if(!apiKey.isEmpty()){
            return "api:" + apiKey;
        }

        //hiçbiri yoksa ID kullan
        return "ip:" + extractUserIP(request);
    }


    private String extractUserIP(HttpServletRequest request){
        for(String header : IP_HEADER_CANDIDATES){
            String userIP = request.getHeader(header);
            if(!userIP.isEmpty() && userIP != null){
                return userIP;
            }
        }
        return request.getRemoteAddr();

    }

}