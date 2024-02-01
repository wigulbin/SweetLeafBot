FROM amazoncorretto:21-alpine-jdk
COPY /target/SweetLeafBot-1.0-SNAPSHOT.jar /home/SweetLeafBot-1.0-SNAPSHOT.jar
ARG guild_id=test
ENV guild_id=${guild_id}
ARG token=test
ENV token=${token}
VOLUME files logs
CMD ["java","-jar","/home/SweetLeafBot-1.0-SNAPSHOT.jar"]