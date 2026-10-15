package com.projectpandora.api.security;

import com.projectpandora.api.user.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtService.parse(token);
                Long userId = Long.valueOf(claims.getSubject());
                userRepository
                        .findById(userId)
                        .ifPresent(
                                user -> {
                                    if (user.isDisabled()) {
                                        SecurityContextHolder.clearContext();
                                        return;
                                    }
                                    UserPrincipal principal =
                                            new UserPrincipal(
                                                    user.getId(),
                                                    user.getUsername(),
                                                    user.getRole(),
                                                    user.getManagerId());
                                    var auth =
                                            new UsernamePasswordAuthenticationToken(
                                                    principal, null, principal.getAuthorities());
                                    SecurityContextHolder.getContext().setAuthentication(auth);
                                });
            } catch (Exception ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
