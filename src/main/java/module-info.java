module br.com.churrasco {
    // Interface Gráfica
    requires javafx.controls;
    requires javafx.fxml;

    // Banco de Dados
    requires java.sql;
    requires org.xerial.sqlitejdbc;

    // Utilitários
    requires org.slf4j;
    requires static lombok;

    // --- A LINHA QUE FALTA (Permissão para a Balança) ---
    requires com.fazecast.jSerialComm;
    requires java.desktop;
    requires escpos.coffee;

    // --- PERMISSÕES DE ACESSO ---

    // Permite que o JavaFX mexa nas telas (.fxml)
    opens br.com.churrasco.view to javafx.fxml;

    // Permite que o JavaFX controle os Controllers
    opens br.com.churrasco.controller to javafx.fxml;

    // Permite que o JavaFX leia os dados das Classes (para tabelas)
    opens br.com.churrasco.model to javafx.base;

    // Exporta o pacote principal
    exports br.com.churrasco;
}