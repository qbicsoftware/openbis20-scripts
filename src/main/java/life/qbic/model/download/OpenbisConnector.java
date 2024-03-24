package life.qbic.model.download;

import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.search.SampleSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.fetchoptions.SpaceFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.search.SpaceSearchCriteria;
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


}
