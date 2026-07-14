package com.sys_top.desafio.engine;

import com.sys_top.desafio.domain.model.UrlCounter;
import com.sys_top.desafio.domain.repository.UrlCounterRepository;
import com.sys_top.desafio.service.UrlCounterService;
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

    private UrlCounterRepository repository;
    private UrlCounterService service;

    @BeforeEach
    void setUp() {
        repository = mock(UrlCounterRepository.class);
        service = new UrlCounterService(repository);
    }

    @Test
    void deveCriarLinhaDoContadorSeNaoExistir() {
        when(repository.existsById(1L)).thenReturn(false);

        service.ensureCounterExists();

        ArgumentCaptor<UrlCounter> captor = ArgumentCaptor.forClass(UrlCounter.class);
        verify(repository).save(captor.capture());
        assertEquals(1L, captor.getValue().getId());
        assertEquals(0L, captor.getValue().getCurrentValue());
    }

    @Test
    void naoDeveRecriarLinhaDoContadorSeJaExistir() {
        when(repository.existsById(1L)).thenReturn(true);

        service.ensureCounterExists();

        verify(repository, never()).save(any());
    }

    @Test
    void deveIncrementarValorDeFormaSequencial() {
        UrlCounter counter = UrlCounter.builder().id(1L).currentValue(0L).build();
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(counter));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals(1L, service.nextValue());
        assertEquals(2L, service.nextValue());
        assertEquals(3L, service.nextValue());
    }

    @Test
    void deveFalharSeContadorNaoEstiverInicializado() {
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, service::nextValue);
    }
}
