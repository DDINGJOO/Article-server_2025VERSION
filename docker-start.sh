#!/bin/bash
# Docker Compose 환경 시작 스크립트

echo "========================================="
echo "Article Server Docker Environment"
echo "========================================="
echo ""

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Docker Compose 실행
echo -e "${YELLOW}Starting Docker Compose services...${NC}"
docker-compose up -d

# 서비스 시작 대기
echo -e "${YELLOW}Waiting for services to be ready...${NC}"
sleep 5

# 서비스 상태 확인
echo ""
echo -e "${GREEN}=== Service Status ===${NC}"
docker-compose ps

# 연결 정보 출력
echo ""
echo -e "${GREEN}=== Connection Information ===${NC}"
echo "MariaDB:"
echo "  - Host: localhost"
echo "  - Port: 13306"
echo "  - Database: article_db"
echo "  - Username: article_user"
echo "  - Password: articlepass123"
echo ""
echo "Redis:"
echo "  - Host: localhost"
echo "  - Port: 16379"
echo ""
echo "Kafka:"
echo "  - Bootstrap Server: localhost:19092"
echo ""
echo -e "${GREEN}=== Management Tools ===${NC}"
echo "phpMyAdmin: http://localhost:18081"
echo "Kafka UI: http://localhost:18080"
echo "RedisInsight: http://localhost:18001"
echo ""

# 로그 확인 옵션
echo -e "${YELLOW}View logs with: docker-compose logs -f [service-name]${NC}"
echo -e "${YELLOW}Stop all services with: ./docker-stop.sh${NC}"
echo ""

# MariaDB 연결 테스트
echo -e "${YELLOW}Testing MariaDB connection...${NC}"
docker exec article-mariadb-dev mariadb -u article_user -particlepass123 -e "SELECT 'MariaDB is ready!' as status;" article_db 2>/dev/null
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ MariaDB is ready${NC}"
else
    echo -e "${RED}✗ MariaDB is not ready yet. Please wait a moment and try again.${NC}"
fi

# Redis 연결 테스트
echo -e "${YELLOW}Testing Redis connection...${NC}"
docker exec article-redis-dev redis-cli ping > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Redis is ready${NC}"
else
    echo -e "${RED}✗ Redis is not ready yet${NC}"
fi

echo ""
echo -e "${GREEN}Docker environment is ready!${NC}"
echo "You can now run the Spring Boot application with: ./gradlew bootRun -Pspring.profiles.active=dev"