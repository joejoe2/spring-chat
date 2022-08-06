FROM eclipse-temurin:17-jre
EXPOSE 8080
COPY start.sh wait-for-it.sh .
RUN chmod +x start.sh && chmod +x wait-for-it.sh
COPY ./target/chat-1.0-SNAPSHOT.jar web.jar
RUN sh -c 'touch web.jar'
