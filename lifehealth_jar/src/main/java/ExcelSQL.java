import java.io.*;
import java.sql.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.Level;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.core.exception.SdkException;

public class ExcelSQL {
    private static final int LINHAS_POR_EXECUCAO = 100;
    private static final String CONTADOR_ARQUIVO = "contador.txt";
    private static final String BUCKET_NAME = "lifehealth-bucket";
    private static final String FILE_KEY = "lifeHealthBase.xlsx";
    private static final Logger logger = Logger.getLogger(ExcelSQL.class.getName());

    public static void main(String[] args) {

        try {
            // Cria um FileHandler que irá gravar os logs no arquivo 'logfile.log', permitindo anexar novas entradas
            FileHandler fileHandler = new FileHandler("logfile.log", true);  // O segundo parâmetro é para 'append' (acrescentar)
            fileHandler.setFormatter(new SimpleFormatter());  // Define o formato simples para os logs
            logger.addHandler(fileHandler);  // Adiciona o FileHandler ao logger

            // Configura o nível de log para INFO (para capturar todas as mensagens de INFO e superiores)
            logger.setLevel(Level.ALL);  // 'ALL' captura todos os níveis de log (FINEST até SEVERE)

        } catch (IOException e) {
            e.printStackTrace();
        }

        String urlMySQL = System.getenv("MYSQL_URL");
        String usuario = System.getenv("MYSQL_USER");
        String senha = System.getenv("MYSQL_PASSWORD");

        try (Connection conexao = DriverManager.getConnection(urlMySQL, usuario, senha);
             S3Client s3Client = new S3Provider().getS3Client()) {

            // Mensagem de Log com nível INFO
            logger.info("Conexão com o banco de dados estabelecida.");
            logger.info("Iniciando leitura do arquivo Excel.");

            // Baixando e lendo o arquivo do S3 diretamente
            try (InputStream leitorExcel = baixarArquivoS3(s3Client, BUCKET_NAME, FILE_KEY);
                 Workbook planilha = new XSSFWorkbook(leitorExcel)) {

                Sheet tabela = planilha.getSheetAt(0);
                int ultimaLinhaLida = lerContador();

                logger.info("Iniciando a inserção de dados...");

                for (int linhaAtual = ultimaLinhaLida + 1; linhaAtual <= ultimaLinhaLida + LINHAS_POR_EXECUCAO; linhaAtual++) {

                    if (linhaAtual > tabela.getLastRowNum()) {
                        logger.info("Não há mais linhas para processar.");
                        break;
                    }

                    Row linha = tabela.getRow(linhaAtual);
                    System.out.println("Processando linha " + (linhaAtual + 1));

                    CasoSaude casoSaude = new CasoSaude();
                    casoSaude.processarDados(linha);
                    casoSaude.inserirNoBanco(conexao);
                }

                // Atualiza o contador
                atualizarContador(ultimaLinhaLida + LINHAS_POR_EXECUCAO);
                logger.info("Dados inseridos com êxito!");

            } catch (SdkException e) { // Capturando as exceções do SDK (AWS)
                logger.severe("Erro ao processar o arquivo Excel.");
                e.printStackTrace();
            } catch (IOException e) {
                logger.severe("Erro de I/O ao processar o arquivo Excel.");
                e.printStackTrace();
            }

        } catch (SQLException e) {
            logger.severe("Erro ao conectar no banco de dados.");
            e.printStackTrace();
        }
    }

    // Função para baixar o arquivo do S3
    public static InputStream baixarArquivoS3(S3Client s3Client, String bucketName, String key) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        logger.fine("Tentando baixar o arquivo: " + key + " do bucket: " + bucketName);
        return s3Client.getObject(request);
    }

    // Método para ler o contador
    private static int lerContador() {
        try (BufferedReader br = new BufferedReader(new FileReader(CONTADOR_ARQUIVO))) {
            String linha = br.readLine();
            return (linha != null && !linha.isEmpty()) ? Integer.parseInt(linha) : 0;
        } catch (IOException e) {
            logger.warning("Não foi possível ler o contador. Assumindo que o contador é 0.");
            return 0;  // Se não conseguir ler o arquivo, assume-se que o contador é 0
        }
    }

    // Método para atualizar o contador
    private static void atualizarContador(int novaLinha) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(CONTADOR_ARQUIVO))) {
            bw.write(String.valueOf(novaLinha));
            logger.fine("Contador atualizado para: " + novaLinha);
        } catch (IOException e) {
            logger.severe("Erro ao atualizar o contador.");
            e.printStackTrace();
        }
    }
}
