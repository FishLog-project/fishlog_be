#도커 이미지를 위한 레시피 장소

# 1. Java 21 환경 기반 (베이스 이미지, 프로젝트 자바 버전에 맞추기)
FROM eclipse-temurin:21-jre

# 컨테이너 안 작업 디렉토리
WORKDIR /app

# 빌드된 JAR를 이미지 안으로 복사 (파일명은 본인 것으로!)
COPY build/libs/fishlog_be-0.0.1-SNAPSHOT.jar app.jar

# 시드 데이터(data/) 복사 — 앱이 /app 기준 상대경로(file:data/spot/spots_seed.json 등)로 읽으므로 필수.
# 없으면 SeedDataInitializer 기동 시 "시드 파일을 찾을 수 없습니다"로 컨텍스트 생성 실패.
COPY data ./data

# 이 컨테이너가 8080을 쓴다는 문서화
EXPOSE 8080

# 컨테이너 시작 시 실행할 명령
ENTRYPOINT ["java", "-jar", "app.jar"]