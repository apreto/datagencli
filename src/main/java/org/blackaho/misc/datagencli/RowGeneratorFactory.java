package org.blackaho.misc.datagencli;

public class RowGeneratorFactory {

  private RowGeneratorFactory() {}

  public static RowGenerator createDefaultRowGenerator() {
    return createFakerRowGenerator();
  }

  public static RowGenerator createFakerRowGenerator() {
    return new FakerRowGenerator();
  }

}
