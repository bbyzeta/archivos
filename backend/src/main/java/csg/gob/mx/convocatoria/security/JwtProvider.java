package csg.gob.mx.convocatoria.security;

import java.util.Date;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtProvider {

    private final SecretKey secretKey;
    private final long expirationTime;

    public JwtProvider(
            @Value("${security.jwt.secret-key}") String secretKeyRaw,
            @Value("${security.jwt.expiration}") long expirationTime) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKeyRaw));
        this.expirationTime = expirationTime;
    }

    public String generateToken(UserDetails userDetails) {
        var roles = userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claims(Map.of("roles", roles))
                .subject(userDetails.getUsername())
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationTime))
                .signWith(secretKey)
                .compact();
    }

    /** Devuelve el usuario del token o null si es inválido/expirado. */
    public String extractUsernameOrNull(String token) {
        try {
            return extractClaim(token, Claims::getSubject);
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsernameOrNull(token);
        return username != null && username.equals(userDetails.getUsername());
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
        return resolver.apply(claims);
    }
}
