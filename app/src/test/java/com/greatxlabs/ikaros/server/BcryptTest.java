package com.greatxlabs.ikaros.server;

import at.favre.lib.crypto.bcrypt.BCrypt;

public class BcryptTest {
    public static void main(String[] args) {
        String[][] users = {
            {"Jefe", "Jefe123", "$2a$12$bQC20mQx63ZdleeRBMQ/8uvwU2by/GAd1mT1.6YhLSy/fO2RAAkwu"},
            {"RRHH", "RRHH123", "$2a$12$5xNos98TAXtX6kNLxENjSeWuBLgJ1XzK.IEWAHqk8S7BJpY68Imse"},
            {"Asignador", "Asignador123", "$2a$12$B32NbUT.Ol0n2OE7XPRtTOGgSsJIp5qujgJqCav1vR8LuZFvs/DK6"},
            {"Coordinador", "Coordinador123", "$2a$12$walY4/3Rk4.5nqKOZQNuVO1kD0.f1UeClqFce/N5OxSINurgwZG0q"},
            {"Registrador", "Registrador123", "$2a$12$rNX/F4iUwkDmWZuvnzCHh.8aikA7/8Ng6iQf63a9M37y75FZcAcIq"},
        };

        for (String[] u : users) {
            String name = u[0];
            String pwd = u[1];
            String hash = u[2];
            boolean valid = BCrypt.verifyer().verify(pwd.toCharArray(), hash).verified;
            System.out.println(name + " (" + pwd + "): " + (valid ? "OK" : "FAIL"));

            if (!valid) {
                String newHash = BCrypt.withDefaults().hashToString(12, pwd.toCharArray());
                System.out.println("  New hash: " + newHash);
                boolean newValid = BCrypt.verifyer().verify(pwd.toCharArray(), newHash).verified;
                System.out.println("  New hash verifies: " + newValid);
            }
        }
    }
}
