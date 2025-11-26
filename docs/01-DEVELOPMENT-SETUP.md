# Article Server Development Setup Guide

**Version**: 1.0.0
**Last Updated**: 2025-11-26
**Compatibility**: Spring Boot 3.5.6, Java 17+

---

## Table of Contents

1. [System Requirements](#system-requirements)
2. [Environment Setup](#environment-setup)
3. [Database Configuration](#database-configuration)
4. [Message Broker Setup](#message-broker-setup)
5. [Application Configuration](#application-configuration)
6. [Build & Run](#build--run)
7. [Testing](#testing)
8. [Docker Setup (Optional)](#docker-setup-optional)
9. [IDE Configuration](#ide-configuration)
10. [Troubleshooting](#troubleshooting)

---

## System Requirements

### Prerequisites

| Component | Required Version | Verification Command     | Notes                 |
|-----------|------------------|--------------------------|-----------------------|
| Java      | 17+              | `java -version`          | OpenJDK or Oracle JDK |
| Gradle    | 7.5+             | `./gradlew --version`    | Wrapper included      |
| MariaDB   | 10.6+            | `mariadb --version`      | Primary database      |
| Redis     | 6.2+             | `redis-server --version` | Caching & locks       |
| Kafka     | 3.0+             | `kafka-topics --version` | Event streaming       |
| Git       | 2.30+            | `git --version`          | Version control       |

### Recommended Tools

| Tool           | Purpose              | Installation                                               |
|----------------|----------------------|------------------------------------------------------------|
| IntelliJ IDEA  | IDE                  | [Download](https://www.jetbrains.com/idea/)                |
| Postman        | API Testing          | [Download](https://www.postman.com/)                       |
| Docker Desktop | Container Management | [Download](https://www.docker.com/products/docker-desktop) |
| DBeaver        | Database GUI         | [Download](https://dbeaver.io/)                            |

---

## Environment Setup

### 1. Clone Repository

```bash
# Clone the repository
git clone https://github.com/DDINGJOO/Article-server_2025VERSION.git

# Navigate to project directory
cd Article-server

# Check branch
git branch -a
```

### 2. Project Structure

```
Article-server/
├── src/
│   ├── main/
│   │   ├── java/           # Source code
│   │   └── resources/      # Configuration files
│   └── test/              # Test code
├── docs/                  # Documentation
├── gradle/                # Gradle wrapper
├── .env.example          # Environment template
├── build.gradle          # Build configuration
└── docker-compose.yml    # Docker setup
```

### 3. Environment Variables

Create `.env` file from template:

```bash
cp .env.example .env
```

Edit `.env` with your configuration:

```properties
# Database
DATABASE_HOST=localhost
DATABASE_PORT=3306
DATABASE_NAME=article_db
DATABASE_USER_NAME=article_user
DATABASE_PASSWORD=your_secure_password
# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
# Kafka
KAFKA_URL1=localhost:9092
KAFKA_URL2=localhost:9093
KAFKA_URL3=localhost:9094
# Application
SPRING_PROFILES_ACTIVE=dev
```

---

## Database Configuration

### MariaDB Installation

#### macOS

```bash
# Install via Homebrew
brew install mariadb

# Start service
brew services start mariadb

# Secure installation
mysql_secure_installation
```

#### Ubuntu/Debian

```bash
# Update package index
sudo apt update

# Install MariaDB
sudo apt install mariadb-server

# Start service
sudo systemctl start mariadb

# Enable on boot
sudo systemctl enable mariadb

# Secure installation
sudo mysql_secure_installation
```

#### Windows

1. Download installer from [MariaDB Downloads](https://mariadb.org/download/)
2. Run installer with default settings
3. Set root password during installation
4. Add MariaDB bin directory to PATH

### Database Setup

```sql
-- Connect to MariaDB
mysql
-u root -p

-- Create database
CREATE DATABASE article_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Create user
CREATE USER 'article_user'@'localhost' IDENTIFIED BY 'your_secure_password';

-- Grant privileges
GRANT ALL PRIVILEGES ON article_db.* TO 'article_user'@'localhost';
FLUSH PRIVILEGES;

-- Verify setup
SHOW DATABASES;
USE article_db;
SHOW TABLES; -- Should be empty initially
```

### Schema Initialization

The application uses **Hibernate auto-DDL** in development mode. Tables will be created automatically on first run.

For production, use migration scripts:

```bash
# Generate schema
./gradlew generateSchema

# Apply migrations (if using Flyway)
./gradlew flywayMigrate
```

---

## Message Broker Setup

### Kafka Installation

#### Using Docker (Recommended)

```bash
# Start Kafka with Zookeeper
docker-compose up -d kafka zookeeper

# Verify Kafka is running
docker-compose ps
```

#### Manual Installation

```bash
# Download Kafka
wget https://downloads.apache.org/kafka/3.5.0/kafka_2.13-3.5.0.tgz
tar -xzf kafka_2.13-3.5.0.tgz
cd kafka_2.13-3.5.0

# Start Zookeeper
bin/zookeeper-server-start.sh config/zookeeper.properties

# Start Kafka (new terminal)
bin/kafka-server-start.sh config/server.properties
```

### Create Topics

```bash
# Create required topics
kafka-topics --create --topic article.created --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
kafka-topics --create --topic article.deleted --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
kafka-topics --create --topic article-image-changed --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1

# List topics
kafka-topics --list --bootstrap-server localhost:9092
```

---

## Application Configuration

### Profile-Based Configuration

The application supports multiple profiles:

| Profile | File                    | Purpose               |
|---------|-------------------------|-----------------------|
| `dev`   | `application-dev.yaml`  | Local development     |
| `test`  | `application-test.yaml` | Testing environment   |
| `prod`  | `application-prod.yaml` | Production deployment |

### Key Configuration Properties

```yaml
# application-dev.yaml
spring:
  datasource:
    url: jdbc:mariadb://${DATABASE_HOST}:${DATABASE_PORT}/${DATABASE_NAME}
    username: ${DATABASE_USER_NAME}
    password: ${DATABASE_PASSWORD}

  jpa:
    hibernate:
      ddl-auto: update  # Auto-create/update schema
    show-sql: true     # Log SQL queries

  kafka:
    bootstrap-servers: ${KAFKA_URL1},${KAFKA_URL2},${KAFKA_URL3}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
    consumer:
      group-id: article-consumer-group
      auto-offset-reset: earliest

  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT}
```

---

## Build & Run

### Build Project

```bash
# Clean build
./gradlew clean build

# Build without tests
./gradlew build -x test

# Check dependencies
./gradlew dependencies
```

### Run Application

#### Option 1: Gradle

```bash
# Run with default profile
./gradlew bootRun

# Run with specific profile
./gradlew bootRun --args='--spring.profiles.active=dev'
```

#### Option 2: JAR

```bash
# Build JAR
./gradlew bootJar

# Run JAR
java -jar build/libs/Article-server-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

#### Option 3: IDE

1. Open project in IntelliJ IDEA
2. Run `ArticleServerApplication.java`
3. Set VM options: `-Dspring.profiles.active=dev`

### Verify Application

```bash
# Health check
curl http://localhost:8080/actuator/health

# Expected response
{
  "status": "UP"
}

# API test
curl -X GET http://localhost:8080/api/v1/articles/search?size=10
```

---

## Testing

### Run All Tests

```bash
# Run all tests
./gradlew test

# Run with coverage
./gradlew test jacocoTestReport

# View coverage report
open build/reports/jacoco/test/html/index.html
```

### Run Specific Tests

```bash
# Run specific test class
./gradlew test --tests ArticleFactoryTest

# Run tests by pattern
./gradlew test --tests "*Factory*"

# Run integration tests only
./gradlew integrationTest
```

### Test Database

Tests use **H2 in-memory database** by default. Configuration in `application-test.yaml`:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
```

---

## Docker Setup (Optional)

### Full Stack with Docker Compose

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f article-server

# Stop services
docker-compose down

# Clean up volumes
docker-compose down -v
```

### Docker Compose Services

```yaml
version: '3.8'
services:
  mariadb:
    image: mariadb:10.6
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: article_db
      MYSQL_USER: article_user
      MYSQL_PASSWORD: password
    ports:
      - "3306:3306"

  redis:
    image: redis:6.2-alpine
    ports:
      - "6379:6379"

  kafka:
    image: confluentinc/cp-kafka:7.4.0
    depends_on:
      - zookeeper
    environment:
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
    ports:
      - "9092:9092"
```

---

## IDE Configuration

### IntelliJ IDEA

1. **Import Project**
	- File → Open → Select project directory
	- Import as Gradle project

2. **Enable Annotation Processing**
	- Preferences → Build, Execution, Deployment → Compiler → Annotation Processors
	- Check "Enable annotation processing"

3. **Configure Lombok**
	- Install Lombok plugin
	- Restart IDE

4. **Set JDK**
	- Project Structure → Project → SDK → Select Java 17

5. **Configure Run Configuration**
	- Edit Configurations → Add Spring Boot
	- Main class: `ArticleServerApplication`
	- Environment variables: Load from `.env`

### VS Code

1. **Install Extensions**
	- Java Extension Pack
	- Spring Boot Extension Pack
	- Lombok Annotations Support

2. **Configure launch.json**

```json
{
  "configurations": [
    {
      "type": "java",
      "name": "Spring Boot-ArticleServerApplication",
      "request": "launch",
      "mainClass": "com.teambind.articleserver.ArticleServerApplication",
      "envFile": "${workspaceFolder}/.env"
    }
  ]
}
```

---

## Troubleshooting

### Common Issues

#### 1. Database Connection Failed

```
Error: Communications link failure
```

**Solution:**

- Check MariaDB is running: `sudo systemctl status mariadb`
- Verify credentials in `.env`
- Check firewall settings
- Test connection: `mysql -h localhost -u article_user -p`

#### 2. Kafka Connection Issues

```
Error: Bootstrap broker localhost:9092 disconnected
```

**Solution:**

- Ensure Kafka is running: `docker-compose ps kafka`
- Check Zookeeper status
- Verify ports are not blocked
- Test with kafka-console-consumer

#### 3. Redis Connection Refused

```
Error: Unable to connect to Redis
```

**Solution:**

- Start Redis: `redis-server`
- Check port 6379 is available: `netstat -an | grep 6379`
- Test connection: `redis-cli ping`

#### 4. Port Already in Use

```
Error: Port 8080 is already in use
```

**Solution:**

- Find process: `lsof -i :8080`
- Kill process: `kill -9 <PID>`
- Or change port in `application.yaml`:

```yaml
server:
  port: 8081
```

#### 5. Gradle Build Failures

```
Error: Could not resolve dependencies
```

**Solution:**

- Clear cache: `./gradlew clean build --refresh-dependencies`
- Check proxy settings if behind firewall
- Verify Maven Central is accessible

### Debug Mode

Enable detailed logging:

```yaml
# application-dev.yaml
logging:
  level:
    com.teambind.articleserver: DEBUG
    org.springframework.web: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type: TRACE
```

---

## Support

### Resources

- [Project Repository](https://github.com/DDINGJOO/Article-server_2025VERSION)
- [Issue Tracker](https://github.com/DDINGJOO/Article-server_2025VERSION/issues)
- [API Documentation](API-SPECIFICATION.md)

### Contact

- Team Lead: platform-team@teambind.com
- Slack Channel: #article-server-dev

---

**Next Steps**: After setup completion, refer to [API Specification](API-SPECIFICATION.md) for endpoint documentation.

---
