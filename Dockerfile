# 1. Java 17 실행 환경 (베이스 이미지)
# openjdk:17-jdk-slim은 deprecated → Eclipse Temurin 사용
FROM eclipse-temurin:17-jre

# 2. 컨테이너 안에서 작업할 디렉토리
WORKDIR /app

# 3. 빌드된 jar 파일을 컨테이너 안으로 복사
# *-SNAPSHOT.jar 패턴을 사용해서 plain 제외
COPY build/libs/*-SNAPSHOT.jar app.jar

# 4. 애플리케이션이 사용하는 포트 문서화
EXPOSE 8080

# 5. 컨테이너 시작 시 실행할 명령어
ENTRYPOINT ["java", "-jar", "app.jar"]