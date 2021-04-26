package io.github.jgcodes.bitfs0x.querier.dialog;

import io.github.jgcodes.bitfs0x.querier.dialog.LoginForm.LoginData;
import javafx.scene.control.Dialog;
import org.postgresql.ds.PGSimpleDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;

public class LoginForm extends Dialog<LoginData> {
  public static record LoginData(
    String hostURL, String dbName, String user, char[] password
    ) {
    public Connection connect() throws SQLException {
      PGSimpleDataSource dSrc = new PGSimpleDataSource();
      dSrc.setServerNames(new String[] {hostURL});
      dSrc.setDatabaseName(dbName);
      dSrc.setUser(user);
      dSrc.setPassword(new String(password));
      Arrays.fill(password, '\0');
      return dSrc.getConnection();
    }
  }
}
