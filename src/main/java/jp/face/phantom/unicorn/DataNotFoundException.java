package jp.face.phantom.unicorn;


public class DataNotFoundException extends RuntimeException {
  
  private static final long serialVersionUID = 6975114702677923086L;
  
  public DataNotFoundException() {
    super();
  }
  public DataNotFoundException(String message) {
    super(message);
  }
  public DataNotFoundException(Throwable cause) {
    super(cause);
  }
  public DataNotFoundException(String message, Throwable cause) {
    super(message,cause);
  }
}