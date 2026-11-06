package com.budget.backend.security.security;

import com.budget.backend.security.entity.Utilisateur;
import com.budget.backend.security.repository.UtilisateurRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class AuthTokenFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private TokenRevocationService tokenRevocationService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = parseJwt(request);
            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
                Claims claims = jwtUtils.getClaims(jwt);
                if (tokenRevocationService.estRevoque(claims.getId())) {
                    filterChain.doFilter(request, response);
                    return;
                }
                String username = claims.getSubject();
                Utilisateur utilisateur = utilisateurRepository.findByUsername(username).orElse(null);
                if (utilisateur == null || emisAvantChangementMotDePasse(claims, utilisateur)) {
                    filterChain.doFilter(request, response);
                    return;
                }
                if (Boolean.TRUE.equals(utilisateur.getDoitChangerMotDePasse()) && !accessibleAvantChangement(request)) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write("{\"status\":403,\"code\":\"MOT_DE_PASSE_A_CHANGER\","
                            + "\"message\":\"Vous devez changer votre mot de passe temporaire avant de continuer.\"}");
                    return;
                }

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            logger.warn("Impossible de définir l'authentification utilisateur : " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /** Jeton émis avant le dernier changement de mot de passe : session fermée. */
    private static boolean emisAvantChangementMotDePasse(Claims claims, Utilisateur utilisateur) {
        Instant modifieLe = utilisateur.getMotDePasseModifieLe();
        return modifieLe != null && claims.getIssuedAt() != null
                && claims.getIssuedAt().toInstant().isBefore(modifieLe.truncatedTo(ChronoUnit.SECONDS));
    }

    private static boolean accessibleAvantChangement(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.startsWith("/api/profil") || path.startsWith("/api/auth/");
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }

        return null;
    }
}
