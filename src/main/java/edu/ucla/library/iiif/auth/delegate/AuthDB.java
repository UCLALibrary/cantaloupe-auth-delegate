
package edu.ucla.library.iiif.auth.delegate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import info.freelibrary.util.Env;
import info.freelibrary.util.StringUtils;

import edu.illinois.library.cantaloupe.delegate.Logger;

/**
 * Provides access to the auth database using a shared connection pool.
 */
public final class AuthDB {

    /** The database URL template. */
    private static final String DB_URL = "jdbc:postgresql://{}:{}/postgres";

    /** Constant for auth database host. */
    private static final String DB_HOST = "AUTHDB_HOST";

    /** Constant for auth database port. */
    private static final String DB_PORT = "AUTHDB_PORT";

    /** Constant for auth database username. */
    private static final String DB_USERNAME = "AUTHDB_USERNAME";

    /** Constant for auth database password. */
    private static final String DB_PASSWORD = "AUTHDB_PASSWORD";

    /** The holder of the database connection. */
    private static final class DatabaseHolder {

        /** The internal database pool. */
        static final HikariDataSource POOL = createPool();
    }

    /**
     * Gets the database connection pool.
     *
     * @return A database connection
     */
    private static DataSource databasePool() {
        return DatabaseHolder.POOL;
    }

    /**
     * Creates a database connection pool.
     *
     * @return A database connection
     */
    private static HikariDataSource createPool() {
        final String dbURL = StringUtils.format(DB_URL, Env.get(DB_HOST, "localhost"), Env.get(DB_PORT, 5432));
        final HikariConfig config = new HikariConfig();

        config.setDriverClassName("org.postgresql.Driver");
        config.setJdbcUrl(dbURL);
        config.setUsername(Env.get(DB_USERNAME, "postgres"));
        config.setPassword(Env.getOrFail(DB_PASSWORD, "No database password found"));
        config.setMaximumPoolSize(10);
        config.setConnectionTimeout(5_000);

        return new HikariDataSource(config);
    }

    /**
     * Gets the access mode for the supplied item ID. In the non-developer/test databases, the ID is an ARK (e.g.,
     * ark:/21198/z16t1r0h/2t05n535). Local build testing uses TIFF files names as test IDs.
     *
     * @param aItemID An item ID
     * @return The supplied item's access mode
     * @throws SQLException If there is a problem executing the query
     */
    public AccessMode getAccessMode(final String aItemID) throws SQLException {
        final String sql = "SELECT access_mode FROM items WHERE uid = ?";

        try (Connection connection = databasePool().getConnection();
                final PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, aItemID); // Add our requested item ID to the query

            try (ResultSet results = statement.executeQuery()) {
                final int accessMode;

                if (!results.next()) {
                    return AccessMode.OPEN;
                }

                // Gets the access mode from the database
                accessMode = results.getInt("access_mode");

                if (results.wasNull()) {
                    final SQLException error = new SQLException("NULL access_mode for uid: " + aItemID);

                    Logger.error(Arrays.stream(error.getStackTrace()).map(element -> "\tat " + element)
                            .collect(Collectors.joining(System.lineSeparator())));
                    throw error;
                }

                if (results.next()) {
                    final SQLException error = new SQLException("Multiple rows found for uid: " + aItemID);

                    Logger.error(Arrays.stream(error.getStackTrace()).map(element -> "\tat " + element)
                            .collect(Collectors.joining(System.lineSeparator())));
                    // But still fall through to return the access_mode value
                }

                return switch (accessMode) {
                    case 0 -> AccessMode.OPEN;
                    case 1 -> AccessMode.TIERED;
                    default -> AccessMode.OPEN;
                };
            }
        }
    }
}
