package it.smartmall.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

@Service
public class JwtCookieService {

    public static final String COOKIE_NAME = "SMARTMALL_TOKEN";

    @Value("${jwt.expiration}")
    private long jwtExpirationMillis;

    @Value("${smartmall.security.jwt-cookie.secure:false}")
    private boolean secure;

    @Value("${smartmall.security.jwt-cookie.same-site:Lax}")
    private String sameSite;

    public ResponseCookie createJwtCookie(String jwt) {
        return baseCookie(jwt)
                .maxAge(Duration.ofMillis(jwtExpirationMillis))
                .build();
    }

    public ResponseCookie clearJwtCookie() {
        return baseCookie("")
                .maxAge(Duration.ZERO)
                .build();
    }

    public Optional<String> extractToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }

        return Arrays.stream(cookies)
                .filter(cookie -> COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/");
    }
}
