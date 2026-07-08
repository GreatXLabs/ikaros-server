package com.greatxlabs.ikaros.server;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DominioTest {

    @Test
    void usuarioEstaInactivo() {
        Usuario u = new Usuario();
        assertFalse(u.estaInactivo());
        u.desactivar();
        assertTrue(u.estaInactivo());
    }

    @Test
    void usuarioDesactivarCambiaEstado() {
        Usuario u = new Usuario();
        u.desactivar();
        assertEquals(2, u.getEstadoUID());
    }

    @Test
    void tripulanteEstaRetirado() {
        Tripulante t = new Tripulante();
        assertFalse(t.estaRetirado());
        t.retirar();
        assertTrue(t.estaRetirado());
    }

    @Test
    void tripulanteRetirarCambiaEstado() {
        Tripulante t = new Tripulante();
        t.retirar();
        assertEquals(3, t.getEstadoTID());
    }

    @Test
    void misionEstaTerminada() {
        Mision m = new Mision();
        assertFalse(m.estaTerminada());
        m.setEstadoMID(4);
        assertTrue(m.estaTerminada());
        m.setEstadoMID(5);
        assertTrue(m.estaTerminada());
    }

    @Test
    void eventoDesestimarCambiaEstado() {
        Evento e = new Evento();
        e.desestimar();
        assertEquals(2, e.getEstadoEID());
    }

    @Test
    void sesionHaExpirado() {
        Sesion s = new Sesion(1, "JEFE");
        assertFalse(s.haExpirado());
    }

    @Test
    void sesionRenovar() {
        Sesion s = new Sesion(1, "JEFE");
        long antes = s.getUltimaActividad();
        s.renovar();
        assertTrue(s.getUltimaActividad() >= antes);
    }

    @Test
    void registroConvenienceConstructor() {
        Registro r = new Registro(1, 2, 3, 4, 5, "test");
        assertEquals(1, r.getRegistroID());
        assertEquals(2, r.getAccionMID());
        assertEquals(3, r.getUsuarioID());
        assertEquals(4, r.getTipoEntidadID());
        assertEquals(5, r.getEntidadID());
        assertEquals("test", r.getDescripcion());
        assertNotNull(r.getFechaHora());
    }

    @Test
    void jsonKeysPreservanPascalCase() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Usuario u = new Usuario();
        u.setUsuarioID(1);
        u.setRolID(2);
        u.setEstadoUID(1);
        u.setNombre("Test");
        u.setApellido("User");
        u.setUsuario("tuser");
        u.setClave("hash");

        String json = mapper.writeValueAsString(u);
        assertTrue(json.contains("\"UsuarioID\""), "Should contain UsuarioID");
        assertTrue(json.contains("\"RolID\""), "Should contain RolID");
        assertTrue(json.contains("\"EstadoUID\""), "Should contain EstadoUID");
        assertTrue(json.contains("\"Nombre\""), "Should contain Nombre");
        assertTrue(json.contains("\"Apellido\""), "Should contain Apellido");
        assertTrue(json.contains("\"Usuario\""), "Should contain Usuario");
        assertTrue(json.contains("\"Clave\""), "Should contain Clave");
    }
}
