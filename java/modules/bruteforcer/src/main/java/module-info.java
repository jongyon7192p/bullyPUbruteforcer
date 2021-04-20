/**
 * Helps us decide how to send rocks through the multiverse just to hit us in the back.
 */
module io.github.jgcodes.bitfs0x {
  requires io.github.jgcodes.libsm64;

  requires com.sun.jna;
  requires info.picocli;

  requires java.sql;
  requires java.naming;
  requires org.postgresql.jdbc;

  requires java.logging;

  opens io.github.jgcodes.bitfs0x to info.picocli;
  opens io.github.jgcodes.bitfs0x.misc to info.picocli;
}