package fr.cnrs.liris.jpugetgil.sparqltosql.hibernate;

import fr.cnrs.liris.jpugetgil.sparqltosql.VersioningQueryExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Objects;

/**
 * Created by jpugetgil.
 * Singleton for the JDBC session
 */
public class JdbcConnection {

    private static final Logger log = LoggerFactory.getLogger(VersioningQueryExecution.class);

    private static JdbcConnection jdbcConnection;
    private Connection connection;
    private Statement statement;
    private static final String CONNECTION_URL = "jdbc:postgresql://localhost:5432/sparqltosql";
    private static final String CONNECTION_USERNAME = "postgres";
    private static final String CONNECTION_PASSWORD = "password";

    /**
     * Constructor method in order to create db connection & statement
     * @throws SQLException exception can be thrown during DB transaction
     *
     * @author <a href="https://github.com/evrentan">Evren Tan</a>
     */
    private JdbcConnection() throws SQLException {
        try {
            connection = DriverManager.getConnection(CONNECTION_URL, CONNECTION_USERNAME, CONNECTION_PASSWORD);
            statement = connection.createStatement();
        } catch (SQLException exception) {
            log.error(exception.getMessage());
        } finally {
            if (Objects.isNull(statement)) {
                assert connection != null;
                connection.close();
            }
        }
    }

    /**
     * Create the instance of {@link JdbcConnection} object if it is not created yet and guarantee that there is only one single instance is created for this class.
     * @return JdbcConnection created single instance
     *
     * @author <a href="https://github.com/evrentan">Evren Tan</a>
     */
    public static JdbcConnection getInstance() {
        try {
            if (Objects.isNull(jdbcConnection))
                jdbcConnection = new JdbcConnection();
        } catch (SQLException exception) {
            log.error(exception.getMessage());
        }
        return jdbcConnection;
    }

    /**
     * Execute the given SQL query
     * @param sqlQuery SQL query to be executed
     * @return ResultSet result of the SQL query
     * @throws SQLException exception can be thrown during DB transaction
     */
    public ResultSet executeSQL(String sqlQuery) throws SQLException {
        return statement.executeQuery(sqlQuery);
    }
}