package com.example.funnelproxy.config;

import com.example.funnelproxy.model.ServiceMapping;
import com.example.funnelproxy.repository.ServiceMappingRepo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    
    private final ServiceMappingRepo repo;
    
    public DataInitializer(ServiceMappingRepo repo) {
        this.repo = repo;
    }
    
    @Override
    public void run(String... args) throws Exception {
        // Only add sample data if the database is empty
        repo.count().subscribe(count -> {
            if (count == 0) {
                // Add some example services (commented out by default)
                // Uncomment and modify these as needed for your setup
                
                /*
                repo.save(new ServiceMapping(
                    "Home Assistant", 
                    "/ha", 
                    "http://homeassistant:8123", 
                    "homeassistant.local"
                )).subscribe();
                
                repo.save(new ServiceMapping(
                    "Immich", 
                    "/immich", 
                    "http://immich:2283", 
                    "immich.local"
                )).subscribe();
                
                repo.save(new ServiceMapping(
                    "Pi-hole Admin", 
                    "/pihole", 
                    "http://pihole:80", 
                    "pihole.local"
                )).subscribe();
                */
                
                System.out.println("✅ Funnel Proxy initialized. Visit /admin to configure services.");
            }
        });
    }
}