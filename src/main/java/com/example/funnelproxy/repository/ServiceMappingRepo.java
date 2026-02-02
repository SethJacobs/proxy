package com.example.funnelproxy.repository;

import com.example.funnelproxy.model.ServiceMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ServiceMappingRepo extends JpaRepository<ServiceMapping, Long> {
    Optional<ServiceMapping> findByPathPrefix(String pathPrefix);
}