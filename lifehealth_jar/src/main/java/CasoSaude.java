import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class CasoSaude extends DadosExcel {
    private Integer ano;
    private String ufNotificacao;
    private String estadoNotificacao;
    private Integer anoNascPaciente;
    private String sexoPaciente;
    private String isPacienteGestante;
    private String sorotipo;
    private String evolucaoCaso;


    @Override
    public void processarDados(Row linha) {

        this.ano = getCellValueAsInteger(linha.getCell(0));

        this.ufNotificacao = getCellValueAsString(linha.getCell(1));

        this.estadoNotificacao = getCellValueAsString(linha.getCell(2));

        this.anoNascPaciente = getCellValueAsInteger(linha.getCell(3));

        this.sexoPaciente = getCellValueAsString(linha.getCell(4));

        this.isPacienteGestante = getCellValueAsString(linha.getCell(5));

        this.sorotipo = getCellValueAsString(linha.getCell(6));

        this.evolucaoCaso = getCellValueAsString(linha.getCell(7));
    }

    @Override
    public void inserirNoBanco(Connection conexao) throws SQLException {

        String tabelaCasos = "INSERT INTO casos (ano, ufNotificacao, estadoNotificacao, anoNascPaciente, sexoPaciente, " +
                "isPacienteGestante, sorotipo, evolucaoCaso) VALUES(?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement executarCasos = conexao.prepareStatement(tabelaCasos)) {

            executarCasos.setInt(1, this.ano);
            executarCasos.setString(2, this.ufNotificacao);
            executarCasos.setString(3, this.estadoNotificacao);
            executarCasos.setInt(4, this.anoNascPaciente);
            executarCasos.setString(5, this.sexoPaciente);
            executarCasos.setString(6, this.isPacienteGestante);
            executarCasos.setString(7, this.sorotipo);
            executarCasos.setString(8, this.evolucaoCaso);

            executarCasos.executeUpdate();

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

            default:
                return "";
        }
    }
    private static Integer getCellValueAsInteger(Cell cell) {

        if (cell == null) {
            return 0;
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

            default:
                return 0;
        }
    }
}

