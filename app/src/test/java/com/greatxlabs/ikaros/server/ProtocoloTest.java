package com.greatxlabs.ikaros.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

public class ProtocoloTest {

    private Protocolo protocolo;
    private GestorSesiones gestor;
    private AccesoDatos accesoDatos;

    @BeforeEach
    public void setUp() {
        accesoDatos = null;
        gestor = new GestorSesiones(accesoDatos);
        protocolo = new Protocolo(gestor, accesoDatos);
    }

    @Test
    public void testLoginFallido() {
        String respuesta = protocolo.procesar("LOGIN|usuario|clave_erronea");
        assertEquals("ERROR|E02|Usuario o clave incorrectos", respuesta);
    }

    @Test
    public void testOperacionSinToken() {
        String respuesta = protocolo.procesar("LISTAR_TRIPULANTES");
        assertEquals("ERROR|E00|Sesión inválida o vencida", respuesta);
    }

    @Test
    public void testTokenInvalido() {
        String respuesta = protocolo.procesar("LISTAR_TRIPULANTES|token-falso");
        assertEquals("ERROR|E00|Sesión inválida o vencida", respuesta);
    }
}
