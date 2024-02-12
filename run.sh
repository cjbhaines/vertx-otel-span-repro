echo "Running repo"

mode=$1

if [ ! "$mode" == "with_agent" ] && [ ! "$mode" == "without_agent" ]; then
  echo "Invalid mode: $mode"
  echo "Usage: run.sh [with_agent|without_agent]"
  exit 1
fi

echo "Mode: $mode"

echo "Starting docker services"

echo "Shutting down telemetry services if running"
docker-compose -p vertx-otel-repro-telemetry -f docker-compose.yml down

echo "Running telemetry services via docker-compose"
docker-compose -p vertx-otel-repro-telemetry -f docker-compose.yml up -d

echo "Building jar"

./mvnw clean package -DskipTests -ntp -Dlogback.configurationFile=logback-tests.xml

echo "Running jar"

trap "kill 0" EXIT

if [ "$mode" == "with_agent" ]; then
  java \
    -Dotel.exporter.otlp.endpoint=http://localhost:5555 \
    -Dotel.exporter.otlp.protocol=grpc \
    -Dotel.traces.exporter=otlp \
    -Dotel.metrics.exporter=none \
    -Dotel.logs.exporter=none \
    -Dotel.traces.sampler=always_on \
    -Dotel.service.name=otel-repro \
    -Dotel.javaagent.logging=application \
    -javaagent:telemetry/opentelemetry-javaagent.jar \
    -jar target/repro-1.0.0-SNAPSHOT-jar-with-dependencies.jar &

  # Takes a while to start when the agent is enabled
  sleep 15
elif [ "$mode" == "without_agent" ]; then
  java -jar target/repro-1.0.0-SNAPSHOT-jar-with-dependencies.jar &

  sleep 15
fi

echo "Calling Vertx endpoints"

curl -S -s -o /dev/null http://localhost:8888/virtual
curl -S -s -o /dev/null http://localhost:8889/standard

sleep 5

echo "Opening Grafana in browser"

open http://localhost:3500/explore?schemaVersion=1&panes=%7B%22rDy%22:%7B%22datasource%22:%22tempo%22,%22queries%22:%5B%7B%22refId%22:%22A%22,%22datasource%22:%7B%22type%22:%22tempo%22,%22uid%22:%22tempo%22%7D,%22queryType%22:%22traceqlSearch%22,%22limit%22:20,%22tableType%22:%22traces%22,%22filters%22:%5B%7B%22id%22:%22c835351b%22,%22operator%22:%22%3D%22,%22scope%22:%22span%22%7D%5D%7D%5D,%22range%22:%7B%22from%22:%22now-6h%22,%22to%22:%22now%22%7D%7D%7D&orgId=1