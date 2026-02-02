package com.example.funnelproxy.websocket;

import com.example.funnelproxy.model.ServiceMapping;
import com.example.funnelproxy.repository.ServiceMappingRepo;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;

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
        
        List<ServiceMapping> services = repo.findAll();
        ServiceMapping mapping = services.stream()
                .filter(s -> path.startsWith(s.getPathPrefix()))
                .findFirst()
                .orElse(null);
        
        if (mapping == null) {
            return session.close();
        }
        
        // Rewrite path for target service
        String newPath = path.replaceFirst("^" + mapping.getPathPrefix(), "");
        if (!newPath.startsWith("/")) {
            newPath = "/" + newPath;
        }
        
        // Build WebSocket URL for target service
        String targetWsUrl = mapping.getTargetUrl().replace("http://", "ws://").replace("https://", "wss://") + newPath;
        
        try {
            URI targetUri = URI.create(targetWsUrl);
            
            return client.execute(targetUri, targetSession -> {
                // Forward messages from client to target
                Mono<Void> input = session.receive()
                        .map(message -> targetSession.textMessage(message.getPayloadAsText()))
                        .as(targetSession::send);
                
                // Forward messages from target to client
                Mono<Void> output = targetSession.receive()
                        .map(message -> session.textMessage(message.getPayloadAsText()))
                        .as(session::send);
                
                return Mono.zip(input, output).then();
            });
        } catch (Exception e) {
            return session.close();
        }
    }
}