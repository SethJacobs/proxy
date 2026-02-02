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
        // Only handle WebSocket connections for proxied services, not admin paths
        // We'll make this more specific once we have services configured
        // For now, disable WebSocket handling to fix the routing issue
        // map.put("/ws/**", webSocketProxyHandler);
        
        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setUrlMap(map);
        mapping.setOrder(10); // Lower priority than REST controllers
        return mapping;
    }
    
    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}