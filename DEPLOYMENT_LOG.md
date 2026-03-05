# Docker 배포 진행 로그

## 최종 업데이트: 2026-01-21

---

## 완료된 단계

### A단계: 로컬 Spring Boot 실행 에러 해결 ✅
### 1단계: Docker 개념 (이미지 vs 컨테이너) ✅
### 2단계: Docker 설치/동작 원리 ✅
### 3단계: Dockerfile 작성 ✅
### 4단계: 이미지 빌드/레이어 이해 ✅
### 5단계: 컨테이너 실행/포트/환경변수 ✅
### 6단계: 클라우드 서버 준비 ✅
### 7단계: 서버에 Docker 설치 ✅
### 8단계: 이미지 배포/실행 ✅

---

## 현재 상태

### AWS EC2 서버 정보
- **퍼블릭 IP**: 54.180.107.67 (인스턴스 재시작 시 변경됨)
- **인스턴스 이름**: finance-server
- **OS**: Ubuntu 24.04 LTS
- **인스턴스 유형**: t2.micro (프리티어)
- **키 페어**: finance-key2.pem
- **리전**: 서울 (ap-northeast-2)

### SSH 접속 명령어
```bash
ssh -i "C:\Users\dw-016\Downloads\finance-key2.pem" ubuntu@54.180.107.67
```

### Docker Hub
- **사용자명**: dragon5457
- **이미지**: dragon5457/finance-app:1.0

---

## 다음에 해야 할 작업 (8단계 이어서)

### 1. 서버에 SSH 접속
```bash
ssh -i "C:\Users\dw-016\Downloads\finance-key2.pem" ubuntu@43.201.68.171
```

### 2. docker-compose.yml 파일 다시 생성 (들여쓰기 주의!)
```bash
rm docker-compose.yml
nano docker-compose.yml
```

아래 내용 복사 (version, services, volumes 앞에 공백 없어야 함!):

```yaml
version: '3.8'

services:
  db:
    image: mariadb:10.11
    container_name: finance-db
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: rootpassword
      MYSQL_DATABASE: finance_db
      MYSQL_USER: jhy
      MYSQL_PASSWORD: 1234
    volumes:
      - db_data:/var/lib/mysql
    ports:
      - "3307:3306"
    healthcheck:
      test: ["CMD", "healthcheck.sh", "--connect", "--innodb_initialized"]
      interval: 10s
      timeout: 5s
      retries: 5

  app:
    image: dragon5457/finance-app:1.0
    container_name: finance-app
    restart: always
    ports:
      - "8080:8080"
    environment:
      - DB_URL=jdbc:mariadb://db:3306/finance_db?createDatabaseIfNotExist=true
      - DB_USERNAME=jhy
      - DB_PASSWORD=1234
    depends_on:
      db:
        condition: service_healthy

volumes:
  db_data:
```

저장: Ctrl+O → Enter → Ctrl+X

### 3. Docker Compose 실행
```bash
docker compose up -d
```

### 4. 상태 확인
```bash
docker compose ps
```

### 5. 브라우저 접속 테스트
```
http://43.201.68.171:8080
```

---

## 수정된 파일들

### 1. Dockerfile (베이스 이미지 변경)
- 변경: openjdk:17-jdk-slim → eclipse-temurin:17-jre
- 위치: C:\Users\dw-016\Desktop\java_inteli_workspace\finance_DWpj1\Dockerfile

### 2. application.properties (DB 환경변수화)
- 변경: DB URL, 사용자명, 비밀번호를 환경변수로 변경
- 위치: src/main/resources/application.properties
```properties
spring.datasource.url=${DB_URL:jdbc:mariadb://192.168.0.46:3307/finance_db?createDatabaseIfNotExist=true}
spring.datasource.username=${DB_USERNAME:jhy}
spring.datasource.password=${DB_PASSWORD:1234}
```

### 3. docker-compose.yml (새로 생성)
- 위치: C:\Users\dw-016\Desktop\java_inteli_workspace\finance_DWpj1\docker-compose.yml

---

## 남은 단계

### 8단계: 이미지 배포/실행 (Docker Compose) - 거의 완료
- [ ] docker-compose.yml 파일 서버에 올바르게 생성
- [ ] docker compose up -d 실행
- [ ] 브라우저 접속 테스트

### 9단계: 운영 정리 (로그/재배포/롤백/보안)
- [ ] 로그 확인 방법
- [ ] 재배포 방법
- [ ] 롤백 방법
- [ ] 보안 설정

---

## 유용한 명령어 모음

### 로컬 (Windows)
```bash
# JAR 빌드
cd C:\Users\dw-016\Desktop\java_inteli_workspace\finance_DWpj1
.\gradlew clean build -x test

# Docker 이미지 빌드
docker build -t dragon5457/finance-app:1.0 .

# Docker Hub 로그인
docker login

# Docker Hub에 푸시
docker push dragon5457/finance-app:1.0
```

### 서버 (Ubuntu)
```bash
# 컨테이너 상태 확인
docker compose ps

# 로그 확인
docker compose logs -f

# 앱 로그만 확인
docker logs finance-app

# 컨테이너 중지
docker compose down

# 컨테이너 재시작
docker compose restart

# 새 이미지로 업데이트
docker compose pull
docker compose up -d
```

---

## 다음 세션 시작 문장

"어제 Docker 배포 이어서 하자. DEPLOYMENT_LOG.md 파일 확인해줘."

---

## 학습 내용 요약

1. **Docker 이미지 vs 컨테이너**: 이미지는 설계도, 컨테이너는 실행 중인 인스턴스
2. **레이어 캐싱**: Dockerfile의 각 명령어가 레이어를 만들고, 변경 없으면 캐시 사용
3. **포트 매핑**: -p 호스트포트:컨테이너포트
4. **환경변수**: -e 또는 docker-compose.yml의 environment로 전달
5. **Docker Compose**: 여러 컨테이너를 한 번에 정의하고 실행
6. **베이스 이미지**: openjdk는 deprecated → eclipse-temurin 사용