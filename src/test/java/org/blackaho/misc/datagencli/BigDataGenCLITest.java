package org.blackaho.misc.datagencli;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class BigDataGenCLITest {

  private BigDataGenCLI bigDataGenCLI;

  @Rule
  public TemporaryFolder tmpFolder = new TemporaryFolder();


  private final ByteArrayOutputStream systemErr = new ByteArrayOutputStream();
  private final PrintStream originalSystemErr = System.err;


  @Before
  public void before() {
    System.setErr(new PrintStream(systemErr));
    bigDataGenCLI = new BigDataGenCLI();
  }

  @After
  public void after() {
    System.setErr(originalSystemErr);
  }

  @Test
  public void testParseOptionsFormat() {
    String[] args = new String[] {"--format=parquet"};
    bigDataGenCLI.parseOptions(args);
    assertEquals("parquet", bigDataGenCLI.outputFormat);
  }

  @Test
  public void testParseOptionsPartitionBy() {
    String[] args = new String[] {"--partitionby=field1,field2"};
    bigDataGenCLI.parseOptions(args);
    Object[] expected = new String[] {"field1","field2"};
    assertArrayEquals(expected, bigDataGenCLI.partitionBy.toArray());
  }

  @Test
  public void testParseOptionsRepartition() {
    String[] args = new String[] {"--repartition=10"};
    bigDataGenCLI.parseOptions(args);
    assertEquals(10, bigDataGenCLI.repartition);
  }

  @Test
  public void testParseOptionsCoalesce() {
    String[] args = new String[] {"--coalesce=5"};
    bigDataGenCLI.parseOptions(args);
    assertEquals(5, bigDataGenCLI.coalesce);
  }

  // test argument checking
  @Test
  public void testCheckArgumentsValidWithNRowsAndFieldsAndOutput() {
    bigDataGenCLI.fields = Arrays.asList(new String[] {"name.firstName"});
    bigDataGenCLI.nRows = 10;
    bigDataGenCLI.outputFilename = "someFile.csv";
    assertTrue(bigDataGenCLI.checkOptions());
  }

  @Test
  public void testCheckArgumentsValidWithNMbsAndFieldsAndOutput() {
    bigDataGenCLI.fields = Arrays.asList(new String[] {"name.firstName"});
    bigDataGenCLI.nMbytes = 1;
    bigDataGenCLI.outputFilename = "someFile.csv";
    assertTrue(bigDataGenCLI.checkOptions());
  }


  @Test
  public void testCheckArgumentsUnfilledOutputFilename() {
    bigDataGenCLI.fields = Arrays.asList(new String[] {"name.firstName"});
    bigDataGenCLI.nRows = 10;
    assertFalse(bigDataGenCLI.checkOptions());
  }

  @Test
  public void testCheckArgumentsEmptyOutputFilename() {
    bigDataGenCLI.fields = Arrays.asList(new String[] {"name.firstName"});
    bigDataGenCLI.nRows = 10;
    bigDataGenCLI.outputFilename="";
    assertFalse(bigDataGenCLI.checkOptions());
  }


  @Test
  public void testCheckArgumentsInvalidFormat() {
    bigDataGenCLI.fields = Arrays.asList(new String[] {"name.firstName"});
    bigDataGenCLI.nRows = 10;
    bigDataGenCLI.outputFilename = "someFile.csv";
    bigDataGenCLI.outputFormat = "invalid";
    assertFalse(bigDataGenCLI.checkOptions());
  }

  @Test
  public void testCheckArgumentsFilledHeaderLine() {
    // fill mandatory fields
    bigDataGenCLI.fields = Arrays.asList(new String[] {"name.firstName"});
    bigDataGenCLI.nRows = 10;
    bigDataGenCLI.outputFilename = "someFile.csv";
    // fill header line
    bigDataGenCLI.headerLine="some,header";
    assertFalse(bigDataGenCLI.checkOptions());
  }

  @Test
  public void testCheckArgumentsEmptyHeaderLine() {
    // fill mandatory fields
    bigDataGenCLI.fields = Arrays.asList(new String[] {"name.firstName"});
    bigDataGenCLI.nRows = 10;
    bigDataGenCLI.outputFilename = "someFile.csv";
    // fill header line
    bigDataGenCLI.headerLine="";
    assertTrue(bigDataGenCLI.checkOptions());
  }


  @Test
  public void testPrepareOutputFile() {
    // must always return false, file handling is done by spark
    bigDataGenCLI.outputFilename = "someValidFileName.csv";
    assertFalse( bigDataGenCLI.prepareOutputFile() );
  }

  @Test
  public void testMainWithListFields() {
    bigDataGenCLI.main(new String[] {"--listfields"} );
    assertTrue( systemErr.toString().contains("randomLong")); // check if some of our custom functions is present
  }

  @Test
  public void testMainWithIncompleteParams() {
    bigDataGenCLI.main(new String[] {"--fields=name.firstName"} ); // missing --out and either --rows=xx or --mbs=yy
    assertTrue( systemErr.toString().contains("Usage")); // check if err msg/usage present
  }


  @Test
  public void testGetSchema() {
    List<String> header = Arrays.asList("myString", "myDouble", "myLong");
    bigDataGenCLI.rowGenerator = RowGeneratorFactory.createDefaultRowGenerator()
        .setFields(Arrays.asList("name.firstName", "randomDouble(2:1:10)","randomLong(1:10)"))
        .setHeader(header);
    bigDataGenCLI.header = header;

    StructType schema = bigDataGenCLI.getSchema();
    //    assertEquals(schema.fields()[0].dataType()
    assertEquals( 3, schema.fields().length); // 3 types

    assertEquals( "myString", schema.fieldNames()[0]);
    assertEquals(DataTypes.StringType, schema.fields()[0].dataType());

    assertEquals( "myDouble", schema.fieldNames()[1]);
    assertEquals(DataTypes.DoubleType, schema.fields()[1].dataType());

    assertEquals( "myLong", schema.fieldNames()[2]);
    assertEquals( DataTypes.LongType, schema.fields()[2].dataType());
  }

  @Test
  public void testMainWithGenerateRowsNoHeaderAndNoSeparator() {
    // Calling SparkSession.builder here so next call to SparkSession.builder().getOrCreate() will use these settings
    SparkSession.builder().master("local[2]").appName("BigDataGenCLITest").getOrCreate();
    String outFileName = tmpFolder.getRoot().getAbsolutePath() + "/tmpFile1.csv";
    String [] args = new String[] {"--rows=5", "--fields=name.firstName","--out="+outFileName};
    bigDataGenCLI.main(args);
    // check file exists
    File outFile = new File(outFileName);
    File outFileSuccess = new File(outFileName + "/_SUCCESS"); // spark generates this _SUCCESS file if all ok
    assertTrue(outFile.exists());
    assertTrue(outFile.isDirectory());
    assertTrue(outFileSuccess.exists());
  }


  @Test
  public void testMainWithGenerateRowsHeaderAndSeparator() {
    // Calling SparkSession.builder here so next call to SparkSession.builder().getOrCreate() will use these settings
    SparkSession.builder().master("local[2]").appName("BigDataGenCLITest").getOrCreate();
    String outFileName = tmpFolder.getRoot().getAbsolutePath() + "/tmpFile1.csv";
    String [] args = new String[] {"--rows=5", "--fields=name.firstName,name.lastName","--header=firstName1,lastName2", "--separator=|", "--out="+outFileName};
    bigDataGenCLI.main(args);
    // check results using spark
    SparkSession session = SparkSession.builder().master("local[1]").appName("BigDataGenCLITest").getOrCreate();
    Dataset ds = session.read().format("csv").option("header","true").option("sep","|").load(outFileName);
    int firstNameIndex = ds.schema().fieldIndex("firstName1");
    int lastNameIndex = ds.schema().fieldIndex("lastName2");
    long nRows = ds.count();
    session.close();
    // assert headers (and separator too)
    assertEquals(0, firstNameIndex);
    assertEquals(1, lastNameIndex);
    // assert nRows
    assertEquals(5L, nRows);
  }

  @Test
  public void testMainWithGenerateRowsAndCoalesce() {
    // Calling SparkSession.builder here so next call to SparkSession.builder().getOrCreate() will use these settings
    SparkSession.builder().master("local[2]").appName("BigDataGenCLITest").getOrCreate();
    String outFileName = tmpFolder.getRoot().getAbsolutePath() + "/tmpFile1.csv";
    String [] args = new String[] {"--rows=10", "--fields=name.firstName,name.lastName","--out="+outFileName,"--coalesce=1"};
    bigDataGenCLI.main(args);
    // assert generated file has expected number of partitions (i.e., part files)
    File outFile = new File(outFileName);
    long numPartitions = Arrays.stream(outFile.list()).filter( s -> s.startsWith("part-") ).count();
    assertEquals(1, numPartitions);
  }

  @Test
  public void testMainWithGenerateRowsAndRepartition() {
    // Calling SparkSession.builder here so next call to SparkSession.builder().getOrCreate() will use these settings
    SparkSession.builder().master("local[2]").appName("BigDataGenCLITest").getOrCreate();
    String outFileName = tmpFolder.getRoot().getAbsolutePath() + "/tmpFile1.csv";
    String [] args = new String[] {"--rows=10", "--fields=name.firstName,name.lastName","--out="+outFileName,"--repartition=4"};
    bigDataGenCLI.main(args);
    // assert generated file has expected number of partitions (i.e., part files)
    File outFile = new File(outFileName);
    long numPartitions = Arrays.stream(outFile.list()).filter( s -> s.startsWith("part-") ).count();
    assertEquals(4, numPartitions);
  }



}
