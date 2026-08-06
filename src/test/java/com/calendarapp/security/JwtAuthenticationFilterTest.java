package com.calendarapp.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.calendarapp.configuration.JwtProperties;
import com.calendarapp.entity.User;
import com.calendarapp.repository.UserRepository;

import jakarta.servlet.FilterChain;

// Unit tests for JwtAuthenticationFilter: checks what does (and does not) end up in
// the SecurityContext for a variety of Authorization headers. Uses a real
// JwtService/JwtProperties (like JwtServiceTest) so tokens are genuinely
// signed/parsed, and a mocked UserRepository to control who "exists" in the DB.
class JwtAuthenticationFilterTest {

    private static final String SECRET =
            "f4a1c9e27b6d3081f5a9c4e6b2d7108f3c5a8e1b4d6f9c2a7e0b3d5f8a1c4e6b";
    private static final long ONE_HOUR_MS = 3_600_000L;

    private JwtService jwtService;
    private UserRepository userRepository;
    private JwtAuthenticationFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(new JwtProperties(SECRET, ONE_HOUR_MS));
        userRepository = mock(UserRepository.class);
        filter = new JwtAuthenticationFilter(jwtService, userRepository);
        filterChain = mock(FilterChain.class);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static User existingUser() {
        User user = new User();
        user.setId(1L);
        user.setFirstName("Jane");
        user.setLastName("Doe");
        user.setEmail("jane@example.com");
        user.setPasswordHash("hashed-password");
        return user;
    }

    @Test
    void noHeaderMeansNotLoggedIn() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(userRepository);
    }

    @Test
    void wrongHeaderFormatMeansNotLoggedIn() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "NotBearer sometoken");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(userRepository);
    }

    @Test
    void goodTokenLogsUserIn() throws Exception {
        User user = existingUser();
        String token = jwtService.generateToken(user.getEmail(), user.getId());
        given(userRepository.findByEmail(user.getEmail())).willReturn(Optional.of(user));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getPrincipal()).isSameAs(user);
        assertThat(authentication.getCredentials()).isNull();
        assertThat(authentication.getAuthorities()).isEmpty();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void expiredTokenDoesNotLogUserIn() throws Exception {
        JwtService shortLivedJwtService = new JwtService(new JwtProperties(SECRET, 1));
        String token = shortLivedJwtService.generateToken("jane@example.com", 1L);
        Thread.sleep(25); // let the 1ms expiration pass

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void messedUpTokenDoesNotLogUserIn() throws Exception {
        String token = jwtService.generateToken("jane@example.com", 1L);
        String tamperedToken = token.substring(0, token.length() - 2)
                + (token.endsWith("A") ? "B" : "A") + "A";

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + tamperedToken);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void deletedUserCantLogIn() throws Exception {
        String token = jwtService.generateToken("jane@example.com", 1L);
        given(userRepository.findByEmail("jane@example.com")).willReturn(Optional.empty());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}
