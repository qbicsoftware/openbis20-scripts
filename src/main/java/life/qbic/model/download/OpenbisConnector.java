package life.qbic.model.download;

import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.DataSetPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.search.DataSetSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.ExperimentIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.search.ExperimentSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SampleIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.search.SampleSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.fetchoptions.SpaceFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.search.SpaceSearchCriteria;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.dataset.create.UploadedDataSetCreation;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.DataSetFile;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.download.DataSetFileDownload;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.download.DataSetFileDownloadOptions;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.download.DataSetFileDownloadReader;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.fetchoptions.DataSetFileFetchOptions;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.id.DataSetFilePermId;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.id.IDataSetFileId;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.search.DataSetFileSearchCriteria;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import life.qbic.model.DatasetWithProperties;
import life.qbic.model.OpenbisExperimentWithDescendants;
import life.qbic.model.SampleTypeConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class OpenbisConnector {

  private static final Logger LOG = LogManager.getLogger(OpenbisConnector.class);
  private final OpenBIS openBIS;

  //public final static Pattern experimentIdPattern = Pattern.compile("\\/[A-Za-z0-9]+\\/[A-Za-z0-9]+\\/[A-Za-z0-9]+");
  //public final static Pattern sampleIdPattern = Pattern.compile("\\/[A-Za-z0-9]+\\/[A-Za-z0-9]+");
  public static Pattern datasetCodePattern = Pattern.compile("[0-9]^{17}-[0-9]+");

  public OpenbisConnector(OpenBIS authentication) {
    this.openBIS = authentication;
  }

  public List<String> getSpaces() {
    SpaceSearchCriteria criteria = new SpaceSearchCriteria();
    SpaceFetchOptions options = new SpaceFetchOptions();
    return openBIS.searchSpaces(criteria, options).getObjects()
        .stream().map(Space::getCode).collect(Collectors.toList());
  }

  public DataSetPermId registerDatasetForExperiment(Path uploadPath, String experimentID,
      List<String> parentCodes) {
    UploadedDataSetCreation creation = prepareDataSetCreation(uploadPath, parentCodes);
    creation.setExperimentId(new ExperimentIdentifier(experimentID));

    try {
      return openBIS.createUploadedDataSet(creation);
    } catch (final Exception e) {
      LOG.error(e.getMessage());
    }
    return null;
  }

  public DataSetPermId registerDatasetForSample(Path uploadPath, String sampleID,
      List<String> parentCodes) {
    UploadedDataSetCreation creation = prepareDataSetCreation(uploadPath, parentCodes);
    creation.setSampleId(new SampleIdentifier(sampleID));

    try {
      return openBIS.createUploadedDataSet(creation);
    } catch (final Exception e) {
      LOG.error(e.getMessage());
    }
    return null;
  }

  private UploadedDataSetCreation prepareDataSetCreation(Path uploadPath, List<String> parentCodes) {
    final String uploadId = openBIS.uploadFileWorkspaceDSS(uploadPath);

    final UploadedDataSetCreation creation = new UploadedDataSetCreation();
    creation.setUploadId(uploadId);
    creation.setParentIds(parentCodes.stream().map(DataSetPermId::new).collect(
        Collectors.toList()));
    creation.setTypeId(new EntityTypePermId("UNKNOWN", EntityKind.DATA_SET));
    return creation;
  }

  private static void copyInputStreamToFile(InputStream inputStream, File file)
      throws IOException {
    System.err.println(file.getPath());
    try (FileOutputStream outputStream = new FileOutputStream(file, false)) {
      int read;
      byte[] bytes = new byte[8192];
      while ((read = inputStream.read(bytes)) != -1) {
        outputStream.write(bytes, 0, read);
      }
    }
  }

  public List<DataSet> listDatasetsOfExperiment(List<String> spaces, String experiment) {
    DataSetSearchCriteria criteria = new DataSetSearchCriteria();
    criteria.withExperiment().withCode().thatEquals(experiment);
    if (!spaces.isEmpty()) {
      criteria.withAndOperator();
      criteria.withExperiment().withProject().withSpace().withCodes().thatIn(spaces);
    }
    DataSetFetchOptions options = new DataSetFetchOptions();
    options.withType();
    options.withRegistrator();
    options.withExperiment().withProject().withSpace();
    return openBIS.searchDataSets(criteria, options).getObjects();
  }

  public File downloadDataset(String targetPath, String datasetID) {
    DataSetFileDownloadOptions options = new DataSetFileDownloadOptions();
    IDataSetFileId fileToDownload = new DataSetFilePermId(new DataSetPermId(datasetID),
        "");

    // Setting recursive flag to true will return both subfolders and files
    options.setRecursive(true);

    // Read the contents and print them out
    InputStream stream = openBIS.downloadFiles(new ArrayList<>(List.of(fileToDownload)),
        options);
    DataSetFileDownloadReader reader = new DataSetFileDownloadReader(stream);
    DataSetFileDownload file;
    while ((file = reader.read()) != null) {
      DataSetFile df = file.getDataSetFile();
      String currentPath = df.getPath().replace("original", "");
      if (df.isDirectory()) {
        File newDir = new File(targetPath, currentPath);
        if (!newDir.exists()) {
          if(!newDir.mkdirs()) {
            throw new RuntimeException("Could not create folders for downloaded dataset.");
          }
        }
      } else {
        File toWrite = new File(targetPath, currentPath);
        try {
          copyInputStreamToFile(file.getInputStream(), toWrite);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }
    return new File(targetPath);
  }

  public InputStream streamDataset(String datasetCode, String filePath) {
    DataSetFileDownloadOptions options = new DataSetFileDownloadOptions();
    IDataSetFileId fileToDownload = new DataSetFilePermId(new DataSetPermId(datasetCode),
        filePath);

    // Setting recursive flag to true will return both subfolders and files
    options.setRecursive(true);

    // Read the contents and print them out
    InputStream stream = openBIS.downloadFiles(new ArrayList<>(List.of(fileToDownload)),
        options);

    DataSetFileDownloadReader reader = new DataSetFileDownloadReader(stream);
    return reader.read().getInputStream();
  }

  public Map<SampleTypeConnection, Integer> queryFullSampleHierarchy(List<String> spaces) {
    Map<SampleTypeConnection, Integer> hierarchy = new HashMap<>();
    if (spaces.isEmpty()) {
      spaces = getSpaces();
    }
    for (String space : spaces) {
      SampleFetchOptions fetchType = new SampleFetchOptions();
      fetchType.withType();
      SampleFetchOptions withDescendants = new SampleFetchOptions();
      withDescendants.withChildrenUsing(fetchType);
      withDescendants.withType();
      SampleSearchCriteria criteria = new SampleSearchCriteria();
      criteria.withSpace().withCode().thatEquals(space.toUpperCase());
      SearchResult<Sample> result = openBIS.searchSamples(criteria, withDescendants);
      for (Sample s : result.getObjects()) {
        SampleType parentType = s.getType();
        List<Sample> children = s.getChildren();
        if (children.isEmpty()) {
          SampleTypeConnection leaf = new SampleTypeConnection(parentType);
          if (hierarchy.containsKey(leaf)) {
            int count = hierarchy.get(leaf) + 1;
            hierarchy.put(leaf, count);
          } else {
            hierarchy.put(leaf, 1);
          }
        } else {
          for (Sample c : children) {
            SampleType childType = c.getType();
            SampleTypeConnection connection = new SampleTypeConnection(parentType, childType);
            if (hierarchy.containsKey(connection)) {
              int count = hierarchy.get(connection) + 1;
              hierarchy.put(connection, count);
            } else {
              hierarchy.put(connection, 1);
            }
          }
        }
      }
    }
    return hierarchy;
  }

  private Optional<String> getPropertyFromSampleHierarchy(String propertyName, List<Sample> samples) {
    for(Sample s : samples) {
      if(s.getProperties().containsKey(propertyName)) {
        return Optional.of(s.getProperties().get(propertyName));
      }
      return getPropertyFromSampleHierarchy(propertyName, s.getParents());
    }
    return Optional.empty();
  }

  public Optional<String> findPropertyInSampleHierarchy(String propertyName,
      ExperimentIdentifier experimentId) {
    return getPropertyFromSampleHierarchy(propertyName,
        getSamplesWithAncestorsOfExperiment(experimentId));
  }

  public Map<String, List<Experiment>> getExperimentsBySpace(List<String> spaces) {
    Map<String, List<Experiment>> result = new HashMap<>();
    ExperimentFetchOptions options = new ExperimentFetchOptions();
    options.withProject().withSpace();
    ExperimentSearchCriteria criteria = new ExperimentSearchCriteria();
    criteria.withProject().withSpace().withCodes().thatIn(spaces);
    for (Experiment e : openBIS.searchExperiments(criteria, options).getObjects()) {
      String space = e.getProject().getSpace().getCode();
      if(result.containsKey(space)) {
        result.get(space).add(e);
      } else {
        result.put(space, new ArrayList<>());
      }
    }
    return result;
  }

  public Map<String, List<Sample>> getSamplesBySpace(List<String> spaces) {
    Map<String, List<Sample>> result = new HashMap<>();
    SampleFetchOptions options = new SampleFetchOptions();
    options.withSpace();
    SampleSearchCriteria criteria = new SampleSearchCriteria();
    criteria.withSpace().withCodes().thatIn(spaces);
    for (Sample s : openBIS.searchSamples(criteria, options).getObjects()) {
      String space = s.getSpace().getCode();
      if(!result.containsKey(space)) {
        result.put(space, new ArrayList<>());
      }
      result.get(space).add(s);
    }
    return result;
  }

  public Map<String, Map<String, List<Experiment>>> getExperimentsByTypeAndSpace(List<String> spaces) {
    Map<String, Map<String, List<Experiment>>> result = new HashMap<>();
    ExperimentFetchOptions options = new ExperimentFetchOptions();
    options.withProject().withSpace();
    options.withType();

    ExperimentSearchCriteria criteria = new ExperimentSearchCriteria();
    criteria.withProject().withSpace().withCodes().thatIn(spaces);
    for (Experiment exp : openBIS.searchExperiments(criteria, options).getObjects()) {
      String space = exp.getProject().getSpace().getCode();
      String type = exp.getType().getCode();
      if(!result.containsKey(space)) {
        Map<String, List<Experiment>> typeMap = new HashMap<>();
        typeMap.put(type, new ArrayList<>(Arrays.asList(exp)));
        result.put(space, typeMap);
      } else {
        Map<String, List<Experiment>> typeMap = result.get(space);
        if(!typeMap.containsKey(type)) {
          typeMap.put(type, new ArrayList<>());
        }
        typeMap.get(type).add(exp);
      }
    }
    return result;
  }

  public Map<String, Map<String, List<Sample>>> getSamplesByTypeAndSpace(List<String> spaces) {
    Map<String, Map<String, List<Sample>>> result = new HashMap<>();
    SampleFetchOptions options = new SampleFetchOptions();
    options.withSpace();
    options.withType();

    SampleSearchCriteria criteria = new SampleSearchCriteria();
    criteria.withSpace().withCodes().thatIn(spaces);
    for (Sample s : openBIS.searchSamples(criteria, options).getObjects()) {
      String space = s.getSpace().getCode();
      String type = s.getType().getCode();
      if(!result.containsKey(space)) {
        Map<String, List<Sample>> typeMap = new HashMap<>();
        typeMap.put(type, new ArrayList<>(Arrays.asList(s)));
        result.put(space, typeMap);
      } else {
        Map<String, List<Sample>> typeMap = result.get(space);
        if(!typeMap.containsKey(type)) {
          typeMap.put(type, new ArrayList<>());
        }
        typeMap.get(type).add(s);
      }
    }
    return result;
  }

  public Map<String, Map<String, List<DataSet>>> getDatasetsByTypeAndSpace(List<String> spaces) {
    Map<String, Map<String, List<DataSet>>> result = new HashMap<>();
    DataSetFetchOptions options = new DataSetFetchOptions();
    options.withSample().withSpace();
    options.withExperiment().withProject().withSpace();
    options.withType();
    DataSetSearchCriteria criteria = new DataSetSearchCriteria();
    criteria.withOrOperator();
    criteria.withSample().withSpace().withCodes().thatIn(spaces);
    criteria.withExperiment().withProject().withSpace().withCodes().thatIn(spaces);
    for (DataSet d : openBIS.searchDataSets(criteria, options).getObjects()) {
      String space = getSpaceFromSampleOrExperiment(d);
      String type = d.getType().getCode();
      if(!result.containsKey(space)) {
        Map<String, List<DataSet>> typeMap = new HashMap<>();
        typeMap.put(type, new ArrayList<>(Arrays.asList(d)));
        result.put(space, typeMap);
      } else {
        Map<String, List<DataSet>> typeMap = result.get(space);
        if(!typeMap.containsKey(type)) {
          typeMap.put(type, new ArrayList<>());
        }
        typeMap.get(type).add(d);
      }
    }
    return result;
  }

  private String getSpaceFromSampleOrExperiment(DataSet d) {
    try {
      if (d.getSample() != null) {
        return d.getSample().getSpace().getCode();
      }
      if (d.getExperiment() != null) {
        return d.getExperiment().getProject().getSpace().getCode();
      }
    } catch (NullPointerException e) {

    }
    System.out.println("Dataset " + d + "does not seem to be attached to a space");
    return "NO SPACE";
  }

  private List<Sample> getSamplesWithAncestorsOfExperiment(ExperimentIdentifier experimentId) {
    SampleFetchOptions allProps = new SampleFetchOptions();
    allProps.withType();
    allProps.withProperties();
    SampleFetchOptions withAncestors = new SampleFetchOptions();
    withAncestors.withParentsUsing(allProps);
    withAncestors.withProperties();
    withAncestors.withType();

    SampleSearchCriteria criteria = new SampleSearchCriteria();
    criteria.withExperiment().withId().thatEquals(experimentId);

    return openBIS.searchSamples(criteria, withAncestors).getObjects();
  }

  public List<DataSet> findDataSets(List<String> codes) {
    if (codes.isEmpty()) {
      return new ArrayList<>();
    }
    DataSetSearchCriteria criteria = new DataSetSearchCriteria();
    criteria.withCodes().thatIn(codes);
    DataSetFetchOptions options = new DataSetFetchOptions();
    options.withExperiment();
    options.withType();
    return openBIS.searchDataSets(criteria, options).getObjects();
  }

  public boolean datasetExists(String code) {
    return !findDataSets(new ArrayList<>(Arrays.asList(code))).isEmpty();
  }

  public boolean experimentExists(String experimentID) {
    ExperimentSearchCriteria criteria = new ExperimentSearchCriteria();
    criteria.withIdentifier().thatEquals(experimentID);

    return !openBIS.searchExperiments(criteria, new ExperimentFetchOptions()).getObjects()
        .isEmpty();
  }

  public boolean sampleExists(String objectID) {
    SampleSearchCriteria criteria = new SampleSearchCriteria();
    criteria.withIdentifier().thatEquals(objectID);

    return !openBIS.searchSamples(criteria, new SampleFetchOptions()).getObjects()
        .isEmpty();
  }

  public OpenbisExperimentWithDescendants getExperimentWithDescendants(String experimentID) {
    ExperimentSearchCriteria criteria = new ExperimentSearchCriteria();
    criteria.withIdentifier().thatEquals(experimentID);

    ExperimentFetchOptions fetchOptions = new ExperimentFetchOptions();
    fetchOptions.withType();
    fetchOptions.withProject();
    fetchOptions.withProperties();
    DataSetFetchOptions dataSetFetchOptions = new DataSetFetchOptions();
    dataSetFetchOptions.withType();
    dataSetFetchOptions.withRegistrator();
    SampleFetchOptions sampleFetchOptions = new SampleFetchOptions();
    sampleFetchOptions.withProperties();
    sampleFetchOptions.withDataSetsUsing(dataSetFetchOptions);
    fetchOptions.withDataSetsUsing(dataSetFetchOptions);
    fetchOptions.withSamplesUsing(sampleFetchOptions);

    Experiment experiment = openBIS.searchExperiments(criteria, fetchOptions).getObjects().get(0);

    Map<DataSetPermId, List<DataSetFile>> datasetCodeToFiles = new HashMap<>();
    for(DataSet dataset : experiment.getDataSets()) {
      datasetCodeToFiles.put(dataset.getPermId(), getDatasetFiles(dataset));
    }

    return new OpenbisExperimentWithDescendants(experiment, experiment.getSamples(),
        experiment.getDataSets()
            .stream().map(DatasetWithProperties::new)
            .collect(Collectors.toList()), datasetCodeToFiles);
  }

  public List<DataSetFile> getDatasetFiles(DataSet dataset) {
    DataSetFileSearchCriteria criteria = new DataSetFileSearchCriteria();

    DataSetSearchCriteria dataSetCriteria = criteria.withDataSet().withOrOperator();
    dataSetCriteria.withCode().thatEquals(dataset.getCode());

    SearchResult<DataSetFile> result = openBIS.searchFiles(criteria, new DataSetFileFetchOptions());

    return result.getObjects();
  }
}
