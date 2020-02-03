package org.blackaho.misc.datagencli;

import org.apache.spark.api.java.function.MapPartitionsFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.catalyst.encoders.RowEncoder;
import org.apache.spark.sql.types.DataType;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class BigDataGenCLI extends DataGenCLI {


  static final String OPTION_OUTPUTFORMAT = "--format=";
  static final String OPTION_PARTITIONBY = "--partitionby=";
  static final String OPTION_REPARTITION = "--repartition=";
  static final String OPTION_COALESCE = "--coalesce=";

  String outputFormat = "csv";
  List<String> partitionBy = new ArrayList<>();
  int repartition = -1;
  int coalesce = -1;


  public static void main(String[] args) {
    BigDataGenCLI dataGenCLI = new BigDataGenCLI();
    dataGenCLI.parseOptions(args);
    if (!dataGenCLI.checkOptions()) {
      return;
    }
    dataGenCLI.run();
  }

  @Override
  public void parseOptions(String[] args) {
    super.parseOptions(args);
    // additional parameters for BigDataGenCli --format=orc/avro/parquet
    for (String arg : args) {
      if (arg.startsWith(OPTION_OUTPUTFORMAT)) {
        outputFormat = arg.substring(OPTION_OUTPUTFORMAT.length(), arg.length());
      } else if (arg.startsWith(OPTION_PARTITIONBY)) {
        partitionBy = parseCSVStringAsList(arg.substring(OPTION_PARTITIONBY.length(), arg.length()));
      } else if (arg.startsWith(OPTION_REPARTITION)) {
        repartition = Integer.parseInt(arg.substring(OPTION_REPARTITION.length(), arg.length()));
      } else if (arg.startsWith(OPTION_COALESCE)) {
        coalesce = Integer.parseInt(arg.substring(OPTION_COALESCE.length(), arg.length()));
      }

    }
  }


  @Override
  public boolean checkOptions() {
    if (!super.checkOptions()) return false;
    if (this.listFieldsOnly) return true;  // if --listfields only, we can continue
    // check additional options for BigDataGenCli - e.g., out is mandatory
    if (this.outputFilename==null || this.outputFilename.isEmpty()) {
      err.println("Option --output is mandatory with BigDataGenCLI");
      err.println(USAGE);
      return false;
    }
    // headerLine is not supported (only --header) - TBD: Refactor, move headerline from base class.
    if (this.headerLine!=null && !this.headerLine.isEmpty()) {
      err.println("Option --headerline is not supported with BigDataGenCLI, please use --header instead.");
      err.println(USAGE);
      return false;
    }
    if (! (Arrays.asList("csv","orc","parquet","avro","delta").contains(this.outputFormat)) ) {
      err.println("Option --format has invalid data, accepted values are csv, parquet, orc, avro and delta");
      return false;
    }
    return true;
  }



  // override methods that will use spark

  @Override
  protected boolean prepareOutputFile() {
    // we let spark handle file creation
    return false;
  }

  @Override
  protected void runWithNumberOfRows(long numberOfRowsToGenerate) {
    SparkSession sparkSession = SparkSession.builder().getOrCreate();
    // create dataset from range and apply row generator function to gen data
    Dataset ds = sparkSession
      .range(1L, numberOfRowsToGenerate+1L)
      .mapPartitions(new RowGeneratorFunction(this.fields,this.header), RowEncoder.apply(getSchema()));
    // uses repartition/coalesce if applicable
    ds = this.repartition > 0 ? ds.repartition(this.repartition) : ds;
    ds = this.coalesce > 0 ? ds.coalesce(this.coalesce) : ds;
    // and writes the data
    ds.write()
      .format(this.outputFormat)
      .option("sep", this.separator)
      .option("header", this.header!=null ? "true" : "false")
      .partitionBy(this.partitionBy.toArray(new String[] {}))
      .save(this.outputFilename);
    sparkSession.close();
  }

  public static class RowGeneratorFunction implements MapPartitionsFunction<Long,Row> {

    ArrayList<String> fields;
    ArrayList<String> header;

    RowGeneratorFunction(List<String> fields1, List<String> header1) {
      this.fields = new ArrayList<>(fields1);
      this.header = header1 == null ? new ArrayList<>() : new ArrayList<>(header1);
    }

    @Override
    public Iterator<Row> call(Iterator<Long> iterator) throws Exception {
      RowGenerator rowGen = RowGeneratorFactory.createDefaultRowGenerator()
          .setFields(this.fields)
          .setHeader(this.header);

      List<Row> partitionRows = new ArrayList<>();
      while (iterator.hasNext()) {
        Long rowNum = iterator.next();
        partitionRows.add(RowFactory.create(rowGen.generateRow(rowNum).toArray()));
      }

      return partitionRows.iterator();
    }
  }

  // helper method to get schema for fields/header
  protected StructType getSchema() {
    // infer schema for rows (generate a single row so we can infer that)
    List sampleFieldValues = rowGenerator.generateRow(1L);
    StructType schema = new StructType();
    int pos = 0;
    for (Object val : sampleFieldValues) {
      DataType dataType;
      String colName = header!=null ? header.get(pos) : "col"+pos;
      if (val instanceof Long) {
        dataType = DataTypes.LongType;
      } else if (val instanceof Double) {
        dataType = DataTypes.DoubleType;
      } else {
        dataType = DataTypes.StringType;
      }
      schema = schema.add(colName, dataType);
      pos++;
    }
    return schema;
  }


}


