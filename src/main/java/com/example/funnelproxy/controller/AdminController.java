package com.example.funnelproxy.controller;

import com.example.funnelproxy.model.ServiceMapping;
import com.example.funnelproxy.repository.ServiceMappingRepo;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@Order(1) // Higher priority than proxy controller
public class AdminController {
    private final ServiceMappingRepo repo;
    
    public AdminController(ServiceMappingRepo repo) {
        this.repo = repo;
    }
    
    // Serve the admin dashboard HTML
    @GetMapping(value = "/admin", produces = MediaType.TEXT_HTML_VALUE)
    public Mono<String> dashboard() {
        return Mono.fromCallable(() -> {
            try {
                ClassPathResource resource = new ClassPathResource("static/admin.html");
                return new String(resource.getInputStream().readAllBytes());
            } catch (Exception e) {
                return getDefaultAdminHtml();
            }
        });
    }
    
    // API endpoints for managing services
    @GetMapping("/admin/api/services")
    public Flux<ServiceMapping> getServices() {
        return repo.findAll();
    }
    
    @PostMapping("/admin/api/services")
    public Mono<ServiceMapping> addService(@RequestBody ServiceMapping service) {
        // Ensure pathPrefix starts with /
        if (!service.getPathPrefix().startsWith("/")) {
            service.setPathPrefix("/" + service.getPathPrefix());
        }
        return repo.save(service);
    }
    
    @PutMapping("/admin/api/services/{id}")
    public Mono<ServiceMapping> updateService(@PathVariable Long id, @RequestBody ServiceMapping service) {
        service.setId(id);
        // Ensure pathPrefix starts with /
        if (!service.getPathPrefix().startsWith("/")) {
            service.setPathPrefix("/" + service.getPathPrefix());
        }
        return repo.save(service);
    }
    
    @DeleteMapping("/admin/api/services/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteService(@PathVariable Long id) {
        return repo.deleteById(id);
    }
    
    private String getDefaultAdminHtml() {
        return """
<!DOCTYPE html>
<html>
<head>
    <title>Funnel Proxy Dashboard</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 40px; }
        table { border-collapse: collapse; width: 100%; margin: 20px 0; }
        th, td { border: 1px solid #ddd; padding: 12px; text-align: left; }
        th { background-color: #f2f2f2; }
        .btn { padding: 8px 16px; margin: 2px; text-decoration: none; border: none; cursor: pointer; border-radius: 4px; }
        .btn-primary { background-color: #007bff; color: white; }
        .btn-danger { background-color: #dc3545; color: white; }
        .btn-success { background-color: #28a745; color: white; }
        .form-group { margin-bottom: 15px; }
        .form-group label { display: block; margin-bottom: 5px; font-weight: bold; }
        .form-group input { width: 100%; max-width: 400px; padding: 8px; border: 1px solid #ddd; border-radius: 4px; }
        .hidden { display: none; }
        .actions { display: flex; gap: 5px; }
    </style>
</head>
<body>
    <h1>🚀 Funnel Proxy Dashboard</h1>
    <p>Manage your proxied services through a single Tailscale Funnel endpoint.</p>
    
    <div id="services-container">
        <div id="no-services" class="hidden">
            <p><em>No services configured yet. Add your first service below!</em></p>
        </div>
        
        <table id="services-table" class="hidden">
            <thead>
                <tr>
                    <th>Service Name</th>
                    <th>Path Prefix</th>
                    <th>Target URL</th>
                    <th>Host Header</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody id="services-tbody">
            </tbody>
        </table>
    </div>
    
    <div style="margin-top: 30px;">
        <button onclick="showAddForm()" class="btn btn-success">➕ Add New Service</button>
        <a href="/h2-console" class="btn btn-secondary" target="_blank">🗄️ Database Console</a>
    </div>
    
    <!-- Add/Edit Form -->
    <div id="service-form" class="hidden" style="margin-top: 30px; padding: 20px; border: 1px solid #ddd; border-radius: 5px;">
        <h3 id="form-title">Add New Service</h3>
        <form id="service-form-element">
            <input type="hidden" id="service-id">
            <div class="form-group">
                <label for="name">Service Name:</label>
                <input type="text" id="name" placeholder="e.g., Home Assistant" required>
            </div>
            <div class="form-group">
                <label for="pathPrefix">Path Prefix:</label>
                <input type="text" id="pathPrefix" placeholder="e.g., /ha" required>
            </div>
            <div class="form-group">
                <label for="targetUrl">Target URL:</label>
                <input type="text" id="targetUrl" placeholder="e.g., http://homeassistant:8123" required>
            </div>
            <div class="form-group">
                <label for="host">Host Header:</label>
                <input type="text" id="host" placeholder="e.g., homeassistant.local">
            </div>
            <div>
                <button type="submit" class="btn btn-primary">Save Service</button>
                <button type="button" onclick="hideForm()" class="btn btn-secondary">Cancel</button>
            </div>
        </form>
    </div>
    
    <script>
        let services = [];
        
        // Load services on page load
        document.addEventListener('DOMContentLoaded', loadServices);
        
        async function loadServices() {
            try {
                const response = await fetch('/admin/api/services');
                services = await response.json();
                renderServices();
            } catch (error) {
                console.error('Error loading services:', error);
            }
        }
        
        function renderServices() {
            const noServices = document.getElementById('no-services');
            const table = document.getElementById('services-table');
            const tbody = document.getElementById('services-tbody');
            
            if (services.length === 0) {
                noServices.classList.remove('hidden');
                table.classList.add('hidden');
            } else {
                noServices.classList.add('hidden');
                table.classList.remove('hidden');
                
                tbody.innerHTML = services.map(service => `
                    <tr>
                        <td>${service.name}</td>
                        <td>
                            <code>${service.pathPrefix}</code><br>
                            <small><a href="${service.pathPrefix}" target="_blank">🔗 Test Link</a></small>
                        </td>
                        <td>${service.targetUrl}</td>
                        <td>${service.host || ''}</td>
                        <td class="actions">
                            <button onclick="editService(${service.id})" class="btn btn-secondary">Edit</button>
                            <button onclick="deleteService(${service.id})" class="btn btn-danger">Delete</button>
                        </td>
                    </tr>
                `).join('');
            }
        }
        
        function showAddForm() {
            document.getElementById('form-title').textContent = 'Add New Service';
            document.getElementById('service-form-element').reset();
            document.getElementById('service-id').value = '';
            document.getElementById('service-form').classList.remove('hidden');
        }
        
        function editService(id) {
            const service = services.find(s => s.id === id);
            if (service) {
                document.getElementById('form-title').textContent = 'Edit Service';
                document.getElementById('service-id').value = service.id;
                document.getElementById('name').value = service.name;
                document.getElementById('pathPrefix').value = service.pathPrefix;
                document.getElementById('targetUrl').value = service.targetUrl;
                document.getElementById('host').value = service.host || '';
                document.getElementById('service-form').classList.remove('hidden');
            }
        }
        
        function hideForm() {
            document.getElementById('service-form').classList.add('hidden');
        }
        
        async function deleteService(id) {
            if (confirm('Are you sure you want to delete this service?')) {
                try {
                    await fetch(`/admin/api/services/${id}`, { method: 'DELETE' });
                    await loadServices();
                } catch (error) {
                    console.error('Error deleting service:', error);
                    alert('Error deleting service');
                }
            }
        }
        
        // Handle form submission
        document.getElementById('service-form-element').addEventListener('submit', async (e) => {
            e.preventDefault();
            
            const serviceId = document.getElementById('service-id').value;
            const service = {
                name: document.getElementById('name').value,
                pathPrefix: document.getElementById('pathPrefix').value,
                targetUrl: document.getElementById('targetUrl').value,
                host: document.getElementById('host').value
            };
            
            try {
                if (serviceId) {
                    // Update existing service
                    await fetch(`/admin/api/services/${serviceId}`, {
                        method: 'PUT',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify(service)
                    });
                } else {
                    // Add new service
                    await fetch('/admin/api/services', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify(service)
                    });
                }
                
                hideForm();
                await loadServices();
            } catch (error) {
                console.error('Error saving service:', error);
                alert('Error saving service');
            }
        });
    </script>
</body>
</html>
                """;
    }
}