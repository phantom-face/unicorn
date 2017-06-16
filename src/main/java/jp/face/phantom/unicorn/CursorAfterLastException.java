package jp.face.phantom.unicorn;


public class CursorAfterLastException extends RuntimeException {
  
  private static final long serialVersionUID = 6975114702677923086L;
  
  public CursorAfterLastException() {
    super();
  }
  public CursorAfterLastException(String message) {
    super(message);
  }
  public CursorAfterLastException(Throwable cause) {
    super(cause);
  }
  public CursorAfterLastException(String message, Throwable cause) {
    super(message,cause);
  }
}