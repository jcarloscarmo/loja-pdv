module br.com.churrasco {
    // Dependências necessárias
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.xerial.sqlitejdbc;
    requires org.slf4j;
    requires static lombok;

    // --- PERMISSÕES DE ACESSO (O Segredo está aqui) ---

    // Permite que o JavaFX mexa nas telas (.fxml)
    opens br.com.churrasco.view to javafx.fxml;

    // Permite que o JavaFX controle a classe PDVController
    opens br.com.churrasco.controller to javafx.fxml;

    // Permite que o JavaFX leia os dados da classe Produto (para tabelas)
    opens br.com.churrasco.model to javafx.base;

    // Exporta o pacote principal
    exports br.com.churrasco;
}