# 🚀 Funnel Proxy Manager

A dynamic reverse proxy that provides a single Tailscale Funnel endpoint for multiple backend applications. Access Home Assistant, Immich, Pi-hole, and other services through sub-paths without needing separate DNS names.

## ✨ Features

- **Single Funnel Endpoint**: One Tailscale Funnel URL for all your services
- **Dynamic Routing**: Path-based routing (`/ha`, `/immich`, `/pihole`) with automatic path rewriting
- **WebSocket Support**: Full real-time functionality for Home Assistant and other WebSocket-dependent apps
- **Persistent Configuration**: H2 database stores all service mappings
- **Web Dashboard**: Easy-to-use interface for managing services
- **Zero Downtime**: Add/remove services without restarting

## 🏗️ Architecture

```
Internet → Tailscale Funnel → Funnel Proxy → Backend Services
                              (Port 80)
                                  ↓
                            ┌─────────────┐
                            │   /admin    │ → Web Dashboard
                            │   /ha       │ → Home Assistant
                            │   /immich   │ → Immich Photos
                            │   /pihole   │ → Pi-hole Admin
                            │   /grafana  │ → Grafana
                            └─────────────┘
```

## 🚀 Quick Start

### Prerequisites

- Java 17+
- Maven 3.6+
- Tailscale with Funnel enabled

### 1. Build and Run

```bash
# Clone and build
git clone <repository>
cd funnel-proxy
mvn clean package

# Run the application
java -jar target/funnel-proxy-1.0.0.jar
```

### 2. Configure Tailscale Funnel

```bash
# Enable funnel for your Tailscale node
tailscale funnel 80 on

# Your services will be accessible at:
# https://your-tailscale-node.tail2ca5d.ts.net/admin
```

### 3. Add Services

1. Visit `/admin` to access the dashboard
2. Click "Add New Service"
3. Configure your first service:
   - **Name**: Home Assistant
   - **Path Prefix**: `/ha`
   - **Target URL**: `http://homeassistant:8123`
   - **Host Header**: `homeassistant.local` (optional)

## 📋 Service Configuration Examples

### Home Assistant
- **Path**: `/ha`
- **Target**: `http://homeassistant:8123`
- **Host**: `homeassistant.local`

### Immich
- **Path**: `/immich`
- **Target**: `http://immich:2283`
- **Host**: `immich.local`

### Pi-hole
- **Path**: `/pihole`
- **Target**: `http://pihole:80`
- **Host**: `pihole.local`

### Grafana
- **Path**: `/grafana`
- **Target**: `http://grafana:3000`
- **Host**: `grafana.local`

## 🔧 Configuration

### Application Properties

```properties
# Server configuration
server.port=80

# H2 Database
spring.datasource.url=jdbc:h2:file:./data/funnel-proxy
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# WebSocket support
spring.webflux.websocket.max-frame-payload-length=65536
```

### Environment Variables

You can override configuration using environment variables:

```bash
export SERVER_PORT=8080
export SPRING_DATASOURCE_URL=jdbc:h2:file:/data/funnel-proxy
java -jar funnel-proxy-1.0.0.jar
```

## 🌐 How It Works

### HTTP Requests
1. Request comes in: `https://node.tail2ca5d.ts.net/ha/lovelace`
2. Proxy matches path prefix `/ha`
3. Rewrites path: `/ha/lovelace` → `/lovelace`
4. Sets Host header to configured value
5. Forwards to: `http://homeassistant:8123/lovelace`

### WebSocket Connections
1. WebSocket upgrade request: `wss://node.tail2ca5d.ts.net/ha/api/websocket`
2. Proxy establishes connection to: `ws://homeassistant:8123/api/websocket`
3. Bidirectional message forwarding maintains real-time functionality

## 🛠️ Development

### Running in Development

```bash
mvn spring-boot:run
```

### Database Console

Access the H2 console at `/h2-console`:
- **JDBC URL**: `jdbc:h2:file:./data/funnel-proxy`
- **Username**: `sa`
- **Password**: (empty)

### Adding Custom Logic

Extend the `ProxyService` class to add custom headers, authentication, or request/response transformation.

## 🐳 Docker Deployment

```dockerfile
FROM openjdk:17-jre-slim
COPY target/funnel-proxy-1.0.0.jar app.jar
EXPOSE 80
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

```bash
docker build -t funnel-proxy .
docker run -p 80:80 -v $(pwd)/data:/data funnel-proxy
```

## 🔒 Security Considerations

- The proxy runs on port 80 and should be behind Tailscale Funnel
- Admin interface is accessible at `/admin` - consider adding authentication for production
- Database file is stored locally - ensure proper backup procedures
- WebSocket connections are proxied without additional authentication

## 🚨 Troubleshooting

### Service Not Accessible
1. Check if the service is running: `curl http://target-host:port`
2. Verify path prefix doesn't conflict with existing routes
3. Check logs for proxy errors

### WebSocket Issues
1. Ensure target service supports WebSocket connections
2. Check if the service requires specific headers or protocols
3. Verify WebSocket URL transformation is correct

### Database Issues
1. Check file permissions for `./data/` directory
2. Access H2 console to verify service mappings
3. Restart application if database becomes corrupted

## 📝 License

MIT License - feel free to modify and distribute.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

---

**Happy proxying!** 🎉