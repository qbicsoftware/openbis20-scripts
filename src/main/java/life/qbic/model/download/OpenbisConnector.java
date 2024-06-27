package life.qbic.model.download;

import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.DataSetPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.search.DataSetSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.ExperimentIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.search.ExperimentSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.search.SampleSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.fetchoptions.SpaceFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.search.SpaceSearchCriteria;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.dataset.create.UploadedDataSetCreation;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.DataSetFile;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.download.DataSetFileDownload;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.download.DataSetFileDownloadOptions;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.download.DataSetFileDownloadReader;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.id.DataSetFilePermId;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.id.IDataSetFileId;
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
import java.util.stream.Collectors;
import life.qbic.model.SampleTypeConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class OpenbisConnector {

  private static final Logger LOG = LogManager.getLogger(OpenbisConnector.class);
  OpenBIS openBIS;
  /**
   * Constructor for a QBiCDataDownloader instance
   *
   * @param AppServerUri  The openBIS application server URL (AS)
   * @param sessionToken The session token for the datastore & application servers
   */
  public OpenbisConnector(
          String AppServerUri,
          String sessionToken) {
    /*
    this.sessionToken = sessionToken;

    if (!AppServerUri.isEmpty()) {
      applicationServer =
          HttpInvokerUtils.createServiceStub(
              IApplicationServerApi.class, AppServerUri + IApplicationServerApi.SERVICE_URL, 10000);
    } else {
      applicationServer = null;
    }

     */
  }

  public OpenbisConnector(OpenBIS authentication) {
    this.openBIS = authentication;
  }

  public List<String> getSpaces() {
    SpaceSearchCriteria criteria = new SpaceSearchCriteria();
    SpaceFetchOptions options = new SpaceFetchOptions();
    return openBIS.searchSpaces(criteria, options).getObjects()
        .stream().map(Space::getCode).collect(Collectors.toList());
  }

  public DataSetPermId registerDataset(Path uploadPath, String experimentID, List<String> parentCodes) {
    final String uploadId = openBIS.uploadFileWorkspaceDSS(uploadPath);

    final UploadedDataSetCreation creation = new UploadedDataSetCreation();
    creation.setUploadId(uploadId);
    creation.setExperimentId(new ExperimentIdentifier(experimentID));
    creation.setParentIds(parentCodes.stream().map(DataSetPermId::new).collect(
        Collectors.toList()));
    creation.setTypeId(new EntityTypePermId("UNKNOWN", EntityKind.DATA_SET));

    try
    {
      return openBIS.createUploadedDataSet(creation);
    } catch (final Exception e)
    {
      e.printStackTrace();
    }
    return null;
  }

  public static void main(String[] args) throws IOException {
    String as = "";
    String dss = "";
    String user = "";
    String pass = "";

    OpenBIS authentication =
        new OpenBIS(as, dss);
      authentication.login(user, pass);
    OpenbisConnector c = new OpenbisConnector(authentication);

    String space = "TEMP_PLAYGROUND";
    String project = "TEMP_PLAYGROUND";
    String experiment = "E123";
    String expID = "/TEMP_PLAYGROUND/TEMP_PLAYGROUND/E123";
    Path toUpload = Path.of("/Users/afriedrich/Downloads/cats");
    //c.registerDataset(toUpload, expID, List.of("20240512182451335-425180", "20240512191248297-425181"));

    String basePath = "/Users/afriedrich/Downloads/downloaded_cats";
    //c.downloadDataset(basePath, "20240503175717814-263844");
    c.listDatasetsOfExperiment(List.of(), experiment);

  }


private static void copyInputStreamToFile(InputStream inputStream, File file)
    throws IOException {
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
  if(!spaces.isEmpty()) {
    criteria.withAndOperator();
    criteria.withExperiment().withProject().withSpace().withCodes().thatIn(spaces);
  }
  DataSetFetchOptions options = new DataSetFetchOptions();
  options.withType();
  options.withRegistrator();
  options.withExperiment().withProject().withSpace();
  return openBIS.searchDataSets(criteria, options).getObjects();
}

  public void downloadDataset(String targetPath, String datasetID) throws IOException {
    DataSetFileDownloadOptions options = new DataSetFileDownloadOptions();
    IDataSetFileId fileToDownload = new DataSetFilePermId(new DataSetPermId(datasetID),
        "");//TODO test this path

    // Setting recursive flag to true will return both the subfolder directory object AND file3.txt
    options.setRecursive(true);

    // Setting recursive flag to false will return just the meta data of the directory object
    //options.setRecursive(false);

    // Read the contents and print them out
    InputStream stream = openBIS.downloadFiles(new ArrayList<>(Arrays.asList(fileToDownload)), options);
    DataSetFileDownloadReader reader = new DataSetFileDownloadReader(stream);
    DataSetFileDownload file = null;
    while ((file = reader.read()) != null)
    {
      DataSetFile df = file.getDataSetFile();
      String currentPath = df.getPath().replace("original","");
      if(df.isDirectory()) {
          File newDeer = new File(targetPath, currentPath);
          if (!newDeer.exists()) {
            newDeer.mkdirs();
          }
      } else {
        File toWrite = new File(targetPath, currentPath);
        copyInputStreamToFile(file.getInputStream(), toWrite);
      }
    }
    }

  public Map<SampleTypeConnection, Integer> queryFullSampleHierarchy(List<String> spaces) {
    Map<SampleTypeConnection, Integer> hierarchy = new HashMap<>();
    if(spaces.isEmpty()) {
      spaces = getSpaces();
    }
    for(String space : spaces) {
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

  public List<DataSet> findDataSets(List<String> codes) {
    DataSetSearchCriteria criteria = new DataSetSearchCriteria();
    criteria.withCodes().thatIn(codes);
    DataSetFetchOptions options = new DataSetFetchOptions();
    return openBIS.searchDataSets(criteria, options).getObjects();
  }

  public boolean experimentExists(String experimentID) {
    ExperimentSearchCriteria criteria = new ExperimentSearchCriteria();
    criteria.withIdentifier().thatEquals(experimentID);

    return !openBIS.searchExperiments(criteria, new ExperimentFetchOptions()).getObjects().isEmpty();
  }
}
