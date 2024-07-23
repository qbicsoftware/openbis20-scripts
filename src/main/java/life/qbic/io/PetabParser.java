package life.qbic.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

  /**
   * adds key-value pairs to the bottom of a petab.yaml found below the provided path
   *
   * @param outputPath the path of the PEtab
   * @param properties map of properties to add
   */
  public void addParameters(String outputPath, Map<String, String> properties) {

    File directory = new File(outputPath);

    File yaml = findYaml(directory);
    if (yaml != null) {
      FileOutputStream fos = null;
      try {
        fos = new FileOutputStream(yaml.getPath(), true);
        for (Map.Entry<String, String> entry : properties.entrySet()) {
          String line = entry.getKey() + ": " + entry.getValue() + "\n";
          fos.write(line.getBytes());
        }
        fos.close();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public static void main(String[] args) throws IOException {

    /*


    File testfile = new File("/Users/afriedrich/git/openbis-20-scripts/example_petab/metaInformation.yaml");
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    MetaInformation metaInfo = mapper.readValue(testfile, MetaInformation.class);
    System.err.println(metaInfo);
    metaInfo.getUnits().setOpenbisParentIds(Arrays.asList("1","2"));
    metaInfo.getUnits().setOpenbisId("3");
    mapper.writeValue(new File("/Users/afriedrich/git/openbis-20-scripts/Output.yaml"), metaInfo);


    ObjectMapper mapper2 = new YAMLMapper();
    MetaInformation metaInfo2 = mapper2.readValue(testfile, MetaInformation.class);
    System.err.println(metaInfo2);

    experimentalCondition:
  conditionId:
    pattern: treatment1_treatment2
  - treatment1: TGFb
    unit: ng/ul
  - treatment2: GAS6
    unit: ug/ml

    File test = new File("/Users/afriedrich/git/openbis-20-scripts/Output.yaml");
    ExperimentalCondition metaInfo = mapper.readValue(test, ExperimentalCondition.class);
    System.err.println(metaInfo);


    List<ConditionWithUnit> list = new ArrayList<>();
    list.add(new ConditionWithUnit("TGFb", "ng/ul"));
    list.add(new ConditionWithUnit("GAS6", "ug/ml"));
    ExperimentalCondition conditions = new ExperimentalCondition(
        new IdWithPattern("treatment1_treatment2"), list);
    //mapper.writeValue(new File("/Users/afriedrich/git/openbis-20-scripts/Output.yaml"),conditions);
*/
  }

}
