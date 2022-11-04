package org.acme;

import java.net.URI;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
public class AppLifecycle {

    static final String EVENT_BUS_ADDRESS = AppLifecycle.class.getName();

    @Inject @RestClient CryostatService cryostat;
    volatile RegistrationInfo registration;
    volatile PluginInfo plugin;
    volatile long timerId = Long.MIN_VALUE;
    @Inject Logger log;
    @Inject Vertx vertx;

    @ConfigProperty(name = "org.acme.CryostatService/mp-rest/url") String cryostatApiUrl;
    @ConfigProperty(name = "quarkus.application.name") String appName;
    @ConfigProperty(name = "quarkus.http.port") int httpPort;
    @ConfigProperty(name = "org.acme.jmxport") int jmxport;
    @ConfigProperty(name = "org.acme.jmxhost") String jmxhost;
    @ConfigProperty(name = "org.acme.CryostatService.callback-host") String callbackHost;
    @ConfigProperty(name = "org.acme.CryostatService.Authorization") String authorization;
    @ConfigProperty(name = "org.acme.CryostatService.enabled") boolean enabled;

    void onStart(@Observes StartupEvent ev) {
        if (!enabled) {
            return;
        }
        this.registration = new RegistrationInfo();
        this.registration.realm = appName;
        this.registration.callback = String.format("http://%s:%d/cryostat-discovery", callbackHost, httpPort);

        vertx.setPeriodic(10_000L, timerId -> {
            this.timerId = timerId;
            try {
                register();
                vertx.cancelTimer(timerId);
            } catch (Exception e) {
                log.error("Registration failure", e);
            }
        });

        vertx.eventBus()
            .consumer(EVENT_BUS_ADDRESS)
            .handler(m -> reregister());
    }

    void onStop(@Observes ShutdownEvent ev) {
        if (!enabled) {
            return;
        }
        vertx.cancelTimer(timerId);
        deregister();
    }

    public boolean isRegistered() {
        return this.registration.id != null && !this.registration.id.isBlank();
    }

    private void register() {
        deregister();

        log.infof("registering self as %s at %s with %s", registration.realm, registration.callback, cryostatApiUrl);
        JsonObject response = cryostat.register(registration, authorization);
        PluginInfo plugin = response.getJsonObject("data").getJsonObject("result").mapTo(PluginInfo.class);
        this.registration.id = plugin.id;
        this.registration.token = plugin.token;

        Node selfNode = new Node();
        selfNode.nodeType = "JVM";
        selfNode.name = "quarkus-test-" + plugin.id;
        selfNode.target = new Node.Target();
        selfNode.target.alias = appName;

        int port = Integer.valueOf(System.getProperty("com.sun.management.jmxremote.port", String.valueOf(jmxport)));

        selfNode.target.connectUrl = URI.create(String.format("service:jmx:rmi:///jndi/rmi://%s:%d/jmxrmi", jmxhost, port));
        log.infof("publishing self as %s", selfNode.target.connectUrl);
        cryostat.update(plugin.id, plugin.token, Set.of(selfNode));

        this.plugin = plugin;
    }

    private void reregister() {
        if (this.plugin == null) {
            register();
            return;
        }

        try {
            log.infof("re-registering self as %s at %s", registration.realm, cryostatApiUrl);
            JsonObject response = cryostat.register(registration, authorization);
            PluginInfo plugin = response.getJsonObject("data").getJsonObject("result").mapTo(PluginInfo.class);
            this.registration.token = plugin.token;
        } catch (Exception e) {
            registration.token = null;
            register();
        }
    }

    private void deregister() {
        if (this.plugin != null) {
            try {
                log.infof("deregistering as %s", plugin.id);
                cryostat.deregister(plugin.id, plugin.token);
            } catch (Exception e) {
                log.warn(e);
                e.printStackTrace();
                log.warn("Failed to deregister as Cryostat discovery plugin");
                return;
            }
            log.infof("Deregistered from Cryostat discovery plugin [%s]", plugin.id);
            this.plugin = null;
        }
    }

}
