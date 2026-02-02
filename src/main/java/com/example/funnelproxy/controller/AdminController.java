package com.example.funnelproxy.controller;

import com.example.funnelproxy.model.ServiceMapping;
import com.example.funnelproxy.repository.ServiceMappingRepo;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@Order(1) // Highest priority
public class AdminController {
    private final ServiceMappingRepo repo;
    
    public AdminController(ServiceMappingRepo repo) {
        this.repo = repo;
    }
    
    // Simple test endpoint
    @GetMapping("/admin/test")
    public Mono<String> test() {
        System.out.println("🧪 Admin test endpoint called");
        return Mono.just("Admin controller is working!");
    }
    
    // Serve the admin dashboard HTML
    @GetMapping(value = "/admin", produces = MediaType.TEXT_HTML_VALUE)
    public Mono<String> dashboard() {
        System.out.println("🎛️ Admin dashboard endpoint called");
        return Mono.just(getAdminHtml());
    }
    
    // API endpoints for managing services
    @GetMapping("/admin/api/services")
    public Flux<ServiceMapping> getServices() {
        return repo.findAll()
                .onErrorResume(error -> {
                    System.err.println("Error fetching services: " + error.getMessage());
                    return Flux.empty();
                });
    }
    
    @PostMapping("/admin/api/services")
    public Mono<ServiceMapping> addService(@RequestBody ServiceMapping service) {
        // Ensure pathPrefix starts with /
        if (service.getPathPrefix() != null && !service.getPathPrefix().startsWith("/")) {
            service.setPathPrefix("/" + service.getPathPrefix());
        }
        return repo.save(service)
                .onErrorResume(error -> {
                    System.err.println("Error saving service: " + error.getMessage());
                    return Mono.empty();
                });
    }
    
    @PutMapping("/admin/api/services/{id}")
    public Mono<ServiceMapping> updateService(@PathVariable Long id, @RequestBody ServiceMapping service) {
        service.setId(id);
        // Ensure pathPrefix starts with /
        if (service.getPathPrefix() != null && !service.getPathPrefix().startsWith("/")) {
            service.setPathPrefix("/" + service.getPathPrefix());
        }
        return repo.save(service)
                .onErrorResume(error -> {
                    System.err.println("Error updating service: " + error.getMessage());
                    return Mono.empty();
                });
    }
    
    @DeleteMapping("/admin/api/services/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteService(@PathVariable Long id) {
        return repo.deleteById(id)
                .onErrorResume(error -> {
                    System.err.println("Error deleting service: " + error.getMessage());
                    return Mono.empty();
                });
    }
    
    private String getAdminHtml() {
        return """
<!DOCTYPE html>
<html>
<head>
    <title>Funnel Proxy Dashboard</title>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <style>
        body { 
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; 
            margin: 0; 
            padding: 20px; 
            background-color: #f5f5f5; 
        }
        .container { 
            max-width: 1200px; 
            margin: 0 auto; 
            background: white; 
            padding: 30px; 
            border-radius: 8px; 
            box-shadow: 0 2px 10px rgba(0,0,0,0.1); 
        }
        h1 { color: #333; margin-bottom: 10px; }
        .subtitle { color: #666; margin-bottom: 30px; }
        table { 
            border-collapse: collapse; 
            width: 100%; 
            margin: 20px 0; 
            background: white;
        }
        th, td { 
            border: 1px solid #ddd; 
            padding: 12px; 
            text-align: left; 
        }
        th { 
            background-color: #f8f9fa; 
            font-weight: 600;
        }
        .btn { 
            padding: 8px 16px; 
            margin: 2px; 
            text-decoration: none; 
            border: none; 
            cursor: pointer; 
            border-radius: 4px; 
            font-size: 14px;
        }
        .btn-primary { background-color: #007bff; color: white; }
        .btn-danger { background-color: #dc3545; color: white; }
        .btn-success { background-color: #28a745; color: white; }
        .btn-secondary { background-color: #6c757d; color: white; }
        .form-group { margin-bottom: 15px; }
        .form-group label { 
            display: block; 
            margin-bottom: 5px; 
            font-weight: 600; 
            color: #333;
        }
        .form-group input { 
            width: 100%; 
            max-width: 400px; 
            padding: 10px; 
            border: 1px solid #ddd; 
            border-radius: 4px; 
            font-size: 14px;
        }
        .hidden { display: none; }
        .actions { display: flex; gap: 5px; }
        .status { 
            padding: 20px; 
            margin: 20px 0; 
            border-radius: 4px; 
            background-color: #d4edda; 
            border: 1px solid #c3e6cb; 
            color: #155724;
        }
        .error { 
            background-color: #f8d7da; 
            border-color: #f5c6cb; 
            color: #721c24;
        }
        code { 
            background-color: #f8f9fa; 
            padding: 2px 4px; 
            border-radius: 3px; 
            font-family: 'Monaco', 'Consolas', monospace;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>🚀 Funnel Proxy Dashboard</h1>
        <p class="subtitle">Manage your proxied services through a single Tailscale Funnel endpoint.</p>
        
        <div id="status" class="status hidden"></div>
        
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
        </div>
        
        <!-- Add/Edit Form -->
        <div id="service-form" class="hidden" style="margin-top: 30px; padding: 20px; border: 1px solid #ddd; border-radius: 5px; background-color: #f8f9fa;">
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
                    <label for="host">Host Header (optional):</label>
                    <input type="text" id="host" placeholder="e.g., homeassistant.local">
                </div>
                <div>
                    <button type="submit" class="btn btn-primary">Save Service</button>
                    <button type="button" onclick="hideForm()" class="btn btn-secondary">Cancel</button>
                </div>
            </form>
        </div>
        
        <div style="margin-top: 40px; padding: 20px; background-color: #f8f9fa; border-radius: 5px;">
            <h3>💡 How it works:</h3>
            <ul>
                <li><strong>Single Funnel URL:</strong> All services accessible through one Tailscale Funnel endpoint</li>
                <li><strong>Path-based routing:</strong> Each service gets its own path prefix (e.g., /ha, /immich, /pihole)</li>
                <li><strong>WebSocket support:</strong> Real-time features work seamlessly (Home Assistant, etc.)</li>
                <li><strong>Dynamic configuration:</strong> Add/remove services without restarting</li>
            </ul>
            
            <h3>🔧 Configuration Tips:</h3>
            <ul>
                <li><strong>Target URL:</strong> Use IP addresses (e.g., <code>http://192.168.1.100:8123</code>) if hostnames don't resolve</li>
                <li><strong>Docker networks:</strong> Use container names if services are in the same Docker network</li>
                <li><strong>Home Assistant:</strong> May need <code>http_base_url</code> configured to work behind a proxy</li>
                <li><strong>Host Header:</strong> Some services require specific host headers to function properly</li>
            </ul>
        </div>
    </div>
    
    <script>
        let services = [];
        
        // Load services on page load
        document.addEventListener('DOMContentLoaded', loadServices);
        
        function showStatus(message, isError = false) {
            const status = document.getElementById('status');
            status.textContent = message;
            status.className = isError ? 'status error' : 'status';
            status.classList.remove('hidden');
            setTimeout(() => status.classList.add('hidden'), 5000);
        }
        
        async function loadServices() {
            try {
                const response = await fetch('/admin/api/services');
                if (response.ok) {
                    services = await response.json();
                    renderServices();
                } else {
                    showStatus('Failed to load services', true);
                }
            } catch (error) {
                console.error('Error loading services:', error);
                showStatus('Error connecting to server', true);
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
                        <td>${escapeHtml(service.name || '')}</td>
                        <td>
                            <code>${escapeHtml(service.pathPrefix || '')}</code><br>
                            <small><a href="${escapeHtml(service.pathPrefix || '')}" target="_blank">🔗 Test Link</a></small>
                        </td>
                        <td>${escapeHtml(service.targetUrl || '')}</td>
                        <td>${escapeHtml(service.host || '')}</td>
                        <td class="actions">
                            <button onclick="editService(${service.id})" class="btn btn-secondary">Edit</button>
                            <button onclick="deleteService(${service.id})" class="btn btn-danger">Delete</button>
                        </td>
                    </tr>
                `).join('');
            }
        }
        
        function escapeHtml(text) {
            const div = document.createElement('div');
            div.textContent = text;
            return div.innerHTML;
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
                document.getElementById('name').value = service.name || '';
                document.getElementById('pathPrefix').value = service.pathPrefix || '';
                document.getElementById('targetUrl').value = service.targetUrl || '';
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
                    const response = await fetch(`/admin/api/services/${id}`, { method: 'DELETE' });
                    if (response.ok) {
                        showStatus('Service deleted successfully');
                        await loadServices();
                    } else {
                        showStatus('Failed to delete service', true);
                    }
                } catch (error) {
                    console.error('Error deleting service:', error);
                    showStatus('Error deleting service', true);
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
                let response;
                if (serviceId) {
                    // Update existing service
                    response = await fetch(`/admin/api/services/${serviceId}`, {
                        method: 'PUT',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify(service)
                    });
                } else {
                    // Add new service
                    response = await fetch('/admin/api/services', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify(service)
                    });
                }
                
                if (response.ok) {
                    showStatus(serviceId ? 'Service updated successfully' : 'Service added successfully');
                    hideForm();
                    await loadServices();
                } else {
                    showStatus('Failed to save service', true);
                }
            } catch (error) {
                console.error('Error saving service:', error);
                showStatus('Error saving service', true);
            }
        });
    </script>
</body>
</html>
                """;
    }
}