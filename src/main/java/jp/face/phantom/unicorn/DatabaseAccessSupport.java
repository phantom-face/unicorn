package jp.face.phantom.unicorn;

import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSourceFactory;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.LimitedRecordCountBeanProcessor;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;

public class DatabaseAccessSupport {

  private static Map<String, DatabaseAccessSupport> INSTANCES = new HashMap<>();

  private static final long FIXED_THREAD_ID = -99L;
  private static final int FIXED_NUMBER = -99;

  private DBMS dbms;
  private long threadId;
  private int number;
  private DataSource dataSource;
  private Connection connection;
  private ResultSet resultSet;
  private int limit;

  private DatabaseAccessSupport(DBMS dbms) throws Exception {

    if (this.dataSource == null) {
      this.setUpConnection(dbms);
    }
  }

  private void setUpConnection(DBMS dbms) throws Exception {

    Properties properties = new Properties();
    InputStream inputStream = null;
    Exception exception = null;

    try {
      for (String propName : dbms.getPropertiesFiles()) {
        exception = null;
        try {
          // AsStreamだとjar化したときに取得できないので一旦URL型で取得
          URL url = this.getClass().getClassLoader().getResource(propName);
          inputStream = url.openStream();
          properties.load(inputStream);
          this.dataSource = BasicDataSourceFactory.createDataSource(properties);
          this.connection = this.dataSource.getConnection();
          break;

        } catch (Exception e) {
          exception = e;
        }
      }
      if (exception != null) throw exception;

    } finally {
      if (inputStream != null) inputStream.close();
    }
  }

  public static synchronized DatabaseAccessSupport getSingletonInstance() throws InstantiationException {
    return getSingletonInstance(DBMS.DEFAULT);
  }

  public static synchronized DatabaseAccessSupport getSingletonInstance(DBMS dbms) throws InstantiationException {
    return getInstance(dbms, DatabaseAccessSupport.FIXED_THREAD_ID, DatabaseAccessSupport.FIXED_NUMBER);
  }

  public static synchronized DatabaseAccessSupport getSingletonInstance(DBMS dbms, int number) throws InstantiationException {
    return getInstance(dbms, DatabaseAccessSupport.FIXED_THREAD_ID, number);
  }

  public static synchronized DatabaseAccessSupport getInstance() throws InstantiationException {
    return getInstance(DBMS.DEFAULT);
  }

  public static synchronized DatabaseAccessSupport getInstance(DBMS dbms) throws InstantiationException {
    return getInstance(dbms, Thread.currentThread().getId(), DatabaseAccessSupport.FIXED_NUMBER);
  }

  public static synchronized DatabaseAccessSupport getInstance(DBMS dbms, int number) throws InstantiationException {
    return getInstance(dbms, Thread.currentThread().getId(), number);
  }

  private static synchronized DatabaseAccessSupport getInstance(DBMS dbms, long threadId, int number) throws InstantiationException {

    // check instance pool.
    if (INSTANCES == null) {
      INSTANCES = new HashMap<>();
    }

    // check instance to this thread.
    String currentInstanceKey =
        Long.toString(threadId) + "-" + dbms.getName() + "-" + Integer.toString(number);

    try {
      if (!INSTANCES.containsKey(currentInstanceKey)) {
        DatabaseAccessSupport das = new DatabaseAccessSupport(dbms);
        das.dbms = dbms;
        das.threadId = threadId;
        das.number = number;
        INSTANCES.put(currentInstanceKey, das);
      }
    } catch (Exception e) {
      throw new InstantiationException(e.getMessage());
    }

    return INSTANCES.get(currentInstanceKey);
  }

  public <T> List<T> find(AbstractResultSetCallBackHandler<T> rsHandler) throws SQLException {

    List<T> result = new ArrayList<>();

    QueryRunner runner = new QueryRunner(this.dataSource);
    ResultSetHandler<List<T>> handler = new BeanListHandler<T>(rsHandler.getType());

    result = (List<T>) runner.query(this.connection, rsHandler.getSQL(), handler,
        (Object[]) rsHandler.getParameters().toArray(new Object[0]));

    if (result.isEmpty())
      throw new DataNotFoundException(rsHandler.getType().getSimpleName() + " not found.");

    return result;
  }
  
  public <T> void load(AbstractResultSetCallBackHandler<T> rsHandler, int limit) throws DataNotFoundException, SQLException {

    if (limit == 0)
      throw new DataNotFoundException("limit is zero.");

    PreparedStatement statement = this.connection.prepareStatement(rsHandler.getSQL());
    int i = 0;
    for (Object p : rsHandler.getParameters()) {
      statement.setObject(i++, p);
    }

    this.limit = limit;
    this.resultSet = statement.executeQuery();
  }

  public <T> List<T> fetch(AbstractResultSetCallBackHandler<T> rsHandler)
      throws CursorAfterLastException, DataNotFoundException, SQLException {

    if ((this.resultSet == null) || (this.resultSet.isClosed()))
      throw new CursorAfterLastException("resultset is null or closed.");

    List<T> result = new ArrayList<>();

    LimitedRecordCountBeanProcessor processor = new LimitedRecordCountBeanProcessor();
    result = processor.toBeanList(this.resultSet, rsHandler.getType(), this.limit);

    if (result.isEmpty())
      throw new DataNotFoundException(rsHandler.getType().getSimpleName() + " not found.");

    return result;
  }

  public <T> int update(AbstractResultSetCallBackHandler<T> rsHandler) throws DataNotUpdateException, SQLException {

    int result = 0;

    QueryRunner runner = new QueryRunner(this.dataSource);
    result = runner.update(this.connection, rsHandler.getSQL(),
        (Object[]) rsHandler.getParameters().toArray(new Object[0]));

    if (result == 0)
      throw new DataNotUpdateException(rsHandler.getType().getSimpleName() + " not updated.");

    return result;
  }

  public <T> int create(AbstractResultSetCallBackHandler<T> rsHandler) throws DataNotUpdateException, SQLException {

    int result = 0;

    try (Statement statement = this.connection.createStatement()) {
      result = statement.executeUpdate(rsHandler.getSQL());

    } catch (SQLException e) {
      throw new DataNotUpdateException(e.getMessage());
    }

    return result;
  }

  public void commit() throws SQLException {
    this.connection.commit();
  }

  public void rollback() throws SQLException {
    this.connection.rollback();
  }

  public void close() throws SQLException {
    DbUtils.closeQuietly(this.resultSet);
    DbUtils.closeQuietly(this.connection);
    this.removeConnection();
  }

  public void commitAndClose() throws SQLException {
    DbUtils.commitAndCloseQuietly(this.connection);
    this.removeConnection();
  }

  public void rollbackAndClose() throws SQLException {
    DbUtils.rollbackAndCloseQuietly(this.connection);
    this.removeConnection();
  }

  private void removeConnection() {
    // check instance to this thread and remove connection.
    String currentInstanceKey = 
        Long.toString(this.threadId) + "-" + this.dbms.getName() + "-" + Integer.toString(this.number);
    if (INSTANCES.containsKey(currentInstanceKey)) {
      INSTANCES.remove(currentInstanceKey);
    }
  }
}
