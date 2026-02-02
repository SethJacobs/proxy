package com.example.funnelproxy.websocket;

import com.example.funnelproxy.model.ServiceMapping;
import com.example.funnelproxy.repository.ServiceMappingRepo;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.publisher.Mono;

import java.net.URI;

@Component
public class WebSocketProxyHandler implements WebSocketHandler {
    
    private final ServiceMappingRepo repo;
    private final ReactorNettyWebSocketClient client;
    
    public WebSocketProxyHandler(ServiceMappingRepo repo) {
        this.repo = repo;
        this.client = new ReactorNettyWebSocketClient();
    }
    
    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String path = session.getHandshakeInfo().getUri().getPath();
        
        System.out.println("🔌 WebSocket connection attempt for path: " + path);
        
        // Skip admin paths - these should not be WebSocket connections
        if (path.startsWith("/admin") || path.equals("/health") || path.equals("/status") || path.equals("/")) {
            System.out.println("❌ Rejecting WebSocket connection for admin path: " + path);
            return session.close();
        }
        
        return repo.findAll()
                .doOnNext(service -> System.out.println("🔍 Checking WebSocket service: " + service.getPathPrefix()))
                .filter(s -> {
                    boolean matches = path.startsWith(s.getPathPrefix());
                    System.out.println("🔍 WebSocket path " + path + " matches " + s.getPathPrefix() + " = " + matches);
                    return matches;
                })
                .next()
                .flatMap(mapping -> proxyWebSocket(session, mapping, path))
                .switchIfEmpty(Mono.defer(() -> {
                    System.out.println("❌ No WebSocket service found for path: " + path);
                    return session.close();
                }));
    }
    
    private Mono<Void> proxyWebSocket(WebSocketSession session, ServiceMapping mapping, String path) {
        // Rewrite path for target service
        String newPath = path.replaceFirst("^" + mapping.getPathPrefix(), "");
        if (!newPath.startsWith("/")) {
            newPath = "/" + newPath;
        }
        
        // Build WebSocket URL for target service
        String targetWsUrl = mapping.getTargetUrl().replace("http://", "ws://").replace("https://", "wss://") + newPath;
        
        System.out.println("🔌 Proxying WebSocket " + path + " -> " + targetWsUrl);
        
        try {
            URI targetUri = URI.create(targetWsUrl);
            
            return client.execute(targetUri, targetSession -> {
                System.out.println("✅ WebSocket connection established to " + targetWsUrl);
                
                // Forward messages from client to target
                Mono<Void> input = session.receive()
                        .doOnNext(message -> System.out.println("📤 Forwarding message to target: " + message.getPayloadAsText()))
                        .map(message -> targetSession.textMessage(message.getPayloadAsText()))
                        .as(targetSession::send);
                
                // Forward messages from target to client
                Mono<Void> output = targetSession.receive()
                        .doOnNext(message -> System.out.println("📥 Forwarding message to client: " + message.getPayloadAsText()))
                        .map(message -> session.textMessage(message.getPayloadAsText()))
                        .as(session::send);
                
                return Mono.zip(input, output).then();
            });
        } catch (Exception e) {
            System.err.println("❌ WebSocket proxy error: " + e.getMessage());
            return session.close();
        }
    }
}