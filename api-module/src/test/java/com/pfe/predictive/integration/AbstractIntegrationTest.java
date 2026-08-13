package com.pfe.predictive.integration;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
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
    // addressed by container name instead of by published port, avoiding
    // NAT entirely. Every other environment (a developer's machine, the
    // Nexus/Vercel/Render Jenkinsfile's Docker-agent stages) leaves this
    // unset and gets Testcontainers' normal, unmodified behavior.
    private static final String IT_DOCKER_NETWORK = System.getenv("IT_POSTGRES_DOCKER_NETWORK");
    private static final String IT_POSTGRES_CONTAINER_NAME = "sentinel-it-postgres";

    static final PostgreSQLContainer<?> POSTGRES = configureNetwork(
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("pfe_it")
                    .withUsername("pfe_it")
                    .withPassword("pfe_it"));

    private static PostgreSQLContainer<?> configureNetwork(PostgreSQLContainer<?> container) {
        if (IT_DOCKER_NETWORK == null || IT_DOCKER_NETWORK.isBlank()) {
            return container;
        }
        // withNetworkMode is the low-level escape hatch that joins an
        // EXISTING external network as-is, rather than Testcontainers'
        // withNetwork(Network)/withNetworkAliases() pair, which only manages
        // networks Testcontainers itself creates. Docker's embedded DNS on a
        // user-defined bridge resolves by container --name, not by
        // --hostname, so the name (not just the hostname) must be fixed here.
        return container
                .withNetworkMode(IT_DOCKER_NETWORK)
                .withCreateContainerCmdModifier(cmd -> cmd.withName(IT_POSTGRES_CONTAINER_NAME));
    }

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () ->
                (IT_DOCKER_NETWORK == null || IT_DOCKER_NETWORK.isBlank())
                        ? POSTGRES.getJdbcUrl()
                        : "jdbc:postgresql://" + IT_POSTGRES_CONTAINER_NAME + ":5432/pfe_it");
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
