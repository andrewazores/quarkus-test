package org.acme;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.jboss.logging.Logger;

@Path("/cryostat-discovery")
public class CryostatResource {

    @Inject AppLifecycle lifecycle;
    @Inject Logger log;

    @POST
    public Void postPing() {
        log.info("received Cryostat registration ping, attempting re-registration...");
        lifecycle.reregister();
        return null;
    }

    @GET
    public Void getPing() {
        return null;
    }
}
