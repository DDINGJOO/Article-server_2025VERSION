# Docker Development Environment Setup

## Overview

Article Server provides a complete Docker Compose environment for local development, including all required services
with custom ports to avoid conflicts with existing containers.

## Services

| Service      | Container Name            | Host Port | Container Port | Description         |
|--------------|---------------------------|-----------|----------------|---------------------|
| MariaDB      | article-mariadb-dev       | 13306     | 3306           | Main database       |
| Redis        | article-redis-dev         | 16379     | 6379           | Cache server        |
| Kafka        | article-kafka-dev         | 19092     | 9092           | Message broker      |
| Zookeeper    | article-zookeeper-dev     | 12181     | 2181           | Kafka coordinator   |
| Kafka UI     | article-kafka-ui-dev      | 18080     | 8080           | Kafka management    |
| phpMyAdmin   | article-phpmyadmin-dev    | 18081     | 80             | Database management |
| RedisInsight | article-redis-insight-dev | 18001     | 8001           | Redis management    |

## Quick Start

### 1. Start the Environment

```bash
# Make scripts executable (first time only)
chmod +x docker-start.sh docker-stop.sh

# Start all services
./docker-start.sh
```

### 2. Run the Application

```bash
# Using Gradle with dev profile
./gradlew bootRun --args='--spring.profiles.active=dev'

# Or using JAR
java -jar -Dspring.profiles.active=dev build/libs/article-server-*.jar
```

### 3. Access Management Tools

- **phpMyAdmin**: http://localhost:18081
	- Username: root
	- Password: articlepass123

- **Kafka UI**: http://localhost:18080
	- No authentication required

- **RedisInsight**: http://localhost:18001
	- Add connection: localhost:16379

## Environment Variables

The following environment variables are configured in `.env.docker`:

```bash
# Database
DATABASE_HOST=localhost
DATABASE_PORT=13306
DATABASE_NAME=article_db
DATABASE_USER_NAME=article_user
DATABASE_PASSWORD=articlepass123

# Redis
REDIS_HOST=localhost
REDIS_PORT=16379

# Kafka
KAFKA_URL1=localhost:19092
```

## Application Profiles

### Development Profile (application-dev.yaml)

Configured to connect to Docker services:

- MariaDB on port 13306
- Redis on port 16379
- Kafka on port 19092

### Docker Profile (application-docker.yaml)

Used when the application runs inside Docker network:

- Uses internal Docker hostnames (article-mariadb, article-redis, etc.)
- Uses internal ports (3306, 6379, 9092)

## Database Schema

Initial schema and test data are automatically loaded from:

- `docker/mariadb/init/01-init-schema.sql` - Database structure
- `docker/mariadb/init/02-init-data.sql` - Sample data

### Sample Data Includes:

- 5 Boards (자유게시판, 공지사항, 이벤트, etc.)
- 10 Keywords (중요, Spring, Docker, etc.)
- 11 Sample Articles (Regular, Notice, Event types)
- Keyword mappings and article images

## Managing the Environment

### View Service Status

```bash
docker-compose ps
```

### View Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f article-mariadb
```

### Stop Services

```bash
./docker-stop.sh
```

### Reset Everything

```bash
# Stop services and remove volumes (deletes all data)
docker-compose down -v

# Start fresh
./docker-start.sh
```

## Testing Connections

### Test MariaDB

```bash
docker exec article-mariadb-dev \
  mariadb -u article_user -particlepass123 \
  -e "SELECT 'Connection OK' as status;" article_db
```

### Test Redis

```bash
docker exec article-redis-dev redis-cli ping
```

### Test Kafka

```bash
docker exec article-kafka-dev \
  kafka-topics --list --bootstrap-server localhost:9092
```

## API Testing

Once the application is running with the dev profile:

### Health Check

```bash
curl http://localhost:8080/actuator/health
```

### Get All Articles

```bash
curl http://localhost:8080/api/v1/articles
```

### Get Article by ID

```bash
curl http://localhost:8080/api/v1/articles/ART20251126001
```

## Troubleshooting

### Port Already in Use

If you get a "port already in use" error, check running containers:

```bash
docker ps | grep -E "13306|16379|19092|18080|18081|18001"
```

### Database Connection Issues

1. Ensure MariaDB is healthy:

```bash
docker-compose ps | grep mariadb
```

2. Check logs:

```bash
docker-compose logs article-mariadb
```

3. Verify credentials:

```bash
docker exec article-mariadb-dev env | grep MYSQL
```

### Redis Connection Issues

1. Check Redis status:

```bash
docker exec article-redis-dev redis-cli info server
```

2. Test connection:

```bash
docker exec article-redis-dev redis-cli --scan
```

### Kafka Connection Issues

1. Check Kafka and Zookeeper:

```bash
docker-compose logs article-kafka article-zookeeper
```

2. List topics:

```bash
docker exec article-kafka-dev kafka-topics \
  --list --bootstrap-server localhost:9092
```

## Performance Monitoring

### Database Queries

View slow queries in application logs (queries slower than 100ms are logged)

### JVM Metrics

Access Prometheus metrics:

```bash
curl http://localhost:8080/actuator/prometheus
```

### Cache Statistics

Monitor Redis cache usage via RedisInsight at http://localhost:18001

## Data Persistence

All data is persisted in Docker volumes:

- `article_mariadb_data` - Database files
- `article_redis_data` - Redis persistence
- `article_kafka_data` - Kafka logs
- `article_zookeeper_data` - Zookeeper data

To backup:

```bash
# Export database
docker exec article-mariadb-dev \
  mysqldump -u root -particlepass123 article_db > backup.sql

# Export Redis
docker exec article-redis-dev \
  redis-cli --rdb /data/dump.rdb
```

## Security Notes

- Default passwords are for development only
- Never use these credentials in production
- Consider using Docker secrets for sensitive data
- Firewall rules should block external access to Docker ports

## Additional Resources

- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Spring Boot with Docker](https://spring.io/guides/gs/spring-boot-docker/)
- [MariaDB Docker Hub](https://hub.docker.com/_/mariadb)
- [Redis Docker Hub](https://hub.docker.com/_/redis)
- [Confluent Kafka Docker](https://docs.confluent.io/platform/current/installation/docker/)
