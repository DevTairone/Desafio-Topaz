package com.sys_top.desafio.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Contador global usado como base para a geração dos códigos curtos.
 *
 * Mantido como uma única linha (id fixo) na tabela url_counter. A leitura
 * e o incremento são feitos dentro de uma transação com lock pessimista de
 * escrita (ver UrlCounterDao), tornando a operação atômica também no nível
 * do banco.
 */
@Entity
@Table(name = "url_counter")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UrlCounter {

    @Id
    private Long id;

    @Column(name = "current_value", nullable = false)
    private Long currentValue;
}
