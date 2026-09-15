FROM gradle:8.10.2-jdk17 AS build
WORKDIR /workspace
COPY . .
RUN gradle --no-daemon :backend:installDist

FROM eclipse-temurin:17-jre
WORKDIR /app
ENV HOST=0.0.0.0
COPY --from=build /workspace/backend/build/install/backend/ /app/
EXPOSE 8080
ENTRYPOINT ["/app/bin/backend"]
