package com.pfe.predictive.config;

import com.pfe.predictive.config.provider.JwtTokenProvider;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Security-chain coverage for JwtAuthenticationFilter: the single place
 * that turns a bearer token into a Spring Security Authentication. Bugs
 * here (e.g. mishandled role prefixes) silently grant/deny access across
 * every @PreAuthorize check in the app, so the role-cleaning logic gets
 * dedicated cases.
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtTokenProvider);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validTokenSetsAuthenticationWithRolePrefixedAuthorities() throws Exception {
        when(jwtTokenProvider.validateAndGetClaims(anyString())).thenReturn(null);
        when(jwtTokenProvider.getUsernameFromToken(anyString())).thenReturn("jane.tech");
        when(jwtTokenProvider.getRolesFromToken(anyString())).thenReturn(List.of("TECHNICIAN", "MANAGER"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid.token.here");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertEquals("jane.tech", auth.getPrincipal());
        List<String> authorities = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        assertTrue(authorities.contains("ROLE_TECHNICIAN"));
        assertTrue(authorities.contains("ROLE_MANAGER"));
        verify(chain).doFilter(request, response);
    }

    @Test
    void rolesAlreadyPrefixedAreNotDoublePrefixed() throws Exception {
        when(jwtTokenProvider.validateAndGetClaims(anyString())).thenReturn(null);
        when(jwtTokenProvider.getUsernameFromToken(anyString())).thenReturn("admin.bob");
        when(jwtTokenProvider.getRolesFromToken(anyString())).thenReturn(List.of("ROLE_ADMIN"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid.token.here");
        filter.doFilterInternal(request, new MockHttpServletResponse(), mock(FilterChain.class));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        List<String> authorities = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        assertEquals(List.of("ROLE_ADMIN"), authorities);
    }

    @Test
    void malformedRoleTokensAreCleanedBeforePrefixing() throws Exception {
        // Roles arriving as a stringified-list artifact, e.g. from an older
        // token format: '["ADMIN"]' split into a single noisy element.
        when(jwtTokenProvider.validateAndGetClaims(anyString())).thenReturn(null);
        when(jwtTokenProvider.getUsernameFromToken(anyString())).thenReturn("admin.bob");
        when(jwtTokenProvider.getRolesFromToken(anyString())).thenReturn(Arrays.asList("[\"ADMIN\"]", "  ", null));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid.token.here");
        filter.doFilterInternal(request, new MockHttpServletResponse(), mock(FilterChain.class));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        List<String> authorities = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        // blank/null entries are dropped, the bracket/quote noise is stripped
        assertEquals(List.of("ROLE_ADMIN"), authorities);
    }

    @Test
    void missingAuthorizationHeaderLeavesRequestUnauthenticatedButContinuesChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }

    @Test
    void nonBearerAuthorizationHeaderIsIgnored() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }

    @Test
    void invalidTokenClearsContextAndStillContinuesChain() throws Exception {
        when(jwtTokenProvider.validateAndGetClaims(anyString())).thenThrow(new JwtException("bad signature"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer garbage.token.value");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        // A rejected token must not block the request from reaching the rest
        // of the chain -- the downstream authorizeHttpRequests rule (or the
        // 401 entry point for protected routes) is what actually rejects it.
        verify(chain).doFilter(request, response);
    }
}
