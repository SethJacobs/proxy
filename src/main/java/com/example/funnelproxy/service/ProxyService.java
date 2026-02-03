package com.example.funnelproxy.service;

import com.example.funnelproxy.model.ServiceMapping;
import com.example.funnelproxy.repository.ServiceMappingRepo;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

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
        String referer = request.getHeaders().getFirst("Referer");
        
        System.out.println("🔍 ProxyService: Looking for service matching path: " + path);
        System.out.println("🔍 Referer header: " + referer);
        
        return repo.findAll()
                .collectList()
                .flatMap(services -> {
                    // First, try exact prefix matching
                    ServiceMapping exactMatch = services.stream()
                            .filter(s -> path.startsWith(s.getPathPrefix()))
                            .max((a, b) -> Integer.compare(a.getPathPrefix().length(), b.getPathPrefix().length()))
                            .orElse(null);
                    
                    // If no exact match, check if this might be a root-level request from a proxied app
                    ServiceMapping contextMatch = null;
                    if (exactMatch == null) {
                        // First, try referer-based matching
                        if (referer != null) {
                            contextMatch = services.stream()
                                    .filter(s -> referer.contains(s.getPathPrefix()))
                                    .findFirst()
                                    .orElse(null);
                            
                            if (contextMatch != null) {
                                System.out.println("🎯 Context-based match: " + path + " likely belongs to " + contextMatch.getName() + " based on referer");
                            }
                        }
                        
                        // If still no match, try pattern-based matching for known asset patterns
                        if (contextMatch == null) {
                            if (path.startsWith("/_app/immutable/") || path.startsWith("/api/")) {
                                // These are likely Immich assets
                                contextMatch = services.stream()
                                        .filter(s -> s.getName().toLowerCase().contains("immich"))
                                        .findFirst()
                                        .orElse(null);
                                
                                if (contextMatch != null) {
                                    System.out.println("🎯 Pattern-based match: " + path + " likely belongs to " + contextMatch.getName() + " based on path pattern");
                                }
                            } else if (path.startsWith("/hacsfiles/") || path.startsWith("/auth/") || path.startsWith("/manifest.json") || path.startsWith("/sw-modern.js")) {
                                // These are likely Home Assistant assets
                                contextMatch = services.stream()
                                        .filter(s -> s.getName().toLowerCase().contains("home") || s.getName().toLowerCase().contains("assistant"))
                                        .findFirst()
                                        .orElse(null);
                                
                                if (contextMatch != null) {
                                    System.out.println("🎯 Pattern-based match: " + path + " likely belongs to " + contextMatch.getName() + " based on path pattern");
                                }
                            }
                        }
                    }
                    
                    ServiceMapping selectedMapping = exactMatch != null ? exactMatch : contextMatch;
                    
                    if (selectedMapping == null) {
                        System.out.println("❌ No service found for path: " + path);
                        response.setStatusCode(org.springframework.http.HttpStatus.NOT_FOUND);
                        return response.setComplete();
                    }
                    
                    return proxyRequest(request, response, selectedMapping, path);
                });
    }
    
    private Mono<Void> proxyRequest(ServerHttpRequest request, ServerHttpResponse response, ServiceMapping mapping, String originalPath) {
        System.out.println("✅ Found matching service: " + mapping.getName() + " for path: " + originalPath);
        
        // Rewrite path: only remove the prefix if the path actually starts with it
        String newPath;
        if (originalPath.startsWith(mapping.getPathPrefix() + "/") || originalPath.equals(mapping.getPathPrefix())) {
            // Normal case: /ha/something -> /something or /ha -> /
            newPath = originalPath.substring(mapping.getPathPrefix().length());
            if (newPath.isEmpty() || !newPath.startsWith("/")) {
                newPath = "/" + newPath;
            }
        } else {
            // Context-based routing: /auth/authorize, /hacsfiles/iconset.js -> keep as-is
            newPath = originalPath;
            System.out.println("🔄 Context-based routing: keeping path as-is");
        }
        
        // Build target URL
        String targetUrl = mapping.getTargetUrl() + newPath;
        if (request.getURI().getQuery() != null) {
            targetUrl += "?" + request.getURI().getQuery();
        }
        
        final String finalTargetUrl = targetUrl;
        System.out.println("🎯 Proxying " + originalPath + " -> " + finalTargetUrl);
        
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
                    
                    // Copy response headers, but modify Location header for redirects
                    clientResponse.headers().asHttpHeaders().forEach((key, values) -> {
                        if (!isHopByHopHeader(key)) {
                            if (key.equalsIgnoreCase("Location")) {
                                // Rewrite Location header to include the path prefix
                                List<String> rewrittenValues = values.stream()
                                        .map(location -> rewriteLocationHeader(location, mapping))
                                        .toList();
                                System.out.println("🔄 Rewritten Location header: " + rewrittenValues);
                                response.getHeaders().addAll(key, rewrittenValues);
                            } else {
                                response.getHeaders().addAll(key, values);
                            }
                        }
                    });
                    
                    // Stream the response body - only rewrite small HTML responses
                    MediaType contentType = clientResponse.headers().contentType().orElse(null);
                    if (shouldRewriteContent(contentType) && isSmallResponse(clientResponse)) {
                        return response.writeWith(
                            rewriteResponseContent(
                                clientResponse.bodyToFlux(DataBuffer.class),
                                contentType,
                                mapping,
                                response.bufferFactory()
                            )
                        );
                    } else {
                        // Stream directly without rewriting for large responses or non-HTML content
                        return response.writeWith(clientResponse.bodyToFlux(DataBuffer.class));
                    }
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
    }
    
    private String rewriteLocationHeader(String location, ServiceMapping mapping) {
        // If the location is a relative path, prepend the path prefix
        if (location.startsWith("/") && !location.startsWith(mapping.getPathPrefix())) {
            return mapping.getPathPrefix() + location;
        }
        return location;
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
    
    private Flux<DataBuffer> rewriteResponseContent(Flux<DataBuffer> originalContent, 
                                                   MediaType contentType, 
                                                   ServiceMapping mapping, 
                                                   DataBufferFactory bufferFactory) {
        
        System.out.println("🔄 Rewriting HTML content for " + mapping.getName());
        
        // Collect all data buffers into a single string (only for small HTML responses)
        return originalContent
            .reduce(bufferFactory.allocateBuffer(), (accumulated, buffer) -> {
                accumulated.write(buffer);
                return accumulated;
            })
            .map(buffer -> {
                // Convert to string
                byte[] bytes = new byte[buffer.readableByteCount()];
                buffer.read(bytes);
                String content = new String(bytes, StandardCharsets.UTF_8);
                
                // Perform content rewriting
                String rewrittenContent = rewriteContent(content, mapping);
                
                // Convert back to DataBuffer
                byte[] rewrittenBytes = rewrittenContent.getBytes(StandardCharsets.UTF_8);
                return bufferFactory.wrap(rewrittenBytes);
            })
            .flux();
    }
    
    private boolean shouldRewriteContent(MediaType contentType) {
        return contentType != null && contentType.includes(MediaType.TEXT_HTML);
    }
    
    private boolean isSmallResponse(org.springframework.web.reactive.function.client.ClientResponse clientResponse) {
        // Only rewrite responses smaller than 1MB to avoid memory issues
        return clientResponse.headers().contentLength().orElse(0L) < 1024 * 1024;
    }
    
    private String rewriteContent(String content, ServiceMapping mapping) {
        String pathPrefix = mapping.getPathPrefix();
        
        // Common patterns to rewrite for web applications
        String rewritten = content;
        
        // Rewrite absolute paths in HTML/JS
        // src="/_app/... -> src="/immich/_app/...
        rewritten = rewritten.replaceAll("(src|href)=\"(/[^\"]*?)\"", "$1=\"" + pathPrefix + "$2\"");
        
        // Rewrite fetch() and similar API calls
        // fetch("/_app/... -> fetch("/immich/_app/...
        rewritten = rewritten.replaceAll("(fetch|import)\\s*\\(\\s*['\"](/[^'\"]*?)['\"]", "$1(\"" + pathPrefix + "$2\"");
        
        // Rewrite CSS url() references
        // url(/_app/... -> url(/immich/_app/...
        rewritten = rewritten.replaceAll("url\\s*\\(\\s*['\"]?(/[^'\"\\)]*?)['\"]?\\s*\\)", "url(" + pathPrefix + "$1)");
        
        // Rewrite JavaScript module imports
        // import ... from "/_app/... -> import ... from "/immich/_app/...
        rewritten = rewritten.replaceAll("(import\\s+.*?\\s+from\\s+['\"])(/[^'\"]*?)(['\"])", "$1" + pathPrefix + "$2$3");
        
        // Rewrite dynamic imports
        // import("/_app/... -> import("/immich/_app/...
        rewritten = rewritten.replaceAll("import\\s*\\(\\s*['\"](/[^'\"]*?)['\"]\\s*\\)", "import(\"" + pathPrefix + "$1\")");
        
        // Special handling for common API patterns
        if (pathPrefix.contains("immich")) {
            // Rewrite Immich API calls
            rewritten = rewritten.replaceAll("(['\"])/api/", "$1" + pathPrefix + "/api/");
        }
        
        if (pathPrefix.contains("ha")) {
            // Rewrite Home Assistant patterns
            rewritten = rewritten.replaceAll("(['\"])/auth/", "$1" + pathPrefix + "/auth/");
            rewritten = rewritten.replaceAll("(['\"])/hacsfiles/", "$1" + pathPrefix + "/hacsfiles/");
        }
        
        System.out.println("🔄 Content rewriting completed for " + mapping.getName());
        return rewritten;
    }
}