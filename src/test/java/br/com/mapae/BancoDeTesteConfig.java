package br.com.mapae;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Sobe um PostGIS em Docker, limpo como o Supabase: quem instala a extensão é a migração.
 */
@TestConfiguration(proxyBeanMethods = false)
public class BancoDeTesteConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer bancoPostgis() {
        return new PostgreSQLContainer(
                DockerImageName.parse("postgis/postgis:17-3.5").asCompatibleSubstituteFor("postgres"))
                .withCopyFileToContainer(
                        MountableFile.forHostPath("docker/banco/20_igualar_supabase.sql"),
                        "/docker-entrypoint-initdb.d/20_igualar_supabase.sql");
    }
}
