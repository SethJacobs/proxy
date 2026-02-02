package com.example.funnelproxy.model;

import jakarta.persistence.*;

@Entity
public class ServiceMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    
    private String name;        // e.g., "Home Assistant"
    private String pathPrefix;  // e.g., "/ha"
    private String targetUrl;   // e.g., "http://homeassistant:8123"
    private String host;        // e.g., "homeassistant.home"
    
    // Default constructor
    public ServiceMapping() {}
    
    // Constructor with parameters
    public ServiceMapping(String name, String pathPrefix, String targetUrl, String host) {
        this.name = name;
        this.pathPrefix = pathPrefix;
        this.targetUrl = targetUrl;
        this.host = host;
    }
    
    // Getters and setters
    public Long getId() { 
        return id; 
    }
    
    public void setId(Long id) { 
        this.id = id; 
    }
    
    public String getName() { 
        return name; 
    }
    
    public void setName(String name) { 
        this.name = name; 
    }
    
    public String getPathPrefix() { 
        return pathPrefix; 
    }
    
    public void setPathPrefix(String pathPrefix) { 
        this.pathPrefix = pathPrefix; 
    }
    
    public String getTargetUrl() { 
        return targetUrl; 
    }
    
    public void setTargetUrl(String targetUrl) { 
        this.targetUrl = targetUrl; 
    }
    
    public String getHost() { 
        return host; 
    }
    
    public void setHost(String host) { 
        this.host = host; 
    }
}