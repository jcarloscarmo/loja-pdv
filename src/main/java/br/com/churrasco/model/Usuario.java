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
    private String perfil; // "DONO" ou "ATENDENTE"

    // Verifica se é administrador
    public boolean isAdmin() {
        return "DONO".equalsIgnoreCase(perfil);
    }
}