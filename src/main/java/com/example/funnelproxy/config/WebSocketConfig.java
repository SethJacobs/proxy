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
        // Handle WebSocket connections for all paths except admin
        map.put("/**", webSocketProxyHandler);
        
        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setUrlMap(map);
        mapping.setOrder(-1); // Give it higher priority
        return mapping;
    }
    
    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}