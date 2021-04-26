package io.github.jgcodes.bitfs0x.output;

import io.github.jgcodes.libsm64.math.FloatVector3;
import org.postgresql.PGConnection;
import org.postgresql.ds.PGSimpleDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DBOutput implements Output {
  private static final String SQL_INSERT = """
    INSERT INTO bully_solutions \
    (target_pos, frame_count, bully_start_pos, bully_start_speed, bully_start_yaw, bully_final_pos, bully_final_speed, bully_final_yaw) \
    VALUES (?, ?, ?, ?, ?, ?, ?, ?)\
    """;

  private final PGConnection pgConnection;
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
    dataSrc.setUser("bruteforcer");
    dataSrc.setPassword(password);
    connection = dataSrc.getConnection();
    pgConnection = (PGConnection) connection;
    statement = connection.prepareStatement(SQL_INSERT);

    //System.err.println(statement.getMetaData().getColumnCount());
  }

  @Override
  public void write(FloatVector3 targetPos, int frame, FloatVector3 startPos, float startSpeed, short startYaw,
                    FloatVector3 finalPos, float finalSpeed, short finalYaw) throws SQLException {
    statement.setArray(1, connection.createArrayOf("float4", targetPos.toWrapperArray()));
    statement.setInt(2, frame);
    statement.setArray(3, connection.createArrayOf("float4", startPos.toWrapperArray()));
    statement.setFloat(4, startSpeed);
    statement.setShort(5, startYaw);
    statement.setArray(6, connection.createArrayOf("float4", finalPos.toWrapperArray()));
    statement.setFloat(7, finalSpeed);
    statement.setShort(8, finalYaw);
    statement.execute();
  }

  @Override
  public void close() throws SQLException {
    statement.close();
    connection.close();
  }
}
