#!/bin/bash
# Docker Compose 환경 종료 스크립트

echo "========================================="
echo "Stopping Article Server Docker Environment"
echo "========================================="
echo ""

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 실행 중인 컨테이너 확인
echo -e "${YELLOW}Current running services:${NC}"
docker-compose ps

echo ""
echo -e "${YELLOW}Stopping all services...${NC}"
docker-compose down

echo ""
echo -e "${GREEN}All services stopped successfully!${NC}"

# 볼륨 삭제 옵션 제공
echo ""
echo -e "${YELLOW}Do you want to remove data volumes? (y/N)${NC}"
read -r response
if [[ "$response" =~ ^([yY][eE][sS]|[yY])$ ]]; then
    echo -e "${RED}Removing data volumes...${NC}"
    docker-compose down -v
    echo -e "${GREEN}Data volumes removed!${NC}"
else
    echo -e "${GREEN}Data volumes preserved.${NC}"
fi

echo ""
echo -e "${GREEN}Docker environment stopped!${NC}"