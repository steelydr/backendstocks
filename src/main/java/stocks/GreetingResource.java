package stocks;

import io.quarkus.qute.Template;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/hello")
public class GreetingResource {
    @Inject
    Template hello;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public String hello() {
        return hello.data("name", "Standard Qute").render();
    }
}