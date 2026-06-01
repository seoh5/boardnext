FROM eclipse-temurin:17-jre
COPY boardrest.jar /
RUN mkdir -p /var/opt/Repository/profile
RUN mkdir -p /var/opt/Repository/file
ENTRYPOINT ["java", "-jar", "boardrest.jar"]
EXPOSE 8080 

