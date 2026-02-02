package com.example.funnelproxy.service;

import com.example.funnelproxy.repository.ServiceMappingRepo;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class ProxyService {
    private final ServiceMappingRepo repo;
    private final WebClient webClient;
    
    public ProxyService(ServiceMappingRepo repo) {
        this.repo = repo;
        this.webClient = WebClient.builder().build();
    }
    
    public Mono<Void> proxy(ServerHttpRequest request, ServerHttpResponse response) {
        String path = request.getPath().value();
        
        // Skip admin paths
        if (path.startsWith("/admin") || path.startsWith("/h2-console")) {
            return Mono.error(new RuntimeException("Admin path not proxied"));
        }
        
        return repo.findAll()
                .filter(s -> path.startsWith(s.getPathPrefix()))
                .next()
                .switchIfEmpty(Mono.defer(() -> {
                    response.setStatusCode(org.springframework.http.HttpStatus.NOT_FOUND);
                    return response.setComplete().then(Mono.empty());
                }))
                .flatMap(mapping -> {
                    // Rewrite path: remove the prefix and ensure it starts with /
                    String newPath = path.replaceFirst("^" + mapping.getPathPrefix(), "");
                    if (!newPath.startsWith("/")) {
                        newPath = "/" + newPath;
                    }
                    
                    // Build target URL
                    String targetUrl = mapping.getTargetUrl() + newPath;
                    if (request.getURI().getQuery() != null) {
                        targetUrl += "?" + request.getURI().getQuery();
                    }
                    
                    // Create headers for the proxied request
                    HttpHeaders headers = new HttpHeaders();
                    request.getHeaders().forEach((key, values) -> {
                        // Skip hop-by-hop headers
                        if (!isHopByHopHeader(key)) {
                            headers.addAll(key, values);
                        }
                    });
                    
                    // Set the Host header to the target host
                    if (mapping.getHost() != null && !mapping.getHost().isEmpty()) {
                        headers.set("Host", mapping.getHost());
                    }
                    
                    // Make the proxied request
                    return webClient.method(request.getMethod())
                            .uri(targetUrl)
                            .headers(h -> h.addAll(headers))
                            .body(request.getBody(), DataBuffer.class)
                            .exchangeToMono(clientResponse -> {
                                // Copy response status
                                response.setStatusCode(clientResponse.statusCode());
                                
                                // Copy response headers
                                clientResponse.headers().asHttpHeaders().forEach((key, values) -> {
                                    if (!isHopByHopHeader(key)) {
                                        response.getHeaders().addAll(key, values);
                                    }
                                });
                                
                                // Stream the response body
                                return response.writeWith(clientResponse.bodyToFlux(DataBuffer.class));
                            })
                            .onErrorResume(error -> {
                                response.setStatusCode(org.springframework.http.HttpStatus.BAD_GATEWAY);
                                return response.setComplete();
                            });
                });
    }
    
    private boolean isHopByHopHeader(String headerName) {
        String lowerName = headerName.toLowerCase();
        return lowerName.equals("connection") ||
               lowerName.equals("keep-alive") ||
               lowerName.equals("proxy-authenticate") ||
               lowerName.equals("proxy-authorization") ||
               lowerName.equals("te") ||
               lowerName.equals("trailers") ||
               lowerName.equals("transfer-encoding") ||
               lowerName.equals("upgrade");
    }
}