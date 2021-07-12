package org.acme;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/stream")
public class StreamResource {

    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public InputStream hello() {
        return new ByteArrayInputStream("Hello Streams".getBytes());
    }
}
