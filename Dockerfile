FROM openjdk:17-jre-slim

# Create app directory
WORKDIR /app

# Copy the jar file
COPY target/funnel-proxy-1.0.0.jar app.jar

# Create data directory for H2 database
RUN mkdir -p /app/data

# Expose port 80
EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:80/admin || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]