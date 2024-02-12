package repro.virtual;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.ext.web.Router;
import repro.middleware.HttpRequestTracingHandler;

public class VirtualThreadVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    Router router = Router.router(vertx);
    router.get("/virtual")
      .handler(new HttpRequestTracingHandler())
      .handler(new ReproVirtualThreadHandler());

    var server = vertx.createHttpServer();
    server.requestHandler(router);
    server.listen(8888, http -> {
      if (http.succeeded()) {
        startPromise.complete();
        System.out.println("HTTP server started on port 8888");
      } else {
        startPromise.fail(http.cause());
      }
    });
  }
  
}
