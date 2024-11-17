import java.io.*;
import java.sql.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.S3Client;

public class ExcelSQL {
    private static final int LINHAS_POR_EXECUCAO = 100;
    private static final String CONTADOR_ARQUIVO = "contador.txt"; // Onde sera armazenado a ultima linha lida pelo for
    private static final String BUCKET_NAME = "lifehealth-bucket"; // Nome do bucket
    private static final String FILE_KEY = "lifeHealthBase_V1.xlsx"; // Nome do arquivo no bucket

    public static void main(String[] args) {
        String urlMySQL = "jdbc:mysql://3.86.163.50:3306/lifeHealth";
        String usuario = "admUser";
        String senha = "lifeHealth123";

        // Instanciando o S3Provider
        S3Provider s3Provider = new S3Provider();
        S3Client s3Client = s3Provider.getS3Client();

        try (Connection conexao = DriverManager.getConnection(urlMySQL, usuario, senha)) {
            System.out.println("Conexão com o banco de dados estabelecida.");
            System.out.println("Iniciando leitura do arquivo Excel.");

            // Baixando o arquivo do S3
            InputStream leitorExcel = baixarArquivoS3(s3Client, BUCKET_NAME, FILE_KEY);
            Workbook planilha = new XSSFWorkbook(leitorExcel);
            Sheet tabela = planilha.getSheetAt(0);

            int ultimaLinhaLida = lerContador();

            System.out.println("INFO - Iniciando a inserção de dados...");
            System.out.println("Aguarde, Isso pode durar alguns segundos.");

            for (int linhaAtual = ultimaLinhaLida + 1; linhaAtual < ultimaLinhaLida + 1 + LINHAS_POR_EXECUCAO; linhaAtual++) {
                if (linhaAtual > tabela.getLastRowNum()) {
                    System.out.println("Não há mais linhas para processar.");
                    break;
                }
                Row linha = tabela.getRow(linhaAtual);
                System.out.println("INFO - Inserindo linha " + (linhaAtual + 1) + " do arquivo Excel ao Banco de Dados");

                // Campos da Tabela Matriz
                Integer ano = getCellValueAsInteger(linha.getCell(0));
                String ufNotificacao = getCellValueAsString(linha.getCell(1));
                String estadoNotificacao = getCellValueAsString(linha.getCell(2));

                Integer anoNascPaciente = getCellValueAsInteger(linha.getCell(3));
                String sexoPaciente = getCellValueAsString(linha.getCell(4));
                String isPacienteGestante = getCellValueAsString(linha.getCell(5));
//                String dataInternacao = getCellValueAsString(linha.getCell(6));

                String sorotipo = getCellValueAsString(linha.getCell(7));
                String evolucaoCaso = getCellValueAsString(linha.getCell(8));
//                String dataObito = getCellValueAsString(linha.getCell(9));

                Integer idadePaciente = 2024 - anoNascPaciente;

                // Insere na tabela ConsumoDados
                String tabelaCasos = "INSERT INTO casos (ano, ufNotificacao, estadoNotificacao, anoNascPaciente, sexoPaciente, isPacienteGestante, sorotipo, evolucaoCaso) VALUES(?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement executarCasos = conexao.prepareStatement(tabelaCasos)){
                    executarCasos.setInt(1, ano);
                    executarCasos.setString(2, ufNotificacao );
                    executarCasos.setString(3, estadoNotificacao);
                    executarCasos.setInt(4, anoNascPaciente);
                    executarCasos.setString(5, sexoPaciente);
                    executarCasos.setString(6, isPacienteGestante);
//                    executarCasos.setString(7, dataInternacao);
                    executarCasos.setString(8, sorotipo);
                    executarCasos.setString(9, evolucaoCaso);
//                    executarCasos.setString(10,dataObito);

                    executarCasos.executeUpdate();
                }
            }
            atualizarContador(ultimaLinhaLida + LINHAS_POR_EXECUCAO);
            planilha.close();
            System.out.println("SUCESSO - Dados inseridos com êxito!");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("FALHA - Houve um erro ao inserir os dados.");
        }
    }

    // Função para baixar arquivo do S3
    private static InputStream baixarArquivoS3(S3Client s3Client, String bucketName, String key) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        return s3Client.getObject(request);
    }

    // Método para reiniciar o contador
    private static void reiniciarContador() {
        atualizarContador(0); // Define o contador para 0
    }

    // Método para ler o contador do arquivo
    private static int lerContador() {
        try (BufferedReader br = new BufferedReader(new FileReader(CONTADOR_ARQUIVO))) {
            String linha = br.readLine();
            return linha != null ? Integer.parseInt(linha) : 0; // Retorna 0 se o arquivo estiver vazio
        } catch (IOException e) {
            return 0; // Retorna 0 se houver erro ao ler o arquivo
        }
    }

    // Método para atualizar o contador no arquivo
    private static void atualizarContador(int novaLinha) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(CONTADOR_ARQUIVO))) {
            bw.write(String.valueOf(novaLinha));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf((int) cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
                return " ";
            default:
                return "";
        }
    }
    private static Integer getCellValueAsInteger(Cell cell) {
        if (cell == null) {
            return 0;  // Retorna 0 se a célula estiver vazia
        }
        switch (cell.getCellType()) {
            case NUMERIC:
                return (int) cell.getNumericCellValue();
            case STRING:
                try {
                    return Integer.parseInt(cell.getStringCellValue());
                } catch (NumberFormatException e) {
                    return 0;
                }
            case BOOLEAN:
                return cell.getBooleanCellValue() ? 1 : 0;
            case BLANK:
                return 0;
            case FORMULA:
                // Aqui, você pode optar por calcular a fórmula e obter o valor inteiro
                try {
                    return (int) cell.getNumericCellValue();  // Assumindo que a fórmula resulta em um valor numérico
                } catch (Exception e) {
                    return 0;  // Retorna 0 em caso de erro
                }
            default:
                return 0;  // Retorna 0 para outros tipos
        }
    }
}
