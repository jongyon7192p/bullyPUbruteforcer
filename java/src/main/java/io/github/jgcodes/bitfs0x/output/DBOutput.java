package io.github.jgcodes.bitfs0x.output;

import io.github.jgcodes.bitfs0x.util.FloatVector2;
import io.github.jgcodes.bitfs0x.util.FloatVector3;
import org.postgresql.ds.PGSimpleDataSource;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DBOutput implements Output {
  private static final String SQL_INSERT = """
    INSERT INTO bully_solutions \
    VALUES ('{?, ?, ?}', ?, ?, '{?, ?, ?}', '{?, ?, ?}, ?, ?, false, ?')
    """;

  private final Connection connection;
  private final PreparedStatement statement;

  /**
   * Opens a new database output. You will need to provide the url of the
   * <abbr title="Bowser in the Fire Sea SQL database">BitFSQL</abbr> server
   * in addition to the password for the "{@code bruteforcer}" user.
   * @param dbUrl url of the BitFSQL DB
   * @param password password
   */
  public DBOutput(String dbUrl, String password) throws SQLException {
    PGSimpleDataSource dataSource = new PGSimpleDataSource();
    dataSource.setServerNames(new String[] {dbUrl});
    dataSource.setDatabaseName("postgres");
    dataSource.setUser("bruteforcer");
    dataSource.setPassword(password);
    connection = dataSource.getConnection();
    statement = connection.prepareStatement(SQL_INSERT);
  }

  @Override
  public void output(FloatVector3 targetPos, int frame, FloatVector3 startPos, float startSpeed, short startYaw,
                     FloatVector3 finalPos, float finalSpeed, short finalYaw) throws SQLException {
    statement.setFloat(1, startPos.x());
    statement.setFloat(2, startPos.y());
    statement.setFloat(3, startPos.z());
    statement.setFloat(4, startSpeed);
    statement.setShort(5, startYaw);
    statement.setFloat(6, targetPos.x());
    statement.setFloat(7, targetPos.y());
    statement.setFloat(8, targetPos.z());
    statement.setFloat(9, finalPos.x());
    statement.setFloat(10, finalPos.y());
    statement.setFloat(11, finalPos.z());
    statement.setFloat(12, finalSpeed);
    statement.setShort(13, finalYaw);
    statement.setInt(14, frame);
    statement.execute();
  }

  @Override
  public void close() throws SQLException {
    statement.close();
    connection.close();
  }
}
