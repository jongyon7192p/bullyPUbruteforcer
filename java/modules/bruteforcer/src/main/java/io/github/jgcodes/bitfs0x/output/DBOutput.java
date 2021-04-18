package io.github.jgcodes.bitfs0x.output;

import io.github.jgcodes.libsm64.math.FloatVector3;
import org.postgresql.ds.PGSimpleDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DBOutput implements Output {
  private static final String SQL_INSERT = """
    INSERT INTO bully_solutions \
    (target_pos, frame_count, start_pos, start_speed, start_yaw, final_pos, final_speed, final_yaw) \
    VALUES ('{?, ?, ?}', ?, '{?, ?, ?}', ?, ?, '{?, ?, ?}', ?, ?)
    """;

  private final Connection connection;
  private final PreparedStatement statement;

  /**
   * Opens a new database output. You will need to provide the url of the
   * <abbr title="Bowser in the Fire Sea SQL database">BitFSQL</abbr> server
   * in addition to the password for the "{@code software}" user.
   * @param dbUrl url of the BitFSQL DB
   * @param password password
   */
  public DBOutput(String dbUrl, String password) throws SQLException {
    PGSimpleDataSource dataSrc = new PGSimpleDataSource();
    dataSrc.setServerNames(new String[] {dbUrl});
    dataSrc.setDatabaseName("postgres");
    dataSrc.setUser("software");
    dataSrc.setPassword(password);
    connection = dataSrc.getConnection();
    statement = connection.prepareStatement(SQL_INSERT);
  }

  @Override
  public void output(FloatVector3 targetPos, int frame, FloatVector3 startPos, float startSpeed, short startYaw,
                     FloatVector3 finalPos, float finalSpeed, short finalYaw) throws SQLException {
    statement.setFloat(1, targetPos.x());
    statement.setFloat(2, targetPos.y());
    statement.setFloat(3, targetPos.z());
    statement.setInt(4, frame);
    statement.setFloat(5, startPos.x());
    statement.setFloat(6, startPos.y());
    statement.setFloat(7, startPos.z());
    statement.setFloat(8, startSpeed);
    statement.setShort(9, startYaw);
    statement.setFloat(10, finalPos.x());
    statement.setFloat(11, finalPos.y());
    statement.setFloat(13, finalPos.z());
    statement.setFloat(14, finalSpeed);
    statement.setFloat(15, finalYaw);
    statement.execute();
  }

  @Override
  public void close() throws SQLException {
    statement.close();
    connection.close();
  }
}
