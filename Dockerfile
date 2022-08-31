#
# Build stage
#
#FROM maven:3-alpine AS build
#WORKDIR /accounting
#COPY . .
#RUN mvn clean package -DskipTests -P FullStack


# Server stage
# 
FROM tomcat:8.5-jre8
WORKDIR /usr/local/tomcat
RUN rm -rf /usr/local/tomcat/webapps/ROOT
COPY ./accounting-application/target/accounting-application.war ./webapps/ROOT.war
#COPY --from=build /accounting/accounting-persistence/target/accounting-persistence-1.0.64-SNAPSHOT.jar ./accounting-persistence-1.0.64-SNAPSHOT.jar  
#COPY --from=build /accounting/accounting-controller/target/accounting-controller-1.0.64-SNAPSHOT.jar ./accounting-controller-1.0.64-SNAPSHOT.jar  
#COPY --from=build /accounting/accounting-service/target/accounting-service-1.0.64-SNAPSHOT.jar ./accounting-service-1.0.64-SNAPSHOT.jar  
COPY ./accounting-application/src/main/resources/application.properties /usr/local/tomcat/
#RUN ["apt-get", "update"]
#RUN ["apt-get", "install", "-y", "nano"]
EXPOSE 8080
ENTRYPOINT ["catalina.sh", "run"]
