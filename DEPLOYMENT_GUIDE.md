# finance_DWpj1 배포 가이드
# WSL2 + Ubuntu + Nginx + HTTPS (Let's Encrypt)

**최종 도메인**: https://finance260107-test.p-e.kr
**서버 공인 IP**: 211.176.114.102

---

## 📋 목차

1. [사전 체크 결과](#1-사전-체크-결과)
2. [수정한 파일 목록](#2-수정한-파일-목록)
3. [WSL2 + Ubuntu 설치](#3-wsl2--ubuntu-설치)
4. [DNS 설정](#4-dns-설정)
5. [환경변수 설정](#5-환경변수-설정)
6. [Nginx 설치 및 리버스 프록시 설정](#6-nginx-설치-및-리버스-프록시-설정)
7. [HTTPS (Let's Encrypt) 적용](#7-https-lets-encrypt-적용)
8. [Spring Boot 실행](#8-spring-boot-실행)
9. [최종 검증](#9-최종-검증)
10. [문제 해결 가이드](#10-문제-해결-가이드)

---

## 1) 사전 체크 결과

### ❌ 문제점 발견

```
┌─────────────────────────────────────────────────────────────┐
│ 항목              │ 상태    │ 결과                          │
├─────────────────────────────────────────────────────────────┤
│ 서버 OS           │ ❌ FAIL │ Windows 10 (MINGW64)          │
│                   │         │ 요구: Ubuntu 20.04/22.04      │
├─────────────────────────────────────────────────────────────┤
│ Spring Boot 8080  │ ❌ FAIL │ 서비스 미실행                 │
│                   │         │ Connection refused            │
├─────────────────────────────────────────────────────────────┤
│ 도메인 DNS        │ ❌ FAIL │ Non-existent domain           │
│                   │         │ DNS 설정 미완료               │
├─────────────────────────────────────────────────────────────┤
│ 서버 공인 IP      │ ✅ OK   │ 211.176.114.102               │
└─────────────────────────────────────────────────────────────┘
```

### ✅ 해결 방안

- **OS 문제**: WSL2에 Ubuntu 22.04 설치
- **Spring Boot**: 애플리케이션 실행
- **DNS 문제**: p-e.kr 관리 페이지에서 A 레코드 추가

---

## 2) 수정한 파일 목록

### ✅ 보안 개선 완료

| 파일 | 변경 내용 |
|------|-----------|
| `src/main/resources/application.properties` | • 민감정보 환경변수로 분리<br>• 기본 프로필 `dev` 설정<br>• 과도한 로그 제거 |
| `src/main/resources/application-dev.properties` | • 개발 환경 전용 설정<br>• 상세 로그 활성화<br>• Thymeleaf 캐시 비활성화 |
| `src/main/resources/application-prod.properties` | • 운영 환경 전용 설정<br>• 로그 레벨 WARNING<br>• Thymeleaf 캐시 활성화<br>• Linux 경로 사용 |
| `.env.example` | • 환경변수 템플릿 |
| `.env` | • 실제 환경변수 값 (Git 제외) |
| `.gitignore` | • `.env` 파일 제외 추가 |

### 🔐 환경변수로 분리된 민감정보

```properties
# Before (하드코딩 - 위험!)
gemini.api.key=AIzaSyAWyeYESnE0xfPYLCTU-njQ1e2ucdatzrE
spring.datasource.password=1234
alphavantage.api.key=GRG0V7M75GKNWPLF

# After (환경변수 - 안전!)
gemini.api.key=${GEMINI_API_KEY}
spring.datasource.password=${DB_PASSWORD}
alphavantage.api.key=${ALPHAVANTAGE_API_KEY}
```

---

## 3) WSL2 + Ubuntu 설치

### Step 1: PowerShell 관리자 권한 실행

1. 시작 메뉴에서 "PowerShell" 검색
2. 우클릭 → **관리자 권한으로 실행**

### Step 2: WSL2 설치

```powershell
# WSL 기능 활성화
wsl --install

# 특정 배포판 설치 (Ubuntu 22.04)
wsl --install -d Ubuntu-22.04

# 설치 확인
wsl --list --verbose
```

**⚠️ 중요**: 설치 후 **재부팅** 필수!

### Step 3: Ubuntu 초기 설정

재부팅 후 Ubuntu가 자동으로 열리면:

```bash
# 사용자 계정 생성
# Username: (원하는 이름)
# Password: (안전한 비밀번호)

# 시스템 업데이트
sudo apt update && sudo apt upgrade -y
```

### Step 4: WSL Ubuntu로 진입

이후 작업은 모두 WSL Ubuntu 터미널에서 진행합니다.

```powershell
# PowerShell에서 Ubuntu 실행
wsl -d Ubuntu-22.04
```

---

## 4) DNS 설정

### p-e.kr 도메인 관리 페이지에서 설정

**접속**: 도메인 등록 업체 관리 페이지 (p-e.kr 관리 업체)

**A 레코드 추가**:
```
타입: A
호스트명: finance260107-test
값(IP 주소): 211.176.114.102
TTL: 3600 (또는 기본값)
```

**DNS 전파 확인** (5~30분 소요):
```bash
# WSL Ubuntu에서 실행
nslookup finance260107-test.p-e.kr

# 또는
dig +short finance260107-test.p-e.kr
# 결과: 211.176.114.102가 나와야 함
```

---

## 5) 환경변수 설정

### Windows에서 `.env` 파일 수정

프로젝트 루트의 `.env` 파일을 확인/수정:

```bash
# .env 파일 내용
DB_PASSWORD=1234
GEMINI_API_KEY=AIzaSyAWyeYESnE0xfPYLCTU-njQ1e2ucdatzrE
OPENAI_API_KEY=your-openai-api-key-here
ALPHAVANTAGE_API_KEY=GRG0V7M75GKNWPLF
SPRING_PROFILES_ACTIVE=prod
```

### WSL Ubuntu에서 환경변수 로드

프로젝트를 WSL로 복사한 후:

```bash
# WSL Ubuntu에서 프로젝트 디렉토리로 이동
cd /mnt/c/Users/dw-016/Desktop/java_inteli_workspace/finance_DWpj1

# 환경변수 설정 (방법 1: .env 파일 사용)
# Spring Boot가 자동으로 .env 파일을 읽지 않으므로
# systemd 서비스 파일에 환경변수를 직접 설정하거나
# 실행 시 export로 설정

# 환경변수 설정 (방법 2: export 사용)
export DB_PASSWORD=1234
export GEMINI_API_KEY=AIzaSyAWyeYESnE0xfPYLCTU-njQ1e2ucdatzrE
export ALPHAVANTAGE_API_KEY=GRG0V7M75GKNWPLF
export SPRING_PROFILES_ACTIVE=prod

# 또는 ~/.bashrc에 추가하여 영구 설정
echo 'export DB_PASSWORD=1234' >> ~/.bashrc
echo 'export GEMINI_API_KEY=AIzaSyAWyeYESnE0xfPYLCTU-njQ1e2ucdatzrE' >> ~/.bashrc
echo 'export ALPHAVANTAGE_API_KEY=GRG0V7M75GKNWPLF' >> ~/.bashrc
echo 'export SPRING_PROFILES_ACTIVE=prod' >> ~/.bashrc
source ~/.bashrc
```

---

## 6) Nginx 설치 및 리버스 프록시 설정

### Step 1: Nginx 설치

```bash
# WSL Ubuntu에서 실행
sudo apt update
sudo apt install nginx -y

# Nginx 버전 확인
nginx -v

# Nginx 시작
sudo systemctl start nginx
sudo systemctl enable nginx

# 상태 확인
sudo systemctl status nginx
```

### Step 2: Nginx 설정 파일 생성

```bash
# 설정 파일 생성
sudo nano /etc/nginx/sites-available/finance260107-test.p-e.kr
```

**파일 내용** (HTTP만 - HTTPS는 certbot이 자동 추가):

```nginx
server {
    listen 80;
    server_name finance260107-test.p-e.kr;

    # 접근 로그
    access_log /var/log/nginx/finance_access.log;
    error_log /var/log/nginx/finance_error.log;

    # 리버스 프록시 설정
    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # WebSocket 지원 (필요시)
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";

        # 타임아웃 설정
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # 정적 파일 캐싱 (선택사항)
    location ~* \.(css|js|jpg|jpeg|png|gif|ico|svg)$ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        expires 7d;
        add_header Cache-Control "public, immutable";
    }
}
```

### Step 3: 설정 활성화

```bash
# 심볼릭 링크 생성
sudo ln -s /etc/nginx/sites-available/finance260107-test.p-e.kr /etc/nginx/sites-enabled/

# 기본 사이트 비활성화 (선택사항)
sudo rm /etc/nginx/sites-enabled/default

# 설정 문법 검사
sudo nginx -t

# 출력 예상:
# nginx: configuration file /etc/nginx/nginx.conf test is successful

# Nginx 재시작
sudo systemctl reload nginx
```

### Step 4: 방화벽 설정 (WSL은 Windows 방화벽 공유)

Windows 방화벽에서 80, 443 포트 허용 필요 (보통 자동 허용됨).

---

## 7) HTTPS (Let's Encrypt) 적용

### Step 1: Certbot 설치

```bash
# WSL Ubuntu에서 실행
sudo apt update
sudo apt install certbot python3-certbot-nginx -y
```

### Step 2: 인증서 발급

**⚠️ 전제조건**:
- DNS A 레코드가 정상 설정되어 있어야 함
- 80 포트가 외부에서 접근 가능해야 함
- Spring Boot가 실행 중일 필요는 없음

```bash
# HTTPS 인증서 자동 발급 및 Nginx 설정
sudo certbot --nginx -d finance260107-test.p-e.kr

# 프롬프트 질문에 답변:
# 1. 이메일 입력 (인증서 만료 알림용)
# 2. 약관 동의: Y
# 3. 마케팅 수신: N (선택)
# 4. HTTP → HTTPS 리다이렉트: 2 (Redirect 선택 권장)
```

**성공 시 출력**:
```
Successfully received certificate.
Certificate is saved at: /etc/letsencrypt/live/finance260107-test.p-e.kr/fullchain.pem
Key is saved at:         /etc/letsencrypt/live/finance260107-test.p-e.kr/privkey.pem
```

### Step 3: 자동 갱신 설정

Let's Encrypt 인증서는 90일 유효. 자동 갱신 확인:

```bash
# 자동 갱신 테스트 (실제 갱신 안 함)
sudo certbot renew --dry-run

# 성공 메시지:
# Congratulations, all simulated renewals succeeded
```

자동 갱신은 systemd 타이머로 자동 설정됨:
```bash
# 타이머 확인
sudo systemctl list-timers | grep certbot
```

### Step 4: Nginx 설정 최종 확인

certbot이 자동으로 Nginx 설정을 수정합니다. 확인:

```bash
sudo cat /etc/nginx/sites-available/finance260107-test.p-e.kr
```

**예상 결과** (certbot이 추가한 부분 포함):
```nginx
server {
    server_name finance260107-test.p-e.kr;

    access_log /var/log/nginx/finance_access.log;
    error_log /var/log/nginx/finance_error.log;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";

        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    location ~* \.(css|js|jpg|jpeg|png|gif|ico|svg)$ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        expires 7d;
        add_header Cache-Control "public, immutable";
    }

    listen 443 ssl; # managed by Certbot
    ssl_certificate /etc/letsencrypt/live/finance260107-test.p-e.kr/fullchain.pem; # managed by Certbot
    ssl_certificate_key /etc/letsencrypt/live/finance260107-test.p-e.kr/privkey.pem; # managed by Certbot
    include /etc/letsencrypt/options-ssl-nginx.conf; # managed by Certbot
    ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem; # managed by Certbot
}

server {
    if ($host = finance260107-test.p-e.kr) {
        return 301 https://$host$request_uri;
    } # managed by Certbot

    listen 80;
    server_name finance260107-test.p-e.kr;
    return 404; # managed by Certbot
}
```

---

## 8) Spring Boot 실행

### 방법 1: 개발 모드 (WSL 터미널에서 직접 실행)

```bash
# WSL Ubuntu에서 프로젝트 디렉토리로 이동
cd /mnt/c/Users/dw-016/Desktop/java_inteli_workspace/finance_DWpj1

# 환경변수 로드 (매번 실행 필요 또는 ~/.bashrc에 추가)
export SPRING_PROFILES_ACTIVE=prod
export DB_PASSWORD=1234
export GEMINI_API_KEY=AIzaSyAWyeYESnE0xfPYLCTU-njQ1e2ucdatzrE
export ALPHAVANTAGE_API_KEY=GRG0V7M75GKNWPLF

# Maven으로 실행
./mvnw spring-boot:run

# 또는 JAR로 빌드 후 실행
./mvnw clean package -DskipTests
java -jar target/finance_DWpj1-*.jar
```

### 방법 2: systemd 서비스 등록 (운영 환경 권장)

#### Step 1: JAR 파일 배포 위치로 복사

```bash
# 배포 디렉토리 생성
sudo mkdir -p /opt/finance_dwpj1
sudo chown $USER:$USER /opt/finance_dwpj1

# JAR 파일 복사
cp target/finance_DWpj1-*.jar /opt/finance_dwpj1/app.jar

# 필요한 디렉토리 생성
sudo mkdir -p /var/www/finance/uploads
sudo mkdir -p /var/www/finance/info_uploads
sudo chown -R $USER:$USER /var/www/finance
```

#### Step 2: Tesseract OCR 설치 (Linux 환경)

```bash
sudo apt install tesseract-ocr tesseract-ocr-eng tesseract-ocr-kor -y

# 설치 확인
tesseract --version

# tessdata 경로 확인
ls -la /usr/share/tesseract-ocr/5/tessdata
```

#### Step 3: systemd 서비스 파일 생성

```bash
sudo nano /etc/systemd/system/finance-dwpj1.service
```

**파일 내용**:

```ini
[Unit]
Description=Finance DWpj1 Spring Boot Application
After=syslog.target network.target

[Service]
User=your-username
Group=your-username
WorkingDirectory=/opt/finance_dwpj1

# 환경변수 설정
Environment="SPRING_PROFILES_ACTIVE=prod"
Environment="DB_PASSWORD=1234"
Environment="GEMINI_API_KEY=AIzaSyAWyeYESnE0xfPYLCTU-njQ1e2ucdatzrE"
Environment="ALPHAVANTAGE_API_KEY=GRG0V7M75GKNWPLF"

# Java 실행
ExecStart=/usr/bin/java -Xms512m -Xmx2g -jar /opt/finance_dwpj1/app.jar

# 재시작 정책
Restart=always
RestartSec=10

# 로그 설정
StandardOutput=journal
StandardError=journal
SyslogIdentifier=finance-dwpj1

[Install]
WantedBy=multi-user.target
```

**⚠️ 중요**: `your-username`을 실제 사용자명으로 변경!

```bash
# 현재 사용자명 확인
whoami
```

#### Step 4: 서비스 시작

```bash
# systemd 데몬 리로드
sudo systemctl daemon-reload

# 서비스 시작
sudo systemctl start finance-dwpj1

# 부팅 시 자동 시작 활성화
sudo systemctl enable finance-dwpj1

# 상태 확인
sudo systemctl status finance-dwpj1

# 로그 확인
sudo journalctl -u finance-dwpj1 -f
```

---

## 9) 최종 검증

### 1. localhost 확인 (WSL 내부)

```bash
curl -I http://127.0.0.1:8080

# 예상 결과:
# HTTP/1.1 200 OK
# Content-Type: text/html;charset=UTF-8
```

### 2. Nginx 리버스 프록시 확인 (HTTP)

```bash
curl -I http://finance260107-test.p-e.kr

# 예상 결과:
# HTTP/1.1 301 Moved Permanently
# Location: https://finance260107-test.p-e.kr/
```

### 3. HTTPS 확인

```bash
curl -I https://finance260107-test.p-e.kr

# 예상 결과:
# HTTP/2 200
# server: nginx
# content-type: text/html;charset=UTF-8
```

### 4. 웹 브라우저 테스트

1. 브라우저에서 접속: **https://finance260107-test.p-e.kr**
2. 자물쇠 아이콘 확인 (HTTPS 정상)
3. 인증서 정보 확인 (Let's Encrypt)

### 5. SSL 등급 체크 (선택사항)

- https://www.ssllabs.com/ssltest/
- 도메인 입력: finance260107-test.p-e.kr
- 예상 등급: A 또는 A+

---

## 10) 문제 해결 가이드

### 📌 DNS 문제

**증상**: `dig finance260107-test.p-e.kr` 결과가 IP와 다름

**해결**:
1. p-e.kr 관리 페이지에서 A 레코드 재확인
2. DNS 전파 대기 (최대 24시간, 보통 5~30분)
3. DNS 캐시 클리어:
   ```bash
   # Windows
   ipconfig /flushdns

   # WSL Ubuntu
   sudo systemd-resolve --flush-caches
   ```

---

### 📌 방화벽 문제

**증상**: 외부에서 접속 안 됨, WSL 내부에서는 됨

**해결**:
1. **Windows 방화벽 확인**:
   - 제어판 → Windows Defender 방화벽 → 고급 설정
   - 인바운드 규칙에서 80, 443 포트 허용 확인

2. **라우터 포트포워딩 확인** (공유기 사용 시):
   - 공유기 관리 페이지 접속
   - 포트포워딩 설정: 외부 80 → 내부 211.176.114.102:80
   - 포트포워딩 설정: 외부 443 → 내부 211.176.114.102:443

3. **WSL과 Windows 네트워크 확인**:
   ```bash
   # WSL에서 Windows IP 확인
   ip route show | grep default

   # Windows에서 WSL IP 확인
   wsl hostname -I
   ```

---

### 📌 Nginx 문제

**증상**: 502 Bad Gateway 또는 Nginx 시작 실패

**해결**:
1. **Nginx 설정 문법 검사**:
   ```bash
   sudo nginx -t
   ```

2. **Spring Boot 8080 포트 확인**:
   ```bash
   curl -I http://127.0.0.1:8080
   # Spring Boot가 실행 중이어야 함
   ```

3. **Nginx 에러 로그 확인**:
   ```bash
   sudo tail -f /var/log/nginx/finance_error.log
   ```

4. **포트 충돌 확인**:
   ```bash
   sudo netstat -tulpn | grep :80
   sudo netstat -tulpn | grep :443
   ```

---

### 📌 Spring Boot 문제

**증상**: 애플리케이션 시작 실패, 환경변수 오류

**해결**:
1. **환경변수 확인**:
   ```bash
   echo $DB_PASSWORD
   echo $GEMINI_API_KEY
   echo $SPRING_PROFILES_ACTIVE
   ```

2. **로그 확인**:
   ```bash
   # systemd 서비스인 경우
   sudo journalctl -u finance-dwpj1 -n 100 --no-pager

   # 직접 실행한 경우
   # 터미널 출력 확인
   ```

3. **데이터베이스 연결 확인**:
   ```bash
   # MariaDB 서버에 연결 가능한지 확인
   telnet 192.168.0.46 3307
   # 또는
   nc -zv 192.168.0.46 3307
   ```

4. **Tesseract 경로 확인**:
   ```bash
   ls -la /usr/share/tesseract-ocr/5/tessdata
   # eng.traineddata, kor.traineddata 존재 확인
   ```

5. **프로필 확인**:
   ```bash
   # application-prod.properties가 올바르게 적용되는지 확인
   # 로그에서 "The following 1 profile is active: prod" 메시지 확인
   ```

---

### 📌 HTTPS/인증서 문제

**증상**: certbot 인증서 발급 실패, SSL 오류

**해결**:
1. **DNS 전파 확인** (필수):
   ```bash
   dig +short finance260107-test.p-e.kr
   # 211.176.114.102가 나와야 함
   ```

2. **80 포트 외부 접근 확인**:
   ```bash
   # 다른 PC에서 확인
   curl -I http://finance260107-test.p-e.kr
   ```

3. **certbot 로그 확인**:
   ```bash
   sudo cat /var/log/letsencrypt/letsencrypt.log
   ```

4. **수동 인증서 발급 시도**:
   ```bash
   sudo certbot certonly --standalone -d finance260107-test.p-e.kr
   # Nginx를 잠시 중지하고 standalone 모드로 시도
   sudo systemctl stop nginx
   sudo certbot certonly --standalone -d finance260107-test.p-e.kr
   sudo systemctl start nginx
   ```

5. **인증서 강제 갱신**:
   ```bash
   sudo certbot renew --force-renewal
   ```

---

### 📌 WSL 관련 문제

**증상**: WSL 시작 안 됨, 네트워크 오류

**해결**:
1. **WSL 버전 확인**:
   ```powershell
   wsl --list --verbose
   # VERSION이 2인지 확인 (WSL2)
   ```

2. **WSL2로 업그레이드**:
   ```powershell
   wsl --set-version Ubuntu-22.04 2
   ```

3. **WSL 재시작**:
   ```powershell
   wsl --shutdown
   wsl -d Ubuntu-22.04
   ```

4. **Windows와 WSL 시간 동기화**:
   ```bash
   # WSL에서 실행
   sudo hwclock -s
   ```

---

## ✅ 배포 완료 체크리스트

- [ ] WSL2 Ubuntu 22.04 설치 완료
- [ ] DNS A 레코드 설정 완료 (finance260107-test.p-e.kr → 211.176.114.102)
- [ ] application.properties 환경변수 분리 완료
- [ ] .env 파일 생성 및 .gitignore 추가 완료
- [ ] Nginx 설치 및 리버스 프록시 설정 완료
- [ ] Let's Encrypt HTTPS 인증서 발급 완료
- [ ] 인증서 자동 갱신 테스트 성공
- [ ] Spring Boot systemd 서비스 등록 완료
- [ ] Spring Boot 8080 포트 정상 실행 확인
- [ ] https://finance260107-test.p-e.kr 접속 성공
- [ ] 자물쇠 아이콘 (HTTPS) 정상 표시
- [ ] 기능 테스트 완료 (로그인, API, OCR 등)

---

## 📞 지원

문제 발생 시:
1. 위 문제 해결 가이드 참조
2. 로그 확인 (Nginx, Spring Boot, certbot)
3. DNS 전파 상태 재확인
4. 방화벽 및 포트포워딩 재확인

**운영 환경 모니터링**:
```bash
# Nginx 상태
sudo systemctl status nginx

# Spring Boot 상태
sudo systemctl status finance-dwpj1

# 인증서 만료일 확인
sudo certbot certificates

# 실시간 로그 모니터링
sudo journalctl -u finance-dwpj1 -f
sudo tail -f /var/log/nginx/finance_error.log
```

---

**배포 완료!** 🎉