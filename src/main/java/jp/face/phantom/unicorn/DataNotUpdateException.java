package jp.face.phantom.unicorn;


public class DataNotUpdateException extends RuntimeException {

  private static final long serialVersionUID = 6397498992139078735L;

  public DataNotUpdateException() {
    super();
  }
  public DataNotUpdateException(String message) {
    super(message);
  }
  public DataNotUpdateException(Throwable cause) {
    super(cause);
  }
  public DataNotUpdateException(String message, Throwable cause) {
    super(message, cause);
  }
}
