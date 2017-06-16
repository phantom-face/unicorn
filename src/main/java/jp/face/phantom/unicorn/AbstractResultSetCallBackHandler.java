package jp.face.phantom.unicorn;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractResultSetCallBackHandler<T> {
  
  private List<Object> parameters = new ArrayList<>();
  
  public abstract String getSQL();
  public abstract Class<T> getType();
  
  public void setParameter(Object param) {
    this.parameters.add(param);
  }
  public List<Object> getParameters() {
    return this.parameters;
  }
  public void reset() {
    this.parameters.clear();
  }
}