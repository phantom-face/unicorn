package jp.face.phantom.unicorn;

public enum DBMS {

  DEFAULT("DEFAULT", 
      new String[] {"primary.jdbc.properties", "secondary.jdbc.properties"}), 
  ORACLE("ORACLE", 
      new String[] {"oracle.primary.jdbc.properties", "oracle.secondary.jdbc.properties"}), 
  MySQL("MySQL", 
      new String[] {"mysql.primary.jdbc.properties", "mysql.secondary.jdbc.properties"}), 
  SQLite("SQLite", 
      new String[] {"sqlite.primary.jdbc.properties", "sqlite.secondary.jdbc.properties"});

  private String name;
  private String[] propertiesFiles;

  private DBMS(String name, String[] propertiesFiles) {
    this.name = name;
    this.propertiesFiles = propertiesFiles;
  }

  public String getName() {
    return this.name;
  }

  public String[] getPropertiesFiles() {
    return propertiesFiles;
  }

//  public static String[] getPropertiesFiles(String name) {
//
//    if (StringUtils.isEmpty(name)) {
//      return INVALID.getPropertiesFiles();
//    }
//
//    DBMS dbms =
//        Stream.of(DBMS.values()).filter(v -> v.name.equals(name)).findFirst().orElse(INVALID);
//
//    return dbms.getPropertiesFiles();
//  }
}
