import java.io.*;
import java.sql.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.core.exception.SdkException;

public class ExcelSQL {
    private static final int LINHAS_POR_EXECUCAO = 60000;
    private static final String CONTADOR_ARQUIVO = "contador.txt";
    private static final String BUCKET_NAME = "lifehealth-bucket";
    private static final String FILE_KEY = "lifeHealthBase.xlsx";

    public static void main(String[] args) {

        String urlMySQL = System.getenv("MYSQL_URL");
        String usuario = System.getenv("MYSQL_USER");
        String senha = System.getenv("MYSQL_PASSWORD");

        try (Connection conexao = DriverManager.getConnection(urlMySQL, usuario, senha);
             S3Client s3Client = new S3Provider().getS3Client()) {

            System.out.println("Conexão com o banco de dados estabelecida.");
            System.out.println("Iniciando leitura do arquivo Excel.");

            // Baixando e lendo o arquivo do S3 diretamente
            try (InputStream leitorExcel = baixarArquivoS3(s3Client, BUCKET_NAME, FILE_KEY);

                 Workbook planilha = new XSSFWorkbook(leitorExcel)) {

                Sheet tabela = planilha.getSheetAt(0);
                int ultimaLinhaLida = lerContador();

                System.out.println("INFO - Iniciando a inserção de dados...");

                for (int linhaAtual = ultimaLinhaLida + 1; linhaAtual <= ultimaLinhaLida + LINHAS_POR_EXECUCAO; linhaAtual++) {

                    if (linhaAtual > tabela.getLastRowNum()) {
                        System.out.println("Não há mais linhas para processar.");
                        break;
                    }

                    Row linha = tabela.getRow(linhaAtual);
                    System.out.println("INFO - Processando linha " + (linhaAtual + 1));

                    CasoSaude casoSaude = new CasoSaude();

                    casoSaude.processarDados(linha);

                    casoSaude.inserirNoBanco(conexao);
                }

                // Atualiza o contador
                atualizarContador(ultimaLinhaLida + LINHAS_POR_EXECUCAO);
                System.out.println("SUCESSO - Dados inseridos com êxito!");

            } catch (SdkException e) { // Capturando as exceções do SDK (AWS)

                System.out.println("FALHA - Erro ao processar o arquivo Excel.");
                e.printStackTrace();

            } catch (IOException e) {

                System.out.println("FALHA - Erro de I/O ao processar o arquivo Excel.");
                e.printStackTrace();
            }

        } catch (SQLException e) {

            System.out.println("FALHA - Erro ao conectar no banco de dados.");
            e.printStackTrace();
        }
    }

    // Função para baixar o arquivo do S3
    private static InputStream baixarArquivoS3(S3Client s3Client, String bucketName, String key) {

        return s3Client.getObject(GetObjectRequest.builder().bucket(bucketName).key(key).build());
    }

    // Método para ler o contador
    private static int lerContador() {

        try (BufferedReader br = new BufferedReader(new FileReader(CONTADOR_ARQUIVO))) {
            String linha = br.readLine();  // Lê a linha uma única vez

            // Verifica se a linha não é nula ou vazia antes de tentar converter
            return (linha != null && !linha.isEmpty()) ? Integer.parseInt(linha) : 0;

        } catch (IOException e) {
            return 0;  // Se não conseguir ler o arquivo, assume-se que o contador é 0
        }
    }

    // Método para atualizar o contador
    private static void atualizarContador(int novaLinha) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(CONTADOR_ARQUIVO))) {
            bw.write(String.valueOf(novaLinha));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
