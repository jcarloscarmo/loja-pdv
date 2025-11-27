package br.com.churrasco;

import br.com.churrasco.dao.ProdutoDAO;
import br.com.churrasco.model.Produto;
import br.com.churrasco.util.DatabaseConnection;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.List;

// Estendemos Application para o Java entender que é um app JavaFX
public class Main extends Application {

    public static void main(String[] args) {
        // Teste Rápido do Banco de Dados (Executa antes de abrir a janela)
        testarBancoDeDados();

        // Inicia a interface gráfica (por enquanto não tem tela, mas vai abrir vazio)
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Carrega o arquivo visual que criamos
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/com/churrasco/view/Menu.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("PDV Churrascaria - Versão 1.0");
        primaryStage.setScene(new Scene(root));
        primaryStage.setMaximized(true); // Abre em tela cheia
        primaryStage.show();
    }


    private static void testarBancoDeDados() {
        System.out.println("--- INICIANDO TESTE DE BANCO ---");

        try {
            // 1. Inicializa a tabela
            DatabaseConnection.getConnection();

            ProdutoDAO dao = new ProdutoDAO();

            // 2. Tenta Salvar um Produto (Picanha)
            // Só cria se não existir (para não duplicar a cada teste)
            if (dao.buscarPorCodigo("10") == null) {
                Produto picanha = new Produto(null, "10", "Picanha Importada", 55.0, 129.90, "KG", 0.0);
                dao.salvar(picanha);
                System.out.println("✅ Sucesso! Produto 'Picanha' salvo no banco.");
            } else {
                System.out.println("ℹ️ O produto 'Picanha' já existia no banco.");
            }

            // 3. Tenta Ler todos os produtos
            List<Produto> produtos = dao.listarTodos();
            System.out.println("\n📦 Lista de Produtos no Banco:");
            for (Produto p : produtos) {
                System.out.println(String.format(" -> [%s] %s | R$ %.2f/%s",
                        p.getCodigo(), p.getNome(), p.getPrecoVenda(), p.getUnidade()));
            }

        } catch (Exception e) {
            System.err.println("❌ Erro no teste de banco: ");
            e.printStackTrace();
        }
        System.out.println("--------------------------------");
    }
}