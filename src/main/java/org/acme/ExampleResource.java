package org.acme;

import io.vertx.axle.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.jvm.hotspot.debugger.ThreadContext;

import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@ActivateRequestContext
@Path("/hello")
public class ExampleResource {

    @Inject
    Vertx vertx;

    @Inject
    ThreadContext threadContext;

    private static final Logger log = LoggerFactory.getLogger( ExampleResource.class );

    @Transactional
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        final Person person = new Person();
        person.persist();
        return "hello";
    }
}