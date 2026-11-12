package br.com.mapae.compartilhado;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Migrações novas levam nome e timestamp: V<yyyyMMddHHmmss>__<nome>.sql (AD-030).
 * V1 e V2 já foram aplicadas no Supabase e não podem ser renomeadas.
 */
class NomesDeMigracaoTests {

    private static final Pattern COM_TIMESTAMP = Pattern.compile("V\\d{14}__[a-z0-9_]+\\.sql");
    private static final Pattern LEGADA = Pattern.compile("V[12]__[a-z0-9_]+\\.sql");

    @Test
    void migracoesNovasUsamTimestampNoNome() throws IOException {
        try (Stream<Path> arquivos = Files.list(Path.of("src/main/resources/db/migration"))) {
            assertThat(arquivos.map(a -> a.getFileName().toString()))
                    .isNotEmpty()
                    .allMatch(n -> COM_TIMESTAMP.matcher(n).matches() || LEGADA.matcher(n).matches());
        }
    }

    /** Cada migração com timestamp registra a si mesma em public.migracao (AD-030). */
    @Test
    void migracoesNovasSeRegistramNaTabelaMigracao() throws IOException {
        try (Stream<Path> arquivos = Files.list(Path.of("src/main/resources/db/migration"))) {
            for (Path arquivo : arquivos.filter(a -> COM_TIMESTAMP.matcher(a.getFileName().toString()).matches()).toList()) {
                String nome = arquivo.getFileName().toString().replaceFirst("\\.sql$", "");
                assertThat(Files.readString(arquivo)).as(arquivo.getFileName().toString())
                        .contains("public.migracao").contains("'" + nome + "'");
            }
        }
    }
}
