package org.acme;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.jboss.logging.Logger;

import io.vertx.core.Vertx;

@Path("/cryostat-discovery")
public class CryostatResource {

    @Inject AppLifecycle lifecycle;
    @Inject Logger log;
    @Inject Vertx vertx;

    @POST
    public Void postPing() {
        log.info("received Cryostat registration ping, attempting re-registration...");
        vertx.eventBus()
            .publish(AppLifecycle.EVENT_BUS_ADDRESS, null);
        return null;
    }

    @GET
    public Void getPing() {
        return null;
    }
}
