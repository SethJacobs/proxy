package com.example.funnelproxy.controller;

import com.example.funnelproxy.model.ServiceMapping;
import com.example.funnelproxy.repository.ServiceMappingRepo;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@Order(1) // Higher priority than proxy controller
public class AdminController {
    private final ServiceMappingRepo repo;
    
    public AdminController(ServiceMappingRepo repo) {
        this.repo = repo;
    }
    
    @GetMapping("/admin")
    public String dashboard(Model model) {
        model.addAttribute("services", repo.findAll());
        return "dashboard";
    }
    
    @GetMapping("/admin/add")
    public String addForm(Model model) {
        model.addAttribute("service", new ServiceMapping());
        return "addService";
    }
    
    @PostMapping("/admin/add")
    public String addService(@ModelAttribute ServiceMapping service) {
        // Ensure pathPrefix starts with /
        if (!service.getPathPrefix().startsWith("/")) {
            service.setPathPrefix("/" + service.getPathPrefix());
        }
        repo.save(service);
        return "redirect:/admin";
    }
    
    @PostMapping("/admin/delete/{id}")
    public String deleteService(@PathVariable Long id) {
        repo.deleteById(id);
        return "redirect:/admin";
    }
    
    @GetMapping("/admin/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        ServiceMapping service = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Service not found"));
        model.addAttribute("service", service);
        return "editService";
    }
    
    @PostMapping("/admin/edit/{id}")
    public String editService(@PathVariable Long id, @ModelAttribute ServiceMapping service) {
        service.setId(id);
        // Ensure pathPrefix starts with /
        if (!service.getPathPrefix().startsWith("/")) {
            service.setPathPrefix("/" + service.getPathPrefix());
        }
        repo.save(service);
        return "redirect:/admin";
    }
}