package org.acme;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.net.URI;
import java.rmi.registry.LocateRegistry;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.management.MBeanServer;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

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
    JMXConnectorServer jmxServer;
    @Inject Logger log;

    @ConfigProperty(name = "quarkus.application.name") String appName;
    @ConfigProperty(name = "org.acme.jmxport") int jmxport;

    void onStart(@Observes StartupEvent ev) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            onStop(null);
        }));
        tryRegister();
        if (this.plugin != null) {
            return;
        }
        this.submission = vertx.setPeriodic(5_000, id -> {
            tryRegister();
            if (this.plugin != null) {
                vertx.cancelTimer(id);
            }
        });
    }

    void onStop(@Observes ShutdownEvent ev) {
        if (this.submission != Long.MIN_VALUE) {
            this.vertx.cancelTimer(this.submission);
        }
        deregister();
    }

    private void tryRegister() {
        if (this.plugin != null) {
            return;
        }
        vertx.<PluginInfo>executeBlocking(promise -> {
            try {
                enableJmx();
                RegistrationInfo registration = new RegistrationInfo();
                registration.realm = "quarkus-test";
                registration.callback = "http://localhost/unimplemented-callback";
                JsonObject response = cryostat.register(registration);
                PluginInfo plugin = response.getJsonObject("data").getJsonObject("result").mapTo(PluginInfo.class);

                Node selfNode = new Node();
                selfNode.nodeType = "JVM";
                selfNode.name = "quarkus-test-" + plugin.id;
                selfNode.target = new Node.Target();
                selfNode.target.alias = appName;

                selfNode.target.connectUrl = URI.create(getSelfJmxUrl().toString());
                log.info("registering self as " + selfNode.target.connectUrl);
                cryostat.update(plugin.id, plugin.token, Set.of(selfNode));

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

    private void enableJmx() throws IOException {
        if (this.jmxServer != null) {
            return;
        }
        try {
            LocateRegistry.createRegistry(jmxport);
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            JMXServiceURL jmxUrl = getSelfJmxUrl();
            this.jmxServer = JMXConnectorServerFactory.newJMXConnectorServer(jmxUrl, null, mBeanServer);
            this.jmxServer.start();
            log.infof("Started JMX on %d", jmxport);
        } catch (IOException e) {
            this.jmxServer = null;
            throw e;
        }
    }

    private void deregister() {
        if (this.plugin != null) {
            vertx.executeBlocking(promise -> {
                try {
                    cryostat.deregister(this.plugin.id, this.plugin.token);
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
        if (this.jmxServer != null) {
            try {
                this.jmxServer.stop();
            } catch (IOException e) {
                log.warn(e);
                e.printStackTrace();
            }
        }
    }

    private JMXServiceURL getSelfJmxUrl() throws MalformedURLException {
        String hostname = System.getProperty("java.rmi.server.hostname", "localhost");
        return new JMXServiceURL(String.format("service:jmx:rmi:///jndi/rmi://%s:%d/jmxrmi", hostname, jmxport));
    }

}
