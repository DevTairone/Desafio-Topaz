package com.sys_top.desafio.application;

import com.sys_top.desafio.domain.model.UrlCounter;
import com.sys_top.desafio.infrastructure.persistence.UrlCounterDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UrlCounterServiceTest {

    private UrlCounterDao urlCounterDao;
    private UrlCounterService service;

    @BeforeEach
    void setUp() {
        urlCounterDao = mock(UrlCounterDao.class);
        service = new UrlCounterService(urlCounterDao);
    }

    @Test
    void deveCriarLinhaDoContadorSeNaoExistir() {
        when(urlCounterDao.existsById(1L)).thenReturn(false);

        service.ensureCounterExists();

        ArgumentCaptor<UrlCounter> captor = ArgumentCaptor.forClass(UrlCounter.class);
        verify(urlCounterDao).save(captor.capture());
        assertEquals(1L, captor.getValue().getId());
        assertEquals(0L, captor.getValue().getCurrentValue());
    }

    @Test
    void naoDeveRecriarLinhaDoContadorSeJaExistir() {
        when(urlCounterDao.existsById(1L)).thenReturn(true);

        service.ensureCounterExists();

        verify(urlCounterDao, never()).save(any());
    }

    @Test
    void deveIncrementarValorDeFormaSequencial() {
        UrlCounter counter = UrlCounter.builder().id(1L).currentValue(0L).build();
        when(urlCounterDao.findByIdForUpdate(1L)).thenReturn(Optional.of(counter));
        when(urlCounterDao.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals(1L, service.nextValue());
        assertEquals(2L, service.nextValue());
        assertEquals(3L, service.nextValue());
    }

    @Test
    void deveFalharSeContadorNaoEstiverInicializado() {
        when(urlCounterDao.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, service::nextValue);
    }
}
