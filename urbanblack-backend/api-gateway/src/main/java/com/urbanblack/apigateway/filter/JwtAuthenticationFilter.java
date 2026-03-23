package com.urbanblack.apigateway.filter;

import com.urbanblack.apigateway.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    // Paths that start with these prefixes are allowed without a JWT token
    private static final String[] PUBLIC_PREFIXES = {
            "/auth/",
            "/auth",
            "/ws",
            "/employees",
            "/otp",
            "/notifications",
            "/eureka",
            "/actuator",
            "/v3/api-docs",
            "/swagger-ui",
            "/webjars",
            "/auth-service/v3/api-docs",
            "/user-service/v3/api-docs",
            "/driver-service/v3/api-docs",
            "/employee-details-service/v3/api-docs",
            "/traffic-operations-service/v3/api-docs",
            "/cab-registration-service/v3/api-docs",
            "/quickkyc-service/v3/api-docs",
            "/notification-service/v3/api-docs",
            "/admin/driver/attendance"
    };

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        String path = request.getURI().getPath();

        // ── Handle OPTIONS pre-flight immediately (no auth needed) ────────
        if (HttpMethod.OPTIONS.equals(request.getMethod())) {
            response.setStatusCode(HttpStatus.OK);
            return response.setComplete();
        }

        // ── Always allow WebSocket handshake and SockJS probe endpoints ───
        if (path.startsWith("/ws")) {
            return chain.filter(exchange);
        }

        // ── Check public paths (no JWT required) ─────────────────────────
        for (String prefix : PUBLIC_PREFIXES) {
            if (path.equals(prefix) || path.startsWith(prefix + "/") || path.startsWith(prefix + "?")) {
                return chain.filter(exchange);
            }
        }

        // ── All other paths: validate JWT ─────────────────────────────────
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }

        String token = authHeader.substring(7);

        // ── Handle Skip Mode for Development ────────
        if ("skip_mode_token".equals(token)) {
            ServerHttpRequest.Builder builder = request.mutate()
                    .header("X-Gateway-Secret", "my-gateway-secret")
                    .header("X-User-Email", "dev@urbanblack.com")
                    .header("X-Role", "ADMIN")
                    .header("X-User-Id", "1");
            return chain.filter(exchange.mutate().request(builder.build()).build());
        }

        try {
            jwtUtil.validateToken(token);
            String username = jwtUtil.extractUsername(token);
            String role = jwtUtil.extractRole(token);
            Long userId = jwtUtil.extractUserId(token);

            ServerHttpRequest.Builder builder = request.mutate()
                    .header("X-Gateway-Secret", "my-gateway-secret");

            if (username != null && !username.equalsIgnoreCase("null")) {
                builder.header("X-User-Email", username);
            }
            if (role != null && !role.equalsIgnoreCase("null")) {
                builder.header("X-Role", role);
            }
            if (userId != null) {
                builder.header("X-User-Id", String.valueOf(userId));
            }

            return chain.filter(exchange.mutate().request(builder.build()).build());
        } catch (Exception e) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
