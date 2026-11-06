
package com.bevans.exception;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(
                    GlobalExceptionHandler.class
            );

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
                request.getDescription(false)
                        .replace("uri=", "")
        );

        return ResponseEntity
                .status(status)
                .body(body);
    }

    @ExceptionHandler(
            MissingServletRequestParameterException.class
    )
    public ResponseEntity<Object> parametroObrigatorio(
            MissingServletRequestParameterException ex,
            WebRequest request
    ) {
        return resposta(
                HttpStatus.BAD_REQUEST,
                "Parâmetro obrigatório ausente: "
                        + ex.getParameterName(),
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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> erroInterno(
            Exception ex,
            WebRequest request
    ) {
        log.error(
                "Erro não tratado na API: {}",
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
