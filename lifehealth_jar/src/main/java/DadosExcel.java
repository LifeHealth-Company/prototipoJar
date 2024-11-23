import org.apache.poi.ss.usermodel.Row;

import java.sql.Connection;
import java.sql.SQLException;

public abstract class DadosExcel {

    public abstract void processarDados(Row linha);

    public abstract void inserirNoBanco(Connection conexao) throws SQLException;
}
