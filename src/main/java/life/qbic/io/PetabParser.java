package life.qbic.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import life.qbic.model.petab.PetabMetadata;

public class PetabParser {

  private final String META_INFO_YAML = "metaInformation.yaml";

  public PetabMetadata parse(String dataPath) {

    File directory = new File(dataPath);
    List<String> sourcePetabReferences = new ArrayList<>();

    File yaml = findYaml(directory);
    if (yaml != null) {
      BufferedReader reader = null;
      try {
        reader = new BufferedReader(new FileReader(yaml));
        boolean inIDBlock = false;
        while (true) {
          String line = reader.readLine();
          if (line == null) {
            break;
          }
          if(inIDBlock && line.contains(":")) {
            inIDBlock = false;
          }
          if(inIDBlock) {
            String[] tokens = line.split("-");
            if(tokens.length == 3) {
              String datasetCode = tokens[1].strip()+"-"+tokens[2].strip();
              sourcePetabReferences.add(datasetCode);
            }
          }
          if (line.contains("openBISSourceIds:")) {
            inIDBlock = true;
          }
        }
        reader.close();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    return new PetabMetadata(sourcePetabReferences);
  }

  public void addDatasetId(String outputPath, String datasetCode) throws IOException {

    Path path = Paths.get(Objects.requireNonNull(findYaml(new File(outputPath))).getPath());
    Charset charset = StandardCharsets.UTF_8;

    String idInLine = "openBISId:(.*)?(\\r\\n|[\\r\\n])";

    String content = Files.readString(path, charset);
    content = content.replaceAll(idInLine, "openBISId: "+datasetCode+"\n");
    Files.write(path, content.getBytes(charset));
  }

  private File findYaml(File directory) {
    for (File file : Objects.requireNonNull(directory.listFiles())) {
      if (file.isFile() && file.getName().equals(META_INFO_YAML)) {
        return file;
      }
      if (file.isDirectory()) {
        return findYaml(file);
      }
    }
    System.out.println(META_INFO_YAML + " not found");
    return null;
  }

}
