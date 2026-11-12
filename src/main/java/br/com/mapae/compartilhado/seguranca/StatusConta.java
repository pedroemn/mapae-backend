package br.com.mapae.compartilhado.seguranca;

/** Toda conta nasce ATIVO; só contas ATIVO recebem token no login. */
public enum StatusConta {
    ATIVO,
    INATIVO,
    BLOQUEADO
}
