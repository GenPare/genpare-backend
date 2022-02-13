FROM openjdk:16-jdk

EXPOSE 8080

RUN mkdir /app
COPY ./build/install/genpare-backend/ /app/

WORKDIR /app/bin
CMD ["./genpare-backend"]