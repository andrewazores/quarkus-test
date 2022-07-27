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

    @Inject Vertx vertx;
    @Inject @RestClient CryostatService cryostat;
    volatile PluginInfo plugin;
    long submission = Long.MIN_VALUE;
    @Inject Logger log;

    @ConfigProperty(name = "quarkus.application.name") String appName;
    @ConfigProperty(name = "org.acme.jmxport") int jmxport;
    @ConfigProperty(name = "org.acme.CryostatService.Authorization") String authorization;

    void onStart(@Observes StartupEvent ev) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            onStop(null);
        }));
        tryRegister();
        this.submission = vertx.setPeriodic(5_000, id -> {
            tryRegister();
            if (this.plugin != null) {
                vertx.cancelTimer(id);
            }
        });
    }

    private void tryRegister() {
        if (this.plugin != null) {
            return;
        }
        vertx.<PluginInfo>executeBlocking(promise -> {
            try {
                RegistrationInfo registration = new RegistrationInfo();
                registration.realm = "quarkus-test";
                registration.callback = "http://localhost/unimplemented-callback";
                JsonObject response = cryostat.register(registration, authorization);
                PluginInfo plugin = response.getJsonObject("data").getJsonObject("result").mapTo(PluginInfo.class);

                Node selfNode = new Node();
                selfNode.nodeType = "JVM";
                selfNode.name = "quarkus-test-" + plugin.id;
                selfNode.target = new Node.Target();
                selfNode.target.alias = appName;

                String hostname = System.getProperty("java.rmi.server.hostname", "localhost");
                int jmxport = Integer.valueOf(System.getProperty("com.sun.management.jmxremote.port", "9097"));

                selfNode.target.connectUrl = URI.create(String.format("service:jmx:rmi:///jndi/rmi://%s:%d/jmxrmi", hostname, jmxport));
                log.info("registering self as " + selfNode.target.connectUrl);
                cryostat.update(plugin.id, authorization, Set.of(selfNode));

                promise.complete(plugin);
            } catch (Exception e) {
                promise.fail(e);
            }
        }, result -> {
            if (result.failed()) {
                log.warn(result.cause());
                result.cause().printStackTrace();
                deregister();
                return;
            }
            this.plugin = result.result();
        });
    }

    void onStop(@Observes ShutdownEvent ev) {
        if (this.submission != Long.MIN_VALUE) {
            this.vertx.cancelTimer(this.submission);
        }
        deregister();
    }

    private void deregister() {
        if (this.plugin != null) {
            vertx.executeBlocking(promise -> {
                try {
                    cryostat.deregister(this.plugin.id, authorization);
                    promise.complete();
                } catch (Exception e) {
                    promise.fail(e);
                }
            }, result -> {
                if (result.failed()) {
                    log.warn(result.cause());
                    result.cause().printStackTrace();
                    log.warn("Failed to deregister as Cryostat discovery plugin");
                    return;
                }
                log.infof("Deregistered from Cryostat discovery plugin [%s]", this.plugin.id);
                this.plugin = null;
            });
        }
    }

}
