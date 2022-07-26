package org.acme;

import java.net.URI;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
public class AppLifecycle {

    PluginInfo plugin;
    @Inject @RestClient CryostatService cryostat;

    void onStart(@Observes StartupEvent ev) {
        RegistrationInfo registration = new RegistrationInfo();
        registration.realm = "quarkus-test";
        registration.callback = "http://localhost/unimplemented-callback";
        JsonObject response = cryostat.register(registration);
        this.plugin = response.getJsonObject("data").getJsonObject("result").mapTo(PluginInfo.class);

        Node selfNode = new Node();
        selfNode.nodeType = "JVM";
        selfNode.name = "quarkus-test-" + this.plugin.id;
        selfNode.target = new Node.Target();
        selfNode.target.alias = "quarkus-test-1";
        selfNode.target.connectUrl = URI.create("service:jmx:rmi:///jndi/rmi://cryostat:9097/jmxrmi");
        cryostat.update(this.plugin.id, this.plugin.token, Set.of(selfNode));
    }

    void onStop(@Observes ShutdownEvent ev) {
        if (plugin != null) {
            cryostat.deregister(this.plugin.id, this.plugin.token);
        }
    }

}
