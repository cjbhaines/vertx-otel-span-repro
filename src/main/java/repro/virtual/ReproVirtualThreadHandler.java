package repro.virtual;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.opentelemetry.api.trace.Span;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class ReproVirtualThreadHandler implements Handler<RoutingContext> {

    private static final Logger log = LoggerFactory.getLogger(ReproVirtualThreadHandler.class);

    @Override
    public void handle(RoutingContext routingContext) {
        
        var span = Span.current();

        log.info("Span ID: " + span.getSpanContext().getSpanId());

        span.setAttribute("custom-prop", "from-virtual-thread-model");

        routingContext.response().setStatusCode(200).end("Virtual Thread Model");
    }
    
}
