
package com.bevans.exception;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ResponseEntity<Object> resposta(
            HttpStatus status,
            String mensagem,
            WebRequest request
    ) {
        Map<String, Object> body = new LinkedHashMap<>();

        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("erro", mensagem);
        body.put(
                "caminho",
                request.getDescription(false).replace("uri=", "")
        );

        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> acessoNegado(
            AccessDeniedException ex,
            WebRequest request
    ) {
        return resposta(
                HttpStatus.FORBIDDEN,
                "Acesso negado ao recurso.",
                request
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Object> naoAutenticado(
            AuthenticationException ex,
            WebRequest request
    ) {
        return resposta(
                HttpStatus.UNAUTHORIZED,
                "Autenticação obrigatória.",
                request
        );
    }

    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<Object> requisicaoInvalida(
            Exception ex,
            WebRequest request
    ) {
        return resposta(
                HttpStatus.BAD_REQUEST,
                "Parâmetro ou corpo da requisição inválido.",
                request
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Object> metodoNaoPermitido(
            HttpRequestMethodNotSupportedException ex,
            WebRequest request
    ) {
        return resposta(
                HttpStatus.METHOD_NOT_ALLOWED,
                "Método HTTP não permitido para este endpoint.",
                request
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> argumentoInvalido(
            IllegalArgumentException ex,
            WebRequest request
    ) {
        return resposta(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(TurmaComRotaAtivaException.class)
    public ResponseEntity<Object> turmaComRotaAtiva(
            TurmaComRotaAtivaException ex,
            WebRequest request
    ) {
        return resposta(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> erroInterno(
            Exception ex,
            WebRequest request
    ) {
        log.error(
                "Erro não tratado em {}",
                request.getDescription(false),
                ex
        );

        return resposta(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno do servidor.",
                request
        );
    }
}
