package io.github.jgcodes.bitfs0x.dialog;

import org.postgresql.ds.PGSimpleDataSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import static io.github.jgcodes.bitfs0x.util.Varargs.array;

public class MinimalExample {
  static {
    try {
      Class.forName("org.postgresql.Driver");
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) throws SQLException {
    PGSimpleDataSource dataSrc = new PGSimpleDataSource();
    dataSrc.setServerNames(array("bitfs.ctauex7nk6ib.us-west-2.rds.amazonaws.com:65432"));
    dataSrc.setDatabaseName("postgres");
    dataSrc.setUser("bruteforcer");
    dataSrc.setPassword("Pdt3+Y8r5K>@aJ%[w7t:");

    try (Connection c = dataSrc.getConnection()) {
      try (Statement s = c.createStatement()) {
        s.execute("SELECT * FROM bully_solutions");
      }
    }
  }
}
