package it.smartmall.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private JwtCookieService jwtCookieService;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new JwtAuthenticationFilter(jwtUtil, userDetailsService, jwtCookieService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void invalidJwtReturnsUnauthorizedWithoutContinuingFilterChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/users");
        MockHttpServletResponse response = new MockHttpServletResponse();

        given(jwtCookieService.extractToken(request)).willReturn(Optional.of("invalid-token"));
        given(jwtUtil.extractUsername("invalid-token"))
                .willThrow(new MalformedJwtException("invalid token details"));

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString())
                .contains("Unauthorized")
                .contains("Token non valido")
                .doesNotContain("invalid token details");
        verify(filterChain, never()).doFilter(any(ServletRequest.class), any(ServletResponse.class));
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void expiredJwtReturnsUnauthorizedWithoutContinuingFilterChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/bookings/my");
        MockHttpServletResponse response = new MockHttpServletResponse();

        given(jwtCookieService.extractToken(request)).willReturn(Optional.of("expired-token"));
        given(jwtUtil.extractUsername("expired-token"))
                .willThrow(new ExpiredJwtException(Jwts.header(), Jwts.claims(), "expired token details"));

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString())
                .contains("Unauthorized")
                .contains("Token non valido")
                .doesNotContain("expired token details");
        verify(filterChain, never()).doFilter(any(ServletRequest.class), any(ServletResponse.class));
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void authorizationHeaderWithoutJwtCookieIsIgnored() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/users");
        request.addHeader("Authorization", "Bearer ignored-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        given(jwtCookieService.extractToken(request)).willReturn(Optional.empty());

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(any(ServletRequest.class), any(ServletResponse.class));
        verifyNoInteractions(jwtUtil, userDetailsService);
    }
}
