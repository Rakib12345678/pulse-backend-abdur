package com.zendot.pulseLive.security.jwt;

import com.zendot.pulseLive.security.UserPrincipal;
import com.zendot.pulseLive.user.model.UserStatus;
import com.zendot.pulseLive.user.repo.UserRepository;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
        var res = (HttpServletResponse) response;

        String auth = req.getHeader("Authorization");

        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);

            try {
                Long userId = jwtService.parseUserId(token);
                var user = userRepo.findById(userId).orElse(null);

                if (user != null) {

                    // ❌ HARD BLOCK DELETED USERS
                    if (user.getStatus() == UserStatus.DELETED) {

                        res.setStatus(403);
                        res.getWriter().write("ACCOUNT_DELETED");
                        return; // STOP FILTER
                    }

                    // ✅ set authentication
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

                    // ❌ BLOCK API ACCESS FOR DEACTIVATED USERS
                    if (user.getStatus() ==
                            UserStatus.DEACTIVATED) {

                        String path = req.getRequestURI();
                        String method = req.getMethod();

                        boolean allowed =
                                path.equals("/api/users/me/reactivate") ||
                                        (path.equals("/api/users/me") && method.equals("DELETE"));

                        if (!allowed) {
                            res.setStatus(403);
                            res.getWriter().write("ACCOUNT_DEACTIVATED");
                            return; // STOP FILTER
                        }
                    }
                }

            } catch (Exception ignored) {
                // Invalid / expired token → no authentication set
            }
        }

        chain.doFilter(request, response);
    }

}
