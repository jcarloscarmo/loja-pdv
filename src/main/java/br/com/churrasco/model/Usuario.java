package br.com.churrasco.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Usuario {
    private Integer id;
    private String nome;
    private String senha;
    private String perfil;

    public boolean isAdmin() {
        if (perfil == null) return false;

        String p = perfil.trim().toUpperCase(); // Remove espaços e põe em maiúsculo

        return p.equals("ADMIN") ||
                p.equals("DONO") ||
                p.equals("ADMINISTRADOR") ||
                p.equals("GERENTE") ||    // Adicionei Gerente
                p.equals("SUPERVISOR") || // Adicionei Supervisor
                p.equals("CHEFE");        // Adicionei Chefe
    }
}