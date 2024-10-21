# OpenBIS Scripts

## User Documentation

### OpenBIS Statistics and Data Model

In order to interact with openBIS, the parameters for the application server URL, the user and their password, have to be provided.

In order to interact with data in openBIS (upload or download), a data store (dss) URL needs to be provided, as well.

Everything but the password can be provided via the config file (config.txt):

* as=https://my-openbis-instance.de/openbis/openbis
* dss=https://my-openbis-instance.de/datastore_server
* user=your-username

Keep in mind that you have to edit the config file or provide these parameters via command line, if you want to use different users or connect to different openBIS instances.

Refer to the help of the respective command or the examples below for more details.

#### Finding Datasets

The list-data command can be used to list Datasets in openBIS and some of their metadata based on the experiment or sample they are attached to. Experiments or samples are specified by their openBIS code or identifier.

The optional 'space' parameter can be used to only show datasets found in the provided space.

Example command:

`java -jar scripts.jar list-data /SPACY/PROJECTX/TEST_PATIENTS1 -config config.txt --openbis-pw`

Example output:

    reading config
    Querying experiment in all available spaces...
    Query is not an object code, querying for: TEST_PATIENTS1 instead.
    [main] INFO org.eclipse.jetty.util.log - Logging initialized @6997ms to org.eclipse.jetty.util.log.Slf4jLog
    Found 4 datasets for experiment TEST_PATIENTS1:
    [1]
    patientIDs: ID 4930-72,ID 4931-79
    ID: 20241014000813459-189089 (/TEST_PATIENTS1)
    Type: UNKNOWN
    Uploaded by Friedrich Andreas (10-14-24 20:58:13)

    [2]
    patientIDs: ID 4930-72,ID 4931-79
    ID: 20241000010001025-189090 (/SPACY/RPOJECTX/TEST_PATIENTS1)
    Type: UNKNOWN
    Uploaded by Friedrich Andreas (10-14-24 21:00:01)

#### Showing Space Statistics

The Statistics command can be used to list the number of collections, sample objects and attached datasets by type for one or all spaces accessible by the user.

The --space command can be used to only show the objects in a specific openBIS space. 

An output file for the resulting list can be specified using the --out command. 

By default, openBIS settings objects and material spaces are ignored. This can be overwritten using --show-settings.

Example command:

`java -jar scripts.jar statistics -config config.txt --openbis-pw`

Example output:

    Querying samples in all available spaces...
    -----
    Summary for TEMP_PLAYGROUND
    -----
    Experiments (9):

    02_MASSSPECTROMETRY_EXPERIMENT: 1
    00_STANDARD_OPERATING_PROTOCOLS: 1
    00_PATIENT_DATABASE: 3
    01_BIOLOGICAL_EXPERIMENT: 4

    Samples (316):

    00_PRIMARY_BLOOD: 128
    03_SDS_PAGE_SETUP: 1
    00_PRIMARY_TISSUE: 24
    PLASMID: 161
    02_EXPERIMENT_TREATMENT: 2

    Attached datasets (30):

    IB_DATA: 4
    SOURCE_CODE: 2
    RAW_DATA: 1
    EXPERIMENT_RESULT: 3
    MS_DATA_RAW: 1
    UNKNOWN: 18
    EXPERIMENT_PROTOCOL: 1

#### Showing Sample Hierarchy

The Sample Types command queries all sample types and prints which types are connected and how often (via samples existing in the queried openBIS instance), creating a sample type hierarchy.

The --space option can be used to only show the sample-types used in a specific openBIS space.

An output file for the resulting hierarchy can be specified using the --out command.

Example command:

`java -jar scripts.jar statistics -config config.txt --openbis-pw`

Example output:

    Querying samples in all available spaces...
    MATERIAL.CHEMICALS (1)
    PATIENT_ID (1)
    PATIENT_ID -> PATIENT_ID (1)
    05_MS_RUN (1)
    00_PATIENT_INFO -> 01_EXPERIMENT_PRIMARYCULTURE (3)
    04_IMMUNOBLOT (4)
    03_SDS_PAGE_SETUP -> 04_IMMUNOBLOT (4)

### Upload/Download and Interaction with PEtab

#### Uploading general data

The Upload Dataset command can be used to upload a Dataset to openBIS and connect it to existing datasets.

To upload a dataset, the path to the file or folder and the object ID to which it should be attached need to be provided. Objects can be experiments or samples.

Parent datasets can be specified using the --parents command.

If the specified object ID or any of the specified parent datasets cannot be found, the script will stop and return an error message.

The dataset type of the new dataset in openBIS can be specified using the --type option, otherwise the type "UNKNOWN" will be used.

Example command:

`java -jar scripts.jar upload-data README.md /SPACY/PROJECTX/MY_SAMPLE -t ATTACHMENT -config config.txt --openbis-pw`

Example output:

    Parameters verified, uploading dataset...

    Dataset 20241021125328024-689105 was successfully attached to experiment`

#### Downloading a PEtab dataset

#### Uploading a PEtab dataset

### Interaction with SEEK instances

#### Transferring Sample Types to SEEK

#### Transferring openBIS objects and files to SEEK

#### Updating nodes in SEEK based on updates in openBIS

### Caveats and Future Options