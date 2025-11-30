#!/bin/bash

# Docker 이미지 설정
IMAGE_NAME="ddingsh9/article-server"
TAG="latest"
FULL_IMAGE_NAME="${IMAGE_NAME}:${TAG}"

# 색상 코드 설정
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}========================================${NC}"
echo -e "${YELLOW}Docker 이미지 빌드 및 푸시 자동화 스크립트${NC}"
echo -e "${YELLOW}========================================${NC}"
echo ""

# Docker 로그인 확인
echo -e "${GREEN}[1/6] Docker Hub 로그인 상태 확인...${NC}"
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}Docker가 실행 중이지 않습니다. Docker를 먼저 시작해주세요.${NC}"
    exit 1
fi

# Docker Hub 로그인 확인
docker pull alpine:latest > /dev/null 2>&1
if [ $? -ne 0 ]; then
    echo -e "${YELLOW}Docker Hub에 로그인이 필요합니다.${NC}"
    docker login
    if [ $? -ne 0 ]; then
        echo -e "${RED}Docker Hub 로그인에 실패했습니다.${NC}"
        exit 1
    fi
fi
echo -e "${GREEN}✓ Docker Hub 로그인 확인 완료${NC}"
echo ""

# Gradle 빌드 실행
echo -e "${GREEN}[2/6] Gradle 빌드 실행...${NC}"
if [ -f "./gradlew" ]; then
    ./gradlew clean bootJar
    if [ $? -ne 0 ]; then
        echo -e "${RED}Gradle 빌드 실패${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ Gradle 빌드 완료${NC}"
else
    echo -e "${YELLOW}Gradle wrapper를 찾을 수 없습니다. JAR 파일이 이미 존재한다고 가정합니다.${NC}"
fi
echo ""

# 기존 이미지 삭제 (있는 경우)
echo -e "${GREEN}[3/6] 기존 Docker 이미지 정리...${NC}"
if docker images | grep -q "${IMAGE_NAME}"; then
    docker rmi -f ${FULL_IMAGE_NAME} 2>/dev/null || true
    echo -e "${GREEN}✓ 기존 이미지 삭제 완료${NC}"
else
    echo -e "${YELLOW}기존 이미지가 없습니다.${NC}"
fi
echo ""

# Docker buildx 설정 확인 및 생성
echo -e "${GREEN}[4/6] Docker buildx 설정 확인...${NC}"
if ! docker buildx ls | grep -q "mybuilder"; then
    echo -e "${YELLOW}buildx 빌더 생성 중...${NC}"
    docker buildx create --name mybuilder --use
    docker buildx inspect --bootstrap
fi
docker buildx use mybuilder
echo -e "${GREEN}✓ Docker buildx 설정 완료${NC}"
echo ""

# Multi-platform Docker 이미지 빌드 및 푸시
echo -e "${GREEN}[5/6] Multi-platform Docker 이미지 빌드 및 푸시 중...${NC}"
echo -e "${YELLOW}대상 플랫폼: linux/amd64, linux/arm64${NC}"
docker buildx build \
    --platform linux/amd64,linux/arm64 \
    --tag ${FULL_IMAGE_NAME} \
    --push \
    .
if [ $? -ne 0 ]; then
    echo -e "${RED}Docker 이미지 빌드 실패${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Multi-platform Docker 이미지 빌드 및 푸시 완료: ${FULL_IMAGE_NAME}${NC}"
echo ""

# 빌드 정보 출력
echo -e "${GREEN}[6/6] 빌드 정보 출력${NC}"
echo ""
echo -e "${YELLOW}========================================${NC}"
echo -e "${GREEN}빌드 및 푸시 완료!${NC}"
echo -e "${YELLOW}========================================${NC}"
echo -e "이미지 이름: ${GREEN}${FULL_IMAGE_NAME}${NC}"
echo -e "이미지 크기: $(docker images ${IMAGE_NAME} --format 'table {{.Size}}' | tail -1)"
echo -e "빌드 시간: $(date '+%Y-%m-%d %H:%M:%S')"
echo ""
echo -e "${YELLOW}다음 명령어로 이미지를 실행할 수 있습니다:${NC}"
echo -e "${GREEN}docker run -d -p 8080:8080 ${FULL_IMAGE_NAME}${NC}"
echo ""
echo -e "${YELLOW}또는 docker-compose를 사용하세요:${NC}"
echo -e "${GREEN}docker-compose up -d${NC}"