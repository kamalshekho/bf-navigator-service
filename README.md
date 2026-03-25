# BF Navigator Service

Spring Boot service for querying German train stations (Deutsche Bahn APIs).

## Features

- **Station Search**: GET `/stations/search?query=Hamburg` – fuzzy search
- **Single Station**: GET `/stations/{id}` – details (wifi, mobility, category)
- **REST Client Tests**: `api/station.rest` – VSCode ready
- **DB Integration**: Deutsche Bahn StaDa v2 API
- **Reactive**: WebClient + MapStruct + Lombok
- **Unit Tests**: Controller & Service fully covered (`mvn test`)

## Quick Start - Local Dev

1. **Run**:

   ```bash
   ./mvnw spring-boot:run
   ```

2. **Test APIs** (port 8080):

   ```
   curl "http://localhost:8080/stations/search?query=Hamburg"
   curl "http://localhost:8080/stations/2458"
   ```

3. **VSCode REST**: Open `api/station.rest` – click Send Request

## Docker Compose (Full Stack)

1. **Start**:

   ```bash
   docker compose up --build
   ```

2. **Ports**:
   - App: `http://localhost:8081`
   - Postgres: `5432`

3. **Test**:
   ```
   curl "http://localhost:8081/stations/search?query=Hamburg"
   ```

## API Endpoints

| Method | Endpoint                     | Description     |
| ------ | ---------------------------- | --------------- |
| GET    | `/stations/search?query={q}` | Search stations |
| GET    | `/stations/{id}`             | Station details |

**DTO Fields**: name, number, evaNumber, city, category, hasSteplessAccess, hasMobilityService, hasWiFi

## Tech Stack

- Spring Boot 3.2 + WebFlux
- PostgreSQL
- MapStruct
- JUnit + Mockito
- OpenAPI Docs: `/swagger-ui.html`
- Docker

## Development

```
# Tests
mvn clean test

# Build JAR
mvn clean package

# Dev server
./mvnw spring-boot:run
```

Enjoy navigating German trains! 🚂
