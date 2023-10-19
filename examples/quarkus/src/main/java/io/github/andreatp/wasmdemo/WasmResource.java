package io.github.andreatp.wasmdemo;

import io.github.andreatp.wasmdemo.model.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/wasm")
public class WasmResource {

    @Inject WasmService service;

    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Response load(InputStream program) throws IOException {
        try (var bais = new ByteArrayInputStream(program.readAllBytes())) {
            if (bais.available() <= 0) {
                return new Response("NOT IMPORTED LENGTH IS 0");
            }
            service.setProgram(bais);
            return new Response("imported");
        } finally {
            program.close();
        }
    }
}
