package com.greatxlabs.ikaros.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para la clase Protocolo.
 * Verifica que el parseo de mensajes y las respuestas sean correctas.
 */
public class ProtocoloTest {

    private Protocolo protocolo;
    private GestorSesiones gestor;

    @BeforeEach
    public void setUp() {
        gestor = new GestorSesiones();
        protocolo = new Protocolo(gestor);
    }

    @Test
    public void testLoginExitoso() {
        String respuesta = protocolo.procesar("LOGIN|admin|1234");
        // Verificamos que la respuesta empiece con OK
        assertTrue(respuesta.startsWith("OK|"), "El login debería ser exitoso");
    }

    @Test
    public void testLoginFallido() {
        String respuesta = protocolo.procesar("LOGIN|usuario|clave_erronea");
        assertEquals("ERROR|E02|Usuario o clave incorrectos", respuesta);
    }

    @Test
    public void testOperacionSinToken() {
        String respuesta = protocolo.procesar("LISTAR_TRIPULANTES");
        assertEquals("ERROR|E00|Token requerido", respuesta);
    }

    @Test
    public void testTokenInvalido() {
        String respuesta = protocolo.procesar("LISTAR_TRIPULANTES|token-falso");
        assertEquals("ERROR|E00|Sesión inválida o vencida", respuesta);
    }
}
