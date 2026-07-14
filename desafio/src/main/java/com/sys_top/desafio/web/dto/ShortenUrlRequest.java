package com.sys_top.desafio.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Corpo da requisição de encurtamento de URL.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShortenUrlRequest {

    private String url;
}
