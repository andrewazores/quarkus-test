package org.acme;

import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.annotations.jaxrs.HeaderParam;
import org.jboss.resteasy.annotations.jaxrs.PathParam;
import org.jboss.resteasy.annotations.jaxrs.QueryParam;

import io.vertx.core.json.JsonObject;

@Path("/api/v2.2/discovery")
@RegisterRestClient
public interface CryostatService {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    JsonObject register(RegistrationInfo registrationInfo, @HeaderParam("Authorization") String auth);

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id}")
    void deregister(@PathParam String id, @QueryParam String token);

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id}")
    void update(@PathParam String id, @QueryParam String token, Set<Node> subtree);

}
