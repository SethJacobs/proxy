package com.example.funnelproxy.config;

import com.example.funnelproxy.websocket.WebSocketProxyHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class WebSocketConfig {
    
    @Bean
    public HandlerMapping webSocketMapping(WebSocketProxyHandler webSocketProxyHandler) {
        Map<String, WebSocketHandler> map = new HashMap<>();
        // Handle WebSocket connections for proxied services
        // This will catch WebSocket upgrade requests for any path that's not admin
        map.put("/**", webSocketProxyHandler);
        
        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setUrlMap(map);
        mapping.setOrder(50); // Between REST controllers (1-10) and proxy controller (200)
        return mapping;
    }
    
    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}