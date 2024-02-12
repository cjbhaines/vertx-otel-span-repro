package repro.middleware;

import io.opentelemetry.api.trace.Span;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class HttpRequestTracingHandler implements Handler<RoutingContext> {

    @Override
    public void handle(RoutingContext routingContext) {

        Span.current()
            .updateName(routingContext.normalizedPath())
            .setAttribute("http.target", routingContext.normalizedPath());

        routingContext.next();
    }
}
