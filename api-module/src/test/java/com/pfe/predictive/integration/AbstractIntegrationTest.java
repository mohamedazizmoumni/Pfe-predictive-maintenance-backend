package com.pfe.predictive.integration;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for *IT.java integration tests. Boots the full Spring context on a
 * random port against a real Postgres container (Testcontainers) so Flyway runs
 * the actual migration set and repositories/queries are exercised for real,
 * instead of mocking the datasource like the unit tests under **&#47;*Test.java do.
 * Requests go through MockMvc (real DispatcherServlet + Spring Security filter
 * chain, no mocked layers) rather than a real socket - see the comment on
 * POSTGRES below for why TestRestTemplate/raw sockets were dropped.
 *
 * <p>Only picked up by `mvn verify` (maven-failsafe-plugin) - `mvn test`
 * (surefire) is scoped to *Test.java/*Tests.java and skips these, so a plain
 * unit-test run never needs Docker.
 */
@Tag("integration")
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    // Singleton container pattern (see the Testcontainers docs' "Singleton
    // containers" section) - started once here and never explicitly stopped;
    // the Ryuk reaper kills it when the JVM exits. Deliberately NOT using
    // @Container/@Testcontainers: that extension scopes its start/stop
    // lifecycle per test CLASS even for a field inherited from this shared
    // base, so under failsafe's default single-JVM-reused-across-classes run
    // it was stopping the container after the first *IT class finished and
    // spinning up a brand new one (new port, empty schema) for the next -
    // every class after the first then failed with connection-refused once
    // the old container was gone. A plain static field + manual .start()
    // sidesteps that lifecycle entirely and is genuinely shared.
    //
    // Real HTTP via TestRestTemplate was also tried and dropped: it hit a
    // JDK HttpURLConnection quirk ("cannot retry due to server
    // authentication, in streaming mode") against the embedded Tomcat in
    // this environment on POST requests, even though the server processed
    // them successfully. MockMvc still drives the real DispatcherServlet and
    // Spring Security filter chain end-to-end without that networking layer.
    //
    // IT_POSTGRES_DOCKER_NETWORK: unset everywhere except the Jenkins-in-
    // Docker CI (sentinel-devops/Jenkinsfile.ci-local), where the JVM running
    // these tests is itself inside a container reached only via a Docker
    // socket mount (DooD). There, Testcontainers' normal host+mapped-port
    // addressing hits a Docker hairpin-NAT limitation: a sibling container
    // cannot reliably reach another sibling's published port back through
    // the bridge gateway - confirmed in that CI as a flat "Connection
    // refused" at the gateway IP, identical whether the host was
    // auto-detected by Testcontainers or computed manually. When this env
    // var is set, Postgres joins that same named network directly and is
    // addressed by network alias instead of by published port, avoiding NAT
    // entirely. Every other environment (a developer's machine, the
    // Nexus/Vercel/Render Jenkinsfile's Docker-agent stages) leaves this
    // unset and gets Testcontainers' normal, unmodified behavior.
    //
    // First attempt used withNetworkMode(String) - the low-level escape
    // hatch - and was ruled out by direct comparison: `docker run` with the
    // identical image/network/env by hand reached "database system is ready
    // to accept connections" in under a second, but the same config driven
    // through withNetworkMode() made Testcontainers' own LogMessageWaitStrategy
    // never see that line and fail after a few seconds - an interaction bug
    // between withNetworkMode() and Testcontainers' own log-follow/wait
    // machinery, not a Docker or Postgres problem. Wrapping the EXISTING
    // network through Testcontainers' own Network abstraction (its normal,
    // fully-supported path for custom networks - ordinarily used for ones it
    // creates itself) and joining via withNetwork()+withNetworkAliases()
    // instead keeps port publishing, log streaming, and the wait strategy on
    // their regular, working code path.
    private static final String IT_DOCKER_NETWORK = System.getenv("IT_POSTGRES_DOCKER_NETWORK");
    private static final String IT_POSTGRES_NETWORK_ALIAS = "sentinel-it-postgres";

    static final PostgreSQLContainer<?> POSTGRES = configureNetwork(
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("pfe_it")
                    .withUsername("pfe_it")
                    .withPassword("pfe_it"));

    private static PostgreSQLContainer<?> configureNetwork(PostgreSQLContainer<?> container) {
        if (IT_DOCKER_NETWORK == null || IT_DOCKER_NETWORK.isBlank()) {
            return container;
        }
        String networkId = DockerClientFactory.lazyClient()
                .listNetworksCmd()
                .withNameFilter(IT_DOCKER_NETWORK)
                .exec()
                .stream()
                .filter(n -> IT_DOCKER_NETWORK.equals(n.getName()))
                .findFirst()
                .map(com.github.dockerjava.api.model.Network::getId)
                .orElseThrow(() -> new IllegalStateException(
                        "Docker network '" + IT_DOCKER_NETWORK + "' not found - required when IT_POSTGRES_DOCKER_NETWORK is set"));

        // Network.builder().id(networkId) does NOT wrap the existing network -
        // verified by decompiling NetworkImpl.getId(): it unconditionally calls
        // its own create() (a real `docker network create`) the first time
        // getId() is invoked and overwrites the preset id field with the new
        // response, regardless of what .id() was given. Confirmed in CI: the
        // postgres container ended up on a brand-new ad-hoc network (random
        // name, different subnet from devops_devops-net) that the jenkins
        // container was never attached to, causing UnknownHostException for
        // the alias even though the alias itself was applied correctly - just
        // on the wrong network. The only way to truly reuse a pre-existing
        // network is to implement the Network interface directly and return
        // the real id from getId(), bypassing NetworkImpl entirely.
        Network existingNetwork = new Network() {
            @Override
            public String getId() {
                return networkId;
            }

            @Override
            public void close() {
                // no-op: devops_devops-net is externally managed by
                // docker-compose - Testcontainers must never create, own, or
                // remove it.
            }

            @Override
            public org.junit.runners.model.Statement apply(org.junit.runners.model.Statement base, org.junit.runner.Description description) {
                return base;
            }
        };

        return container
                .withNetwork(existingNetwork)
                .withNetworkAliases(IT_POSTGRES_NETWORK_ALIAS);
    }

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () ->
                (IT_DOCKER_NETWORK == null || IT_DOCKER_NETWORK.isBlank())
                        ? POSTGRES.getJdbcUrl()
                        : "jdbc:postgresql://" + IT_POSTGRES_NETWORK_ALIAS + ":5432/pfe_it");
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);

        // JwtTokenProvider refuses to start without a real (non-placeholder,
        // >=32 char) secret - see api-module config.provider.JwtTokenProvider.
        // Access tokens are signed with HS512, which needs a key >= 512 bits
        // (64 bytes) per RFC 7518 3.2 - JwtTokenProvider's own >=32-byte
        // floor is too low for that algorithm, so this has to be longer
        // than the minimum it enforces or token generation throws
        // WeakKeyException at request time (login endpoint then reports it
        // as a generic 401, which is what first surfaced this).
        registry.add("jwt.secret", () ->
                "integration-test-jwt-signing-key-do-not-use-in-prod-must-be-at-least-64-bytes-long-for-hs512");

        // No real ML/mail services running in the IT container - point at
        // something harmless rather than localhost:8000, which would just be
        // a slow connection-refused on every retry.
        registry.add("ml.internal-api-key", () -> "it-test-key");
    }

    @LocalServerPort
    protected int port;

    protected String baseUrl() {
        return "http://localhost:" + port;
    }
}
