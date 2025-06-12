# 1단계: Gradle 빌드 단계 (JDK 17 환경)
FROM gradle:7.6.0-jdk17 AS builder
WORKDIR /home/app
# 프로젝트 소스 전체 복사
COPY . .
# Gradle 빌드 실행 (빌드 산출물은 build/libs 아래에 생성됨)
RUN chmod +x gradlew && ./gradlew clean build --no-daemon --refresh-dependencies

# 2단계: 실행 단계 (Debian 기반 OpenJDK 17 사용)
FROM openjdk:17-bullseye
WORKDIR /app

# JAVA_HOME 및 PATH 설정 (openjdk:17-bullseye의 기본 JAVA_HOME는 /usr/local/openjdk-17)
ENV JAVA_HOME=/usr/local/openjdk-17
ENV PATH=$JAVA_HOME/bin:$PATH

# 심볼릭 링크 생성: sonar-scanner가 /usr/lib/jvm/java-17-openjdk 경로를 사용하도록 함
RUN mkdir -p /usr/lib/jvm && ln -s $JAVA_HOME /usr/lib/jvm/java-17-openjdk

# SONAR_JAVA_CMD 및 SONAR_SCANNER_OPTS 설정 (SonarScanner가 Java 17을 사용하도록 강제)
ENV SONAR_JAVA_CMD=$JAVA_HOME/bin/java
ENV SONAR_SCANNER_OPTS="-Djava.home=$JAVA_HOME"

# /tmp/repositories 디렉토리 미리 생성 및 권한 설정
RUN mkdir -p /tmp/repositories && chmod -R 777 /tmp/repositories

# 필요한 패키지 설치 (curl, unzip, bash 등)
# 필요한 패키지 설치 (curl, unzip, bash, python3 등)
RUN apt-get update && apt-get install -y curl unzip bash python3 && rm -rf /var/lib/apt/lists/*

# sonar-scanner 설치 및 압축 해제
# sonar-scanner 설치 및 압축 해제
RUN curl -Lo sonar-scanner.zip https://binaries.sonarsource.com/Distribution/sonar-scanner-cli/sonar-scanner-cli-4.8.0.2856-linux.zip && \
    unzip sonar-scanner.zip && \
    mv sonar-scanner-4.8.0.2856-linux /opt/sonar-scanner && \
    rm sonar-scanner.zip

# 내장 JRE 사용 비활성화: sonar-scanner 스크립트 수정
RUN sed -i 's/use_embedded_jre=true/use_embedded_jre=false/' /opt/sonar-scanner/bin/sonar-scanner

# PATH 에 sonar-scanner 실행 파일이 포함된 디렉토리 추가
ENV PATH=$PATH:/opt/sonar-scanner/bin

# PMD CLI 설치
RUN curl -Lo pmd.zip https://github.com/pmd/pmd/releases/download/pmd_releases/7.12.0/pmd-dist-7.12.0-bin.zip && \
    unzip pmd.zip && \
    mv pmd-bin-7.12.0 /opt/pmd && \
    rm pmd.zip

# PMD CLI의 bin 디렉토리를 PATH에 추가
ENV PATH=$PATH:/opt/pmd/bin

# PMD result xml to json
COPY scripts/pmd_to_sonar.py /app/scripts/pmd_to_sonar.py
RUN chmod +x /app/scripts/pmd_to_sonar.py

# 빌드 단계에서 생성된 jar 파일 복사 (필요에 따라 파일명을 조정)
COPY --from=builder /home/app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

