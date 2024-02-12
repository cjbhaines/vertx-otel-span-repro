# Vertx Virtual Threads OpenTelemetry Auto Instrumentation Issue

Since moving to the Virtual Threading model, the [OpenTelemetry Automatic Java Instrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation) does not propagate the `Span.current()` context onto the Vertx virtual threads. 

We rely on the automatic instrumentation but also embellish the span data with custom attributes using the Otel SDK, and also set the span names to the route name which is much more usable than the HTTP verb.

This repository provides a reproduction of the issue, starting both standard threading model and virtual threading model verticles, with handler endpoints that try to interact with `Span.current()`.

## Repro Overview

This repro consists of 2 Vertx verticles:

- `StandardThreadVerticle` - Uses the Vertx standard threading model
- `VirtualThreadVerticle` - Uses the new Vertx Virtual threading model

Each verticle has an endpoint to call which adds traces, and uses `Span.current()` to get the active span and add a custom property. These verticles run on seperate ports.

Endpoints:

- [http://localhost:8888/virtual](http://localhost:8888/virtual)
- [http://localhost:8889/standard](http://localhost:8889/standard)

There is a key piece of middleware (`HttpRequestTracingHandler`) which makes some adustments to the span name, which by default is the HTTP verb which isn't particularly useful when tracing a large API, so it sets the span name to the normalized request path.

```
Span.current()
  .updateName(routingContext.normalizedPath())
  .setAttribute("http.target", routingContext.normalizedPath());
```

## Running

To run the repro, `run.sh` will:

- Start Grafana, Tempo and the OTEL collector using Docker
- Compile and start the repro jar
- Curl the repro endpoints
- Open Grafana in the browser

Run requires a parameter for which mode to run the jar with:

- `run.sh with_agent` - Which attaches the auto instrumentation agent
- `run.sh without_agent` - Which manually configures the OpenTelemetry SDk instead of using the agent


## Versions

- OpenTelemetry Java Automatic Instrumentation Agent: 2.0.0 (committed to repo)
- OpenTelemetry Java SDK: 1.35.0
- Vertx version: 4.5.3
- Java Version: 
  - openjdk 21.0.2 2024-01-16 LTS
  - OpenJDK Runtime Environment Corretto-21.0.2.13.1 (build 21.0.2+13-LTS)
  - OpenJDK 64-Bit Server VM Corretto-21.0.2.13.1 (build 21.0.2+13-LTS, mixed mode, sharing)
- OS for test results: macOS 14.0 (23A344)

Note: We use `amazoncorretto:21-alpine` in Production.

## Results

| Agent Mode | Standard Threaading Result| Virtual Threading Result |
| --- | --- | --- |
| Without Agent (Manual Otel SDK setup) | ✅ `Span.current()` propagated correctly | ✅ `Span.current()` propagated correctly |
| WITH Agent | ✅ `Span.current()` propagated correctly | ❌ `Span.current()` not propagated |

### With Auto Instrumentation Agent

The repro jar is run with the Auto Instrumentation Agent attached, and therefore does not touch the Vertx or OpenTelemetrey configuration and launches a vanilla Vertx instance.

Run arguments: 

```
run.sh with_agent
```

#### Console Output

![Grafana Visualization - With Agent - Console Output](/screenshots/with_agent_console.png)

#### Standard Threading Model

Result: ✅ `Span.current()` propagated correctly

Outcomes:
- The span is started by Netty
- The middleware handler has correctly changed the name to the normalized path
- The custom property `custom-prop` has been added to the span
- The span has a proper ID (i.e. is not a no-op)

![Grafana Visualization - With Agent - Standard Threading Model](/screenshots/with_agent_grafana_standard.png)

#### Virtual Threading Model

Result: ❌ `Span.current()` is not propagated

Outcomes:
- The span is started by Netty
- The middleware handler was incorrectly pointing at a no-op span
- The custom property was not added due to the no-op span
- The span has a no-op span ID

![Grafana Visualization - With Agent - Virtual Threading Model](/screenshots/with_agent_grafana_virtual.png)

### Without Auto Instrumentation Agent

To prove what we have been doing has worked before switching to Virtual threads, the repro contains an example that does not use the Auto Instrumentation agent.

Run arguments: 

```
run.sh without_agent
```

#### Console Output

![Grafana Visualization - Without Agent - Console Output](/screenshots/without_agent_console.png)

#### Standard Threading Model

Result: ✅ `Span.current()` propagated correctly

Outcomes:
- The span is started by Vertx
- The middleware handler has correctly changed the name to the normalized path
- The custom property `custom-prop` has been added to the span
- The span has a proper ID (i.e. is not a no-op)

![Grafana Visualization - Without Agent - Standard Threading Model](/screenshots/without_agent_grafana_standard.png)

#### Virtual Threading Model

Result: ✅ `Span.current()` propagated correctly

Outcomes:
- The span is started by Vertx
- The middleware handler has correctly changed the name to the normalized path
- The custom property `custom-prop` has been added to the span
- The span has a proper ID (i.e. is not a no-op)

![Grafana Visualization - Without Agent - Virtual Threading Model](/screenshots/without_agent_grafana_virtual.png)