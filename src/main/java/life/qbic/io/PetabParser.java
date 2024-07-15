package life.qbic.io;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import life.qbic.model.petab.MetaInformation;
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
        while (true) {
          String line = reader.readLine();
          if (line == null) {
            break;
          }
          if (line.startsWith("openbisID")) {
            String datasetCode = line.strip().split(": ")[1];
            sourcePetabReferences.add(datasetCode);
          }
        }
        reader.close();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    return new PetabMetadata(sourcePetabReferences);
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

    /*
MetaInformation{
  units=Units{
    measurement='some technique',
    time='min,
    t0 = timepoint of first intervention',
    stimulus='something',
    medium=Medium{type='DMEM', volume=1.5, unit='ml'},
    ncells=CellCountInfo{seeded=0.0, count='null', unit='null'},
    measurement_technique='immunublotting',
    openBISId='null', openBISParentIds=null,
    dateOfExperiment=[2024-04-30]
  },
  preprocessingInformation=null,
  measurementData=MeasurementData{
    measurement=Measurement{
      unit='intensity (a.u.)', lloq='null'},
      time=Time{unit='min'},
      replicateId=IdWithPattern{name='null', pattern='date_gel_replicate'}
    },
    experimentalCondition=ExperimentalCondition{conditionId=IdWithPattern{name='null', pattern='condition'},
    conditions=null}}


    ExperimentInformation:
units:
  measurement: some technique
  time: min, t0 = timepoint of first intervention
  treatment:
  stimulus: something
  medium:
    type: DMEM
    volume: 1.5
    unit: ml
  ncells:
    seeded: 0.4
    ncellsCount: ~
    unit: mio
  measurement_technique: immunublotting
  openBISId: ~
  dateOfExperiment:
    - 2024-04-30
PreprocessingInformation:
  normalizationStatus: Raw (data on linear scale)
  preprocessing:
    method: normalize by blotIt
    arguments:
      housekeeperObservableIds:
    description:
measurementData:
  measurement:
    unit: intensity (a.u.)
    lloq: ~
  time:
    unit: min
  replicateId:
    pattern: date_gel_replicate
experimentalCondition:
  conditionId:
    pattern: condition
  TGFb:
    unit: ng/ul
  GAS6:
    unit: ug/ml

     */
  }

}
