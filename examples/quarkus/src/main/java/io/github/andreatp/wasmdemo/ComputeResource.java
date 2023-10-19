package io.github.andreatp.wasmdemo;

import io.github.andreatp.wasmdemo.model.Response;
import io.github.andreatp.wasmdemo.model.StringContent;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/compute")
public class ComputeResource {

    @Inject WasmService service;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response load(StringContent content) {
        return new Response(service.compute(content.getContent()));
    }
}
