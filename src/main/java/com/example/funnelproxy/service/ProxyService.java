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
        
        System.out.println("🔍 ProxyService: Looking for service matching path: " + path);
        
        return repo.findAll()
                .doOnNext(service -> System.out.println("📋 Found service: " + service.getName() + " -> " + service.getPathPrefix()))
                .filter(s -> {
                    boolean matches = path.startsWith(s.getPathPrefix());
                    System.out.println("🔍 Checking " + s.getPathPrefix() + " against " + path + " = " + matches);
                    return matches;
                })
                .sort((a, b) -> Integer.compare(b.getPathPrefix().length(), a.getPathPrefix().length())) // Longest prefix first
                .next()
                .switchIfEmpty(Mono.defer(() -> {
                    System.out.println("❌ No service found for path: " + path);
                    response.setStatusCode(org.springframework.http.HttpStatus.NOT_FOUND);
                    return response.setComplete().then(Mono.empty());
                }))
                .flatMap(mapping -> {
                    System.out.println("✅ Found matching service: " + mapping.getName() + " for path: " + path);
                    
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
                    
                    final String finalTargetUrl = targetUrl; // Make effectively final
                    System.out.println("🎯 Proxying " + path + " -> " + finalTargetUrl);
                    
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
                        System.out.println("🏠 Setting Host header to: " + mapping.getHost());
                    }
                    
                    // Make the proxied request
                    return webClient.method(request.getMethod())
                            .uri(finalTargetUrl)
                            .headers(h -> h.addAll(headers))
                            .body(request.getBody(), DataBuffer.class)
                            .exchangeToMono(clientResponse -> {
                                System.out.println("📡 Got response: " + clientResponse.statusCode() + " from " + finalTargetUrl);
                                
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
                                String errorMsg = error.getMessage();
                                System.err.println("❌ Proxy error for " + finalTargetUrl + ": " + errorMsg);
                                
                                // Provide helpful error messages
                                if (errorMsg.contains("Failed to resolve")) {
                                    System.err.println("💡 DNS Resolution failed. Try using IP address instead of hostname.");
                                    System.err.println("💡 Example: http://192.168.1.100:8123 instead of http://homeassistant:8123");
                                } else if (errorMsg.contains("Connection refused")) {
                                    System.err.println("💡 Connection refused. Check if the service is running and accessible.");
                                }
                                
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