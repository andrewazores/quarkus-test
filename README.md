[![Docker Repository on Quay](https://quay.io/repository/andrewazores/quarkus-test/status "Docker Repository on Quay")](https://quay.io/repository/andrewazores/quarkus-test)

# code-with-quarkus project

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell script
./mvnw compile quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:10010/q/dev/.

## Packaging and running the application

The [`cryostat-agent`](https://github.com/cryostatio/cryostat-agent) JAR dependency will be downloaded from an
authenticated repository by default. Add or merge the following configuration into your `$HOME/.m2/settings.xml`,
creating the file if it does not exist:

```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">
  <servers>
    <server>
      <id>github</id>
      <username>$MY_GITHUB_USERNAME</username>
      <password>$MY_GITHUB_ACCESSTOKEN</password>
    </server>
  </servers>
</settings>
```

The token must have the `read:packages` permission. It is recommended that this is the *only* permission the token has.

Alternatively, clone the `-agent` repository, check out the required tagged version, and `mvn install` it to install
the `-agent` JAR artifact into the local `.m2` repository, bypassing the need to access the GitHub Package registry.

The application can be packaged using:
```shell script
./mvnw package
```
It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

If you want to build an _über-jar_, execute the following command:
```shell script
./mvnw package -Dquarkus.package.type=uber-jar
```

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

Build a JVM mode OCI image:

`$BUILDER build -t quay.io/andrewazores/quarkus-test:$VERSION -f src/main/docker/Dockerfile.jvm .`

where `BUILDER` is ex. `podman`, `docker`.

## Creating a native executable

You can create a native executable using: 
```shell script
./mvnw package -Pnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: 
```shell script
./mvnw package -Pnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/code-with-quarkus-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/maven-tooling.html.

## Provided examples

### RESTEasy JAX-RS example

REST is easy peasy with this Hello World RESTEasy resource.

[Related guide section...](https://quarkus.io/guides/getting-started#the-jax-rs-resources)
