package br.com.mapae.compartilhado.seguranca;

import java.util.function.Supplier;
import org.springframework.dao.DataIntegrityViolationException;

/** Regras de cadastro comuns às contas de usuário e de loja. */
public final class Cadastro {

    private Cadastro() {
    }

    /** CEP aceito como 00000-000 ou 00000000; guarda só os dígitos. */
    public static String cepSoDigitos(String cep) {
        return cep.replace("-", "").trim();
    }

    /**
     * Salva a conta; se outro cadastro gravou o mesmo username ou email entre a checagem e o insert,
     * o índice único do banco recusa e a resposta é o mesmo 409.
     */
    public static <T> T salvarSemDuplicar(Supplier<T> salvar, String indiceUsername, String indiceEmail) {
        try {
            return salvar.get();
        } catch (DataIntegrityViolationException erro) {
            String mensagem = String.valueOf(erro.getMostSpecificCause().getMessage());
            if (mensagem.contains(indiceUsername)) {
                throw new ContaDuplicadaException("username");
            }
            if (mensagem.contains(indiceEmail)) {
                throw new ContaDuplicadaException("email");
            }
            throw erro;
        }
    }
}
