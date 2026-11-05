package br.com.mapae;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularidadeTests {

    @Test
    void modulosRespeitamAsFronteiras() {
        ApplicationModules.of(MapaeApplication.class).verify();
    }
}
