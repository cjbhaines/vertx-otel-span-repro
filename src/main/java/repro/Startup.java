package repro;

import java.lang.management.ManagementFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.ThreadingModel;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.tracing.opentelemetry.OpenTelemetryOptions;
import repro.standard.StandardThreadVerticle;
import repro.virtual.VirtualThreadVerticle;

public class Startup {

    private static final Logger log = LoggerFactory.getLogger(Startup.class);

    public static void main(String[] args) {

        var runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        var arguments = runtimeMxBean.getInputArguments();

        Vertx vertx;

        if (arguments.stream().anyMatch(arg -> arg.contains("javaagent"))) {
            
            log.info("OpenTelemetry Java Agent is enabled, creating vanilla Vertx instance");
            vertx = Vertx.vertx(new VertxOptions());

        } else {
            
            log.info("OpenTelemetry Java Agent is not enabled, manually setting up OpenTelemetry SDK and creating Vertx instance with tracing options");
            
            Resource resource = Resource.getDefault()
                .merge(Resource.create(Attributes.builder()
                .put("service.name", "otel-repro")
                .build()));

            OtlpGrpcSpanExporter spanExporter = OtlpGrpcSpanExporter
                .builder()
                .setEndpoint("http://localhost:5555")
                .build();

            SdkTracerProvider sdkTracerProvider = SdkTracerProvider
                .builder()
                .setResource(resource)
                .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
                .build();

            OpenTelemetrySdk
                .builder()
                .setTracerProvider(sdkTracerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .buildAndRegisterGlobal();

            OpenTelemetryOptions tracingOptions = new OpenTelemetryOptions(GlobalOpenTelemetry.get());
            vertx = Vertx.vertx(new VertxOptions().setTracingOptions(tracingOptions));
        }

        log.info("Metrics Provider: " + GlobalOpenTelemetry.getMeterProvider().toString());
        log.info("Trace Provider: " + GlobalOpenTelemetry.getTracerProvider().toString());

        vertx
            .deployVerticle(new VirtualThreadVerticle(), new DeploymentOptions().setThreadingModel(ThreadingModel.VIRTUAL_THREAD))
            .onFailure(error -> {
                log.error("Failed to deploy VirtualThreadVerticle:", error);
                System.exit(1);
            });

        vertx
            .deployVerticle(new StandardThreadVerticle())
            .onFailure(error -> {
                log.error("Failed to deploy StandardThreadVerticle:", error);
                System.exit(1);
            });
    }
}
