# Centralized Logging System

A distributed, event-driven microservices-based logging system built with Java and Spring WebFlux. This system demonstrates advanced concurrency patterns, backpressure handling, and resource management.

## 🏗️ Architecture

The system consists of three main microservices:

### 1. **Client Services** (Log Generators)

- Simulates Linux and Windows system logs
- Generates realistic structured logs every 1-2 seconds
- Sends logs via TCP/UDP to the Log Collector
- Implements connection pooling and retry logic

### 2. **Log Collector** (Middleware)

- Receives logs via TCP (port 9090) and UDP (port 9091)
- Parses and validates incoming log messages
- Enriches logs with metadata and blacklist checking
- Forwards processed logs to Central Log Server
- Implements bounded worker pools for resource management

### 3. **Central Log Server** (Storage & Query)

- Ingests logs via REST API (`POST /ingest`)
- Stores logs in thread-safe in-memory storage
- Provides query API with filters (`GET /logs`)
- Exposes metrics endpoint (`GET /metrics`)
- Implements reactive streams with backpressure

## 🚀 Key Features

### Event-Driven Architecture

- **Spring WebFlux**: Fully reactive, non-blocking framework
- **Reactor Core**: Asynchronous stream processing
- **Bounded Schedulers**: Resource-managed thread pools
- **Backpressure**: Automatic flow control with bounded buffers

### Concurrency Management

- ✅ Bounded thread pools (no unbounded thread spawning)
- ✅ Worker pools with queue limits
- ✅ Graceful shutdown with timeout
- ✅ Thread-safe concurrent data structures
- ✅ Reactive streams for async processing

### Resource Management

- Connection pooling for HTTP clients
- Bounded queue sizes (500-20,000 based on load)
- Memory-efficient storage with ConcurrentLinkedQueue
- Automatic cleanup and resource disposal

## �️ Tech Stack

- **Language**: Java 21+
- **Framework**: Spring Boot 4.x with Spring WebFlux
- **Reactive**: Project Reactor, Reactive Streams
- **Build**: Maven
- **Concurrency**: Bounded schedulers, thread pools, reactive backpressure
- **Communication**: TCP, UDP, REST APIs

## 📋 Prerequisites

- Java 21 or higher
- Maven
- Git

## 🛠️ Installation & Setup

### 1. Clone the Repository

```bash
git clone https://github.com/Devanshu-Jethva/centralized-logging-system
cd centralized-logging-system
```

### 2. Build All Services

```bash
mvn clean install
```

This will compile all three microservices and run unit tests.

### 3. Run Services (in separate terminals)

**Terminal 1: Start Log Server**

```bash
cd log-server
mvn spring-boot:run
```

Server will start on `http://localhost:8080`

**Terminal 2: Start Log Collector**

```bash
cd log-collector
mvn spring-boot:run
```

Collector will start on `http://localhost:8081` with:

- TCP listener on port 9090
- UDP listener on port 9091

**Terminal 3: Start Client Service**

```bash
cd client-service
mvn spring-boot:run
```

Client will start generating and sending logs immediately.

## 📡 API Documentation

### Central Log Server APIs

#### 1. Ingest Logs

```bash
POST http://localhost:8080/ingest
Content-Type: application/json

{
  "timestamp": "2025-01-16T12:00:00Z",
  "event.category": "linux_login",
  "event.source.type": "linux",
  "username": "motadata",
  "hostname": "aiops9242",
  "severity": "INFO",
  "raw.message": "<86> aiops9242 sudo: session opened...",
  "is.blacklisted": false
}
```

#### 2. Query Logs

**Get all logs:**

```bash
curl http://localhost:8080/logs
```

**Filter by service (event category):**

```bash
curl "http://localhost:8080/logs?service=linux_login"
curl "http://localhost:8080/logs?service=windows_logout"
```

**Filter by severity level:**

```bash
curl "http://localhost:8080/logs?level=error"
curl "http://localhost:8080/logs?level=warn"
```

**Filter by username:**

```bash
curl "http://localhost:8080/logs?username=root"
```

**Filter blacklisted logs:**

```bash
curl "http://localhost:8080/logs?is.blacklisted=true"
```

**Combined filters with limit:**

```bash
curl "http://localhost:8080/logs?service=linux_login&level=error&limit=10"
curl "http://localhost:8080/logs?username=root&is.blacklisted=true"
```

**Sort by timestamp:**

```bash
curl "http://localhost:8080/logs?sort=timestamp&limit=20"
```

#### 3. Metrics

**Log Server Metrics:**

```bash
curl http://localhost:8080/metrics
```

Response:

```json
{
  "totalLogsReceived": 1523,
  "logsByCategory": {
    "linux_login": 856,
    "windows_login": 667
  },
  "logsBySeverity": {
    "info": 1345,
    "error": 123,
    "warn": 55
  }
}
```

**Log Collector Metrics:**

```bash
curl http://localhost:8081/metrics
```

#### 4. Health Checks

```bash
curl http://localhost:8080/health
curl http://localhost:8081/health
```

## 🧪 Testing

### Run All Tests

```bash
# Run tests for all modules
mvn test

# Run tests for specific module
cd log-server
mvn test
```

### Test Coverage

The project includes comprehensive unit tests covering:

- ✅ Client service application initialization
- ✅ Log generator functionality
- ✅ Log collector processing
- ✅ Log storage operations
- ✅ Query filtering logic
- ✅ Log parsing and enrichment
- ✅ Blacklist detection
- ✅ Metrics accuracy
- ✅ Concurrent processing
- ✅ Backpressure handling

### Integration Testing

**Test Log Flow:**

1. Start all three services
2. Observe client generating logs
3. Check collector metrics:
   ```bash
   curl http://localhost:8081/metrics
   ```
4. Query logs from server:
   ```bash
   curl "http://localhost:8080/logs?limit=10"
   ```

**Test Backpressure:**

Run multiple client instances simultaneously:

```bash
# Terminal 1
cd client-service && mvn spring-boot:run

# Terminal 2
cd client-service && mvn spring-boot:run

# Terminal 3
cd client-service && mvn spring-boot:run
```

Monitor system behavior - it should handle load gracefully without crashes.

## 🏛️ Architecture Details

### Concurrency Implementation

**Log Server:**

- Uses `Sinks.Many` for event-driven ingestion
- Bounded elastic scheduler (20 threads, 10,000 queue)
- Backpressure with overflow detection
- Reactive Flux for query operations

**Log Collector:**

- TCP: Fixed thread pool (20 threads)
- UDP: Fixed thread pool (15 threads)
- Bounded processing scheduler (30 threads, 20,000 queue)
- Async forwarding with retry logic

**Client Service:**

- Scheduled task executor (5 threads)
- Connection pooling for TCP/UDP
- Automatic reconnection on failures

### Backpressure Strategy

1. **Buffer Limits**: All queues have maximum capacity
2. **Overflow Handling**: Returns HTTP 429 when buffer is full
3. **Flow Control**: Reactive streams naturally apply backpressure
4. **Graceful Degradation**: Logs errors but continues processing

### Graceful Shutdown

All services implement `@PreDestroy` hooks:

- Completes in-flight tasks
- Closes network sockets
- Shuts down thread pools with 30-second timeout
- Cleans up resources

## 📊 Log Format Examples

### Linux Login Audit

```json
{
  "message": "<86> aiops9242 sudo: pam_unix(sudo:session): session opened for user root(uid=0) by motadata(uid=1000)"
}
```

### Linux Logout Audit

```json
{
  "message": "<86> server01 systemd-logind: session closed for user developer"
}
```

### Windows Login Audit

```json
{
  "message": "<134> WIN-EQ5V3RA5F7H Microsoft-Windows-Security-Auditing: A user account was successfully logged on. Account Name: Motadata"
}
```

### Windows Event Log

```json
{
  "message": "<134> WIN-SERVER01 Microsoft-Windows-EventLog: Service started successfully"
}
```

## 🔧 Configuration

### Ports

- Log Server: 8080 (HTTP)
- Log Collector: 8081 (HTTP), 9090 (TCP), 9091 (UDP)
- Client: No server port (client only)
