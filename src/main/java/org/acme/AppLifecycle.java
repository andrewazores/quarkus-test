package org.acme;

import java.net.URI;
import java.util.Set;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
public class AppLifecycle {

    @Inject @RestClient CryostatService cryostat;
    volatile PluginInfo plugin;
    long submission = Long.MIN_VALUE;
    @Inject Logger log;

    @ConfigProperty(name = "org.acme.CryostatService/mp-rest/url") String cryostatApiUrl;
    @ConfigProperty(name = "quarkus.application.name") String appName;
    @ConfigProperty(name = "quarkus.http.port") int httpPort;
    @ConfigProperty(name = "org.acme.jmxport") int jmxport;
    @ConfigProperty(name = "org.acme.jmxhost") String jmxhost;
    @ConfigProperty(name = "org.acme.CryostatService.callback-host") String callbackHost;
    @ConfigProperty(name = "org.acme.CryostatService.Authorization") String authorization;

    void onStart(@Observes StartupEvent ev) {
        tryRegister();
    }

    private void tryRegister() {
        if (this.plugin != null) {
            return;
        }

        try {
            RegistrationInfo registration = new RegistrationInfo();
            registration.realm = "quarkus-test-" + UUID.randomUUID();
            registration.callback = String.format("http://%s:%d/cryostat-discovery", callbackHost, httpPort);
            log.infof("registering self as %s at %s", registration.realm, cryostatApiUrl);
            JsonObject response = cryostat.register(registration, authorization);
            PluginInfo plugin = response.getJsonObject("data").getJsonObject("result").mapTo(PluginInfo.class);

            Node selfNode = new Node();
            selfNode.nodeType = "JVM";
            selfNode.name = "quarkus-test-" + plugin.id;
            selfNode.target = new Node.Target();
            selfNode.target.alias = appName;

            int port = Integer.valueOf(System.getProperty("com.sun.management.jmxremote.port", String.valueOf(jmxport)));

            selfNode.target.connectUrl = URI.create(String.format("service:jmx:rmi:///jndi/rmi://%s:%d/jmxrmi", jmxhost, port));
            log.infof("publishing self as %s", selfNode.target.connectUrl);
            cryostat.update(plugin.id, authorization, Set.of(selfNode));

            this.plugin = plugin;
        } catch (Exception e) {
            log.warn(e);
            e.printStackTrace();
            deregister();
            Quarkus.asyncExit(1);
            return;
        }
    }

    void onStop(@Observes ShutdownEvent ev) {
        deregister();
    }

    private void deregister() {
        if (this.plugin != null) {
            try {
                log.infof("deregistering as %s", this.plugin.id);
                cryostat.deregister(this.plugin.id, authorization);
            } catch (Exception e) {
                log.warn(e);
                e.printStackTrace();
                log.warn("Failed to deregister as Cryostat discovery plugin");
                return;
            }
            log.infof("Deregistered from Cryostat discovery plugin [%s]", this.plugin.id);
            this.plugin = null;
        }
    }

}
