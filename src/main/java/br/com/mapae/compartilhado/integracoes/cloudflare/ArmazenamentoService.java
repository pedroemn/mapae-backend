package br.com.mapae.compartilhado.integracoes.cloudflare;

import java.time.Duration;

public interface ArmazenamentoService {

    /**
     * Retorna a URL pública direta da CDN (ex: https://midias.mapae.com.br/locais/123.jpg).
     * O app baixa direto da CDN da Cloudflare com cache em borda.
     */
    String obterUrlPublica(String chaveObjeto);

    /**
     * Gera URL temporária pré-assinada para exibição de arquivos restritos (privados).
     */
    String gerarUrlPreAssinadaParaVisualizacao(String chaveObjeto, Duration duracao);

    /**
     * Gera uma URL de PUT para o app enviar qualquer foto (pública ou privada) diretamente ao storage.
     */
    String gerarUrlPreAssinadaParaUpload(String chaveObjeto, String contentType, Duration duracao);

    /**
     * Exclui o arquivo do storage.
     */
    void excluirArquivo(String chaveObjeto);
}

