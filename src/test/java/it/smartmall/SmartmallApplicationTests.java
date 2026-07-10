package it.smartmall;

import it.smartmall.config.SecurityConfig;
import it.smartmall.controller.AuthController;
import it.smartmall.controller.UserController;
import it.smartmall.model.Role;
import it.smartmall.model.User;
import it.smartmall.repository.RoleChangeRequestRepository;
import it.smartmall.repository.UserRepository;
import it.smartmall.security.JwtAuthenticationFilter;
import it.smartmall.security.JwtCookieService;
import it.smartmall.security.JwtUtil;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AuthController.class, UserController.class})
@Import({
		SecurityConfig.class,
		JwtAuthenticationFilter.class,
		JwtCookieService.class,
		JwtUtil.class
})
@TestPropertySource(properties = {
		"jwt.secret=01234567890123456789012345678901",
		"smartmall.security.jwt-cookie.secure=false",
		"smartmall.security.csrf-cookie.secure=false"
})
class SmartmallApplicationTests {

	private static final String LOGIN_JSON = """
			{
			  "email": "admin@test.com",
			  "password": "password123"
			}
			""";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AuthenticationManager authenticationManager;

	@MockitoBean
	private AuthenticationProvider authenticationProvider;

	@MockitoBean
	private UserDetailsService userDetailsService;

	@MockitoBean
	private UserRepository userRepository;

	@MockitoBean
	private RoleChangeRequestRepository roleChangeRequestRepository;

	@MockitoBean
	private PasswordEncoder passwordEncoder;

	private User adminUser;

	@BeforeEach
	void setUp() {
		adminUser = new User();
		adminUser.setId(1L);
		adminUser.setEmail("admin@test.com");
		adminUser.setPassword("encoded-password");
		adminUser.setRole(Role.SUPER_ADMIN);

		given(userDetailsService.loadUserByUsername("admin@test.com")).willReturn(adminUser);
		given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
				.willReturn(new UsernamePasswordAuthenticationToken(
						adminUser,
						null,
						adminUser.getAuthorities()
				));
	}

	@Test
	void contextLoads() {
	}

	@Test
	void protectedEndpointWithoutTokenIsNotAccessible() throws Exception {
		mockMvc.perform(get("/api/admin/users"))
				.andExpect(status().is4xxClientError());
	}

	@Test
	void protectedEndpointWithInvalidCookieTokenReturnsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/admin/users")
						.cookie(new Cookie(JwtCookieService.COOKIE_NAME, "invalid-token")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Token non valido"));
	}

	@Test
	void loginWithoutCsrfHeaderReturnsForbidden() throws Exception {
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(LOGIN_JSON))
				.andExpect(status().isForbidden());
	}

	@Test
	void loginSetsHttpOnlyJwtCookie() throws Exception {
		Cookie csrfCookie = getCsrfCookie();

		MvcResult result = mockMvc.perform(post("/api/auth/login")
						.cookie(csrfCookie)
						.header("X-XSRF-TOKEN", csrfCookie.getValue())
						.contentType(MediaType.APPLICATION_JSON)
						.content(LOGIN_JSON))
				.andExpect(status().isOk())
				.andReturn();

		Cookie jwtCookie = result.getResponse().getCookie(JwtCookieService.COOKIE_NAME);
		String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);

		assertThat(jwtCookie).isNotNull();
		assertThat(jwtCookie.isHttpOnly()).isTrue();
		assertThat(jwtCookie.getPath()).isEqualTo("/");
		assertThat(jwtCookie.getMaxAge()).isGreaterThan(0);
		assertThat(setCookie)
				.contains(JwtCookieService.COOKIE_NAME)
				.contains("SameSite=Lax");
		assertThat(result.getResponse().getContentAsString()).doesNotContain("token");
	}

	@Test
	void logoutClearsJwtCookie() throws Exception {
		Cookie csrfCookie = getCsrfCookie();
		Cookie jwtCookie = loginAndGetJwtCookie(csrfCookie);

		MvcResult result = mockMvc.perform(post("/api/auth/logout")
						.cookie(csrfCookie, jwtCookie)
						.header("X-XSRF-TOKEN", csrfCookie.getValue()))
				.andExpect(status().isOk())
				.andReturn();

		Cookie clearedCookie = result.getResponse().getCookie(JwtCookieService.COOKIE_NAME);

		assertThat(clearedCookie).isNotNull();
		assertThat(clearedCookie.getValue()).isEmpty();
		assertThat(clearedCookie.getMaxAge()).isZero();
		assertThat(clearedCookie.isHttpOnly()).isTrue();
	}

	@Test
	void protectedEndpointAuthenticatesFromJwtCookie() throws Exception {
		Cookie csrfCookie = getCsrfCookie();
		Cookie jwtCookie = loginAndGetJwtCookie(csrfCookie);

		mockMvc.perform(get("/api/me").cookie(jwtCookie))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("admin@test.com"))
				.andExpect(jsonPath("$.role").value("SUPER_ADMIN"));
	}

	@Test
	void viteOriginPreflightAllowsCredentialsAndCsrfHeader() throws Exception {
		mockMvc.perform(options("/api/auth/login")
						.header(HttpHeaders.ORIGIN, "http://localhost:5173")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type, Accept, X-XSRF-TOKEN"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
	}

	private Cookie getCsrfCookie() throws Exception {
		MvcResult csrfResult = mockMvc.perform(get("/api/auth/csrf"))
				.andExpect(status().isOk())
				.andReturn();

		Cookie csrfCookie = csrfResult.getResponse().getCookie("XSRF-TOKEN");

		assertThat(csrfCookie).isNotNull();
		assertThat(csrfCookie.isHttpOnly()).isFalse();
		assertThat(csrfCookie.getValue()).isNotBlank();
		return csrfCookie;
	}

	private Cookie loginAndGetJwtCookie(Cookie csrfCookie) throws Exception {
		MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
						.cookie(csrfCookie)
						.header("X-XSRF-TOKEN", csrfCookie.getValue())
						.contentType(MediaType.APPLICATION_JSON)
						.content(LOGIN_JSON))
				.andExpect(status().isOk())
				.andReturn();

		Cookie jwtCookie = loginResult.getResponse().getCookie(JwtCookieService.COOKIE_NAME);

		assertThat(jwtCookie).isNotNull();
		return jwtCookie;
	}
}
