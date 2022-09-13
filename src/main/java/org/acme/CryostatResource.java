package org.acme;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;

import io.vertx.core.Vertx;

@Path("/cryostat-discovery")
public class CryostatResource {

    @Inject AppLifecycle lifecycle;
    @Inject Logger log;
    @Inject Vertx vertx;

    @POST
    public Response postPing() {
        if (!lifecycle.isRegistered()) {
            return Response.noContent().header("Content-Length", 0).build();
        }
        log.info("received Cryostat registration ping, attempting re-registration...");
        vertx.eventBus()
            .publish(AppLifecycle.EVENT_BUS_ADDRESS, null);
        return Response.ok().header("Content-Length", 0).build();
    }

    @GET
    public Response getPing() {
        if (!lifecycle.isRegistered()) {
            return Response.noContent().header("Content-Length", 0).build();
        }
        return Response.ok().header("Content-Length", 0).build();
    }
}
