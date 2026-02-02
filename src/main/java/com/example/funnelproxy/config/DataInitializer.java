package com.example.funnelproxy.config;

import com.example.funnelproxy.model.ServiceMapping;
import com.example.funnelproxy.repository.ServiceMappingRepo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    
    private final ServiceMappingRepo repo;
    private final DatabaseClient databaseClient;
    
    public DataInitializer(ServiceMappingRepo repo, DatabaseClient databaseClient) {
        this.repo = repo;
        this.databaseClient = databaseClient;
    }
    
    @Override
    public void run(String... args) throws Exception {
        // Create table if it doesn't exist
        databaseClient.sql("""
            CREATE TABLE IF NOT EXISTS service_mapping (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                path_prefix VARCHAR(255) NOT NULL UNIQUE,
                target_url VARCHAR(500) NOT NULL,
                host VARCHAR(255)
            )
            """)
            .then()
            .subscribe(
                unused -> System.out.println("✅ Database table created successfully"),
                error -> System.err.println("❌ Error creating table: " + error.getMessage())
            );
        
        // Check if we need to add sample data
        repo.count().subscribe(count -> {
            if (count == 0) {
                System.out.println("✅ Funnel Proxy initialized. Visit /admin to configure services.");
            }
        });
    }
}