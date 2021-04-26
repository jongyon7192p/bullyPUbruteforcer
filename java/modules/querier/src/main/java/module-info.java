/**
 * A simple database query software for the BitFSQL database
 */
module io.github.jgcodes.bitfs0x.querier {
  requires java.sql;
  requires java.naming;
  requires org.postgresql.jdbc;

  requires javafx.controls;
  requires javafx.fxml;

  opens io.github.jgcodes.bitfs0x.querier to javafx.graphics, javafx.fxml;
}