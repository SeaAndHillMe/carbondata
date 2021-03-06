/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.carbondata.util;

import java.io.*;

import org.apache.carbondata.core.metadata.datatype.DataTypes;
import org.apache.carbondata.sdk.file.CarbonReader;
import org.apache.carbondata.sdk.file.CarbonWriter;
import org.apache.carbondata.core.metadata.datatype.Field;
import org.apache.carbondata.sdk.file.Schema;

import static org.apache.carbondata.sdk.file.utils.SDKUtil.listFiles;

public class BinaryUtil {
  public static void binaryToCarbon(String sourceImageFolder, String outputPath,
                                    String sufAnnotation, final String sufImage) throws Exception {
    Field[] fields = new Field[5];
    fields[0] = new Field("binaryId", DataTypes.INT);
    fields[1] = new Field("binaryName", DataTypes.STRING);
    fields[2] = new Field("binary", DataTypes.BINARY);
    fields[3] = new Field("labelName", DataTypes.STRING);
    fields[4] = new Field("labelContent", DataTypes.STRING);
    CarbonWriter writer = CarbonWriter
        .builder()
        .outputPath(outputPath)
        .withCsvInput(new Schema(fields))
        .withBlockSize(256)
        .writtenBy("binaryExample")
        .withPageSizeInMb(1)
        .build();
    binaryToCarbon(sourceImageFolder, writer, sufAnnotation, sufImage);
  }

  public static boolean binaryToCarbon(String sourceImageFolder, CarbonWriter writer,
      String sufAnnotation, final String sufImage) throws Exception {
    int num = 1;

    byte[] originBinary = null;

    // read and write image data
    for (int j = 0; j < num; j++) {

      Object[] files = listFiles(sourceImageFolder, sufImage).toArray();

      if (null != files) {
        for (int i = 0; i < files.length; i++) {
          // read image and encode to Hex
          BufferedInputStream bis = new BufferedInputStream(
              new FileInputStream(new File((String) files[i])));
          originBinary = new byte[bis.available()];
          while ((bis.read(originBinary)) != -1) {
          }

          String labelFileName = ((String) files[i]).split(sufImage)[0] + sufAnnotation;
          BufferedInputStream txtBis = new BufferedInputStream(new FileInputStream(labelFileName));
          String labelValue = null;
          byte[] labelBinary = null;
          labelBinary = new byte[txtBis.available()];
          while ((txtBis.read(labelBinary)) != -1) {
            labelValue = new String(labelBinary, "UTF-8");
          }
          // write data
          writer.write(new Object[]{i, (String) files[i], originBinary,
              labelFileName, labelValue});
          bis.close();
          txtBis.close();
        }
      }
      writer.close();
    }
    return true;
  }

  public static boolean carbonToBinary(String carbonPath, String outputPath)
      throws IOException, InterruptedException {
    CarbonReader reader = CarbonReader
        .builder(carbonPath)
        .build();
    return carbonToBinary(reader, outputPath);
  }

  public static boolean carbonToBinary(CarbonReader reader, String outputPath)
      throws IOException, InterruptedException {
    System.out.println("\nData:");
    int i = 0;
    while (i < 20 && reader.hasNext()) {
      Object[] row = (Object[]) reader.readNextRow();

      byte[] outputBinary = (byte[]) row[1];
      System.out.println(row[0] + " " + row[2] + " image size:" + outputBinary.length);

      // save image, user can compare the save image and original image
      String originalPath = (String) row[0];
      int index = originalPath.lastIndexOf("/");
      File file = new File(outputPath);
      if (!file.exists()) {
        assert file.mkdir();
      }
      String destString = outputPath + originalPath.substring(index, originalPath.length());
      BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(destString));
      bos.write(outputBinary);
      bos.close();
      i++;
    }
    System.out.println("number of reading: " + i);
    System.out.println("\nFinished");
    reader.close();
    return true;
  }
}
