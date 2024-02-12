package repro.standard;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.ext.web.Router;
import repro.middleware.HttpRequestTracingHandler;

public class StandardThreadVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    Router router = Router.router(vertx);
    router.get("/standard")
      .handler(new HttpRequestTracingHandler())
      .handler(new ReproStandardThreadHandler());

    var server = vertx.createHttpServer();
    server.requestHandler(router);
    server.listen(8889, http -> {
      if (http.succeeded()) {
        startPromise.complete();
        System.out.println("HTTP server started on port 8889");
      } else {
        startPromise.fail(http.cause());
      }
    });
  }

}
