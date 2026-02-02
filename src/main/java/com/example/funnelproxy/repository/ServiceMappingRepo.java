package com.example.funnelproxy.repository;

import com.example.funnelproxy.model.ServiceMapping;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface ServiceMappingRepo extends ReactiveCrudRepository<ServiceMapping, Long> {
    Mono<ServiceMapping> findByPathPrefix(String pathPrefix);
}