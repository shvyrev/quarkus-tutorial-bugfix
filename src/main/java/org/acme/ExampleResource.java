package org.acme;

import io.quarkus.arc.ManagedContext;
import io.vertx.axle.core.Vertx;
import io.vertx.axle.core.buffer.Buffer;
import io.vertx.axle.ext.web.client.HttpResponse;
import io.vertx.axle.ext.web.client.WebClient;
import io.vertx.core.json.JsonObject;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.ext.web.client.WebClientOptions;

import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.transaction.SystemException;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

@ActivateRequestContext
@Path("/hello")
public class ExampleResource {

    @Inject
    Vertx vertx;

    @Inject
    ThreadContext threadContext;

    @Inject
    ManagedExecutor managedExecutor;

    private static final Logger log = LoggerFactory.getLogger( ExampleResource.class );

    @Transactional
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        final Person person = new Person();
        person.persist();
        return "hello";
    }

    @Transactional
    @GET
    @Path("/v1/people")
    public CompletionStage<List<Person>> firstVersionPeople() throws SystemException {
        // Create a REST client to the Star Wars API
        WebClient client = WebClient.create(vertx,
                new WebClientOptions()
                        .setDefaultHost("swapi.co")
                        .setDefaultPort(443)
                        .setSsl(true));

        // get the list of Star Wars people, with context capture
        return threadContext.withContextCapture(client.get("/api/people/").send())
                .thenApply(response -> {
                    JsonObject json = response.bodyAsJsonObject();
                    List<Person> persons = new ArrayList<>(json.getInteger("count"));
                    // Store them in the DB
                    // Note that we're still in the same transaction as the outer method
                    for (Object element : json.getJsonArray("results")) {
                        Person person = new Person();
                        person.name = ((JsonObject) element).getString("name");
                        person.persist();
                        persons.add(person);
                    }
                    return persons;
                });
    }


    @Transactional
    @GET
    @Path("/v2/people")
    public CompletionStage<List<Person>> secondVersionPeople() throws SystemException {
        // Create a REST client to the Star Wars API
        WebClient client = WebClient.create(vertx,
                new WebClientOptions()
                        .setDefaultHost("swapi.co")
                        .setDefaultPort(443)
                        .setSsl(true));
        return client.get("/api/people").send()
                .thenApplyAsync(response -> {
                    final JsonObject json = response.bodyAsJsonObject();
                    List<Person> persons = new ArrayList<>(json.getInteger("count"));
                    for (Object element : json.getJsonArray("results")) {
                        Person person = new Person();
                        person.name = ((JsonObject) element).getString("name");
                        person.persist();
                        persons.add(person);
                    }
                    return persons;
                    }, managedExecutor);
    }
}