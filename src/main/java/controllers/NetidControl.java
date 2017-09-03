package controllers;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/netid")
public class NetidControl {
    @GET
    public String getNetid() {
        return "lm769";
    }
}
