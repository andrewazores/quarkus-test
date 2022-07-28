package org.acme;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

@Path("/cryostat-discovery")
public class CryostatResource {

    @POST
    public Void ping() {
        return null;
    }
}

