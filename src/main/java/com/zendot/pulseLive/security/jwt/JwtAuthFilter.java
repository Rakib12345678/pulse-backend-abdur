package com.zendot.pulseLive.security.jwt;

import com.zendot.pulseLive.security.UserPrincipal;
import com.zendot.pulseLive.user.repo.UserRepository;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthFilter implements Filter {

    private final JwtService jwtService;
    private final UserRepository userRepo;

    public JwtAuthFilter(JwtService jwtService, UserRepository userRepo) {
        this.jwtService = jwtService;
        this.userRepo = userRepo;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        String auth = req.getHeader("Authorization");

        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);

            try {
                Long userId = jwtService.parseUserId(token);

                userRepo.findById(userId).ifPresent(user -> {
                    var principal = new UserPrincipal(user.getId(), user.getEmail());

                    var authentication = new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            java.util.List.of()
                    );

                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(req)
                    );

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });

            } catch (Exception ignored) {
                // Invalid / expired token → no authentication set
            }
        }

        chain.doFilter(request, response);
    }
}
