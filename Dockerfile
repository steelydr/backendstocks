# Use Eclipse Temurin (OpenJDK 17)
FROM eclipse-temurin:17-jre as runtime

# Set working directory
WORKDIR /app

# Copy the Quarkus JAR file to the container
COPY target/backendstocks-1.0.0-SNAPSHOT-runner.jar app.jar

# Expose the port the application listens on
EXPOSE 8080

# Command to run the application
CMD ["java", "-jar", "/app/app.jar"]
