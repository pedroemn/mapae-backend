package br.com.mapae.locais.aplicacao;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Relógio do filtro "aberto agora"; os testes trocam por um relógio fixo. */
@Configuration(proxyBeanMethods = false)
public class RelogioConfig {

    @Bean
    Clock relogio() {
        return Clock.systemUTC();
    }
}
