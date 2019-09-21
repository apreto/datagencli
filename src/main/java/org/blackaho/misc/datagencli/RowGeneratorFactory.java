package org.blackaho.misc.datagencli;

public class RowGeneratorFactory {

  public static RowGenerator createDefaultRowGenerator() {
    return createFakerRowGenerator();
  }

  public static RowGenerator createFakerRowGenerator() {
    return new FakerRowGenerator();
  }

}
