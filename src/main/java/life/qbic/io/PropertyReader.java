package life.qbic.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.TreeMap;

public class PropertyReader {

    public static TreeMap<String, String> getProperties(String infile) {

      TreeMap<String, String> properties = new TreeMap<>();
      BufferedReader bfr = null;
      try {
        bfr = new BufferedReader(new FileReader(new File(infile)));
      } catch (FileNotFoundException e) {
        throw new RuntimeException(e);
      }

      String line;
      while (true) {
        try {
          if ((line = bfr.readLine()) == null)
            break;
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        if (!line.startsWith("#") && !line.isEmpty()) {
          String[] property = line.trim().split("=");
          properties.put(property[0].trim(), property[1].trim());
        }
      }

      try {
        bfr.close();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      return(properties);
    }
}
