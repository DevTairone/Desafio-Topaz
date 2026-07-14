package com.sys_top.desafio.infrastructure.web;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * Ativa o JAX-RS para a aplicação inteira. Combinado com o context-root "/"
 * definido em jboss-web.xml, os recursos ficam expostos direto na raiz do
 * servidor (ex.: GET /{codigo}), sem prefixo extra de path.
 */
@ApplicationPath("/")
public class JaxRsActivator extends Application {
}
