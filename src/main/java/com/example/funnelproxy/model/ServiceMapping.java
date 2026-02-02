package com.example.funnelproxy.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("service_mapping")
public class ServiceMapping {
    @Id
    private Long id;
    
    @Column("name")
    private String name;        // e.g., "Home Assistant"
    
    @Column("path_prefix")
    private String pathPrefix;  // e.g., "/ha"
    
    @Column("target_url")
    private String targetUrl;   // e.g., "http://homeassistant:8123"
    
    @Column("host")
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