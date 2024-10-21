# OpenBIS Scripts User Documentation

## OpenBIS Statistics and Data Model

In order to interact with openBIS, the parameters for the application server URL, the user and their password, have to be provided.

In order to interact with data in openBIS (upload or download), a data store (dss) URL needs to be provided, as well.

**Everything but the password can be provided via the config file (config.txt):**

* as=https://my-openbis-instance.de/openbis/openbis
* dss=https://my-openbis-instance.de/datastore_server
* user=your-username

Keep in mind that you have to edit the config file or provide these parameters via command line, if you want to use different users or connect to different openBIS instances.

Refer to the help of the respective command or the examples below for more details.

### Finding Datasets

The list-data command can be used to list Datasets in openBIS and some of their metadata based on the experiment or sample they are attached to. Experiments or samples are specified by their openBIS code or identifier.

The optional 'space' parameter can be used to only show datasets found in the provided space.

**Example command:**

`java -jar scripts.jar list-data /SPACY/PROJECTX/TEST_PATIENTS1 -config config.txt --openbis-pw`

**Example output:**

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

### Showing Space Statistics

The Statistics command can be used to list the number of collections, sample objects and attached datasets by type for one or all spaces accessible by the user.

The --space command can be used to only show the objects in a specific openBIS space. 

An output file for the resulting list can be specified using the --out command. 

By default, openBIS settings objects and material spaces are ignored. This can be overwritten using --show-settings.

**Example command:**

`java -jar scripts.jar statistics -config config.txt --openbis-pw`

**Example output:**

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

### Showing Sample Hierarchy

The Sample Types command queries all sample types and prints which types are connected and how often (via samples existing in the queried openBIS instance), creating a sample type hierarchy.

The --space option can be used to only show the sample-types used in a specific openBIS space.

An output file for the resulting hierarchy can be specified using the --out command.

**Example command:**

`java -jar scripts.jar statistics -config config.txt --openbis-pw`

**Example output:**

    Querying samples in all available spaces...
    MATERIAL.CHEMICALS (1)
    PATIENT_ID (1)
    PATIENT_ID -> PATIENT_ID (1)
    05_MS_RUN (1)
    00_PATIENT_INFO -> 01_EXPERIMENT_PRIMARYCULTURE (3)
    04_IMMUNOBLOT (4)
    03_SDS_PAGE_SETUP -> 04_IMMUNOBLOT (4)

## Upload/Download and Interaction with PEtab

### Uploading general data

The Upload Dataset command can be used to upload a Dataset to openBIS and connect it to existing 
datasets.

To upload a dataset, the path to the file or folder and the object ID to which it should be attached 
need to be provided. Objects can be experiments or samples.

Parent datasets can be specified using the --parents command.

If the specified object ID or any of the specified parent datasets cannot be found, the script will 
stop and return an error message.

The dataset type of the new dataset in openBIS can be specified using the --type option, otherwise 
the type "UNKNOWN" will be used.

**Example command:**

`java -jar scripts.jar upload-data README.md /SPACY/PROJECTX/MY_SAMPLE -t ATTACHMENT -config config.txt --openbis-pw`

**Example output:**

    Parameters verified, uploading dataset...

    Dataset 20241021125328024-689105 was successfully attached to experiment`

### Downloading a PEtab dataset

The Download PEtab command can be used to download a PEtab Dataset from openBIS and store some 
additional information from openBIS in the metaInformation.yaml file (or a respective yaml file 
containing 'metaInformation' in its name).

The Dataset to download is specified by providing the openBIS dataset identifier (code) and the 
PEtab is downloaded to the download path parameter provided.

By design, the Dataset Identifier is added to the downloaded metaInformation.yaml as 'openBISId' 
in order to keep track of the source of this PEtab.

### Uploading a PEtab dataset

The Upload PEtab command can be used to upload a PEtab Dataset to openBIS and connect it to its 
source files if these are stored in the same openBIS instance and referenced in the PEtabs metadata. 

To upload a PEtab dataset, the path to the PEtab folder and the experiment ID to which it should be 
attached need to be provided.

The dataset type of the new dataset in openBIS can be specified using the --type option, otherwise 
the type "UNKNOWN" will be used.

The script will search the **metaInformation.yaml** for the entry "**openBISSourceIds:**" and attach 
the new dataset to all the datasets with ids in the following blocks found in this instance of 
openBIS:
    
    openBISSourceIds:
        - 20210702093837370-184137
        - 20220702100912333-189138

If one or more dataset identifiers are not found, the script will stop without uploading the data 
and inform the user.

## Interaction with SEEK instances

In order to interact with SEEK, the parameters for the server URL, the user (usually an email 
address) and their password, have to be provided.

In order to interact with openBIS (transfer of data and metadata), the respective credentials need 
to be provided, as well.

**Everything but the passwords can be provided via the config file (config.txt):**

* as=https://my-openbis-instance.de/openbis/openbis
* dss=https://my-openbis-instance.de/datastore_server
* user=your-openbis-username
* seek_user=your@email.de
* seek_url=http://localhost:3000

**Furthermore, names of default project and investigation in SEEK can be provided:**

* seek_default_project=seek_test
* seek_default_investigation=default_investigation

In order to keep track of samples transferred from openBIS, the script will try to transfer the 
openBIS identifier of each sample to an additional SEEK sample type attribute (more details in the 
section **Transferring Sample Types to SEEK**).

**Its name can be specified in the config:**

* seek_openbis_sample_title=openBIS Name

Refer to the help of the respective command or the examples below for more details.

### Transferring Sample Types to SEEK

The Sample Type Transfer command transfers sample types and their attributes from an openBIS to a 
SEEK instance.

In order to do this, a mapping of the respective data types/sample attribute types needs to be 
specified. This can be found (and changed) in the provided property file 
**openbis_datatype_to_seek_attributetype.xml**:

        <entry type="REAL">
                <seek_id>3</seek_id>
                <seek_title>Real number</seek_title>
                <seek_type>Float</seek_type>
        </entry>

Each **entry type** denotes the **data type** in openBIS. Note that SEEK needs the identifier of the 
respective sample attribute type (here: *3* for *Float*) in its database. Any changes to the file 
need to reflect this.

The available types and their identifiers can be queried at the endpoint **sample_attribute_types**. 
For example:
`http://localhost:3000/sample_attribute_types`

When transferring openBIS sample types, the command will automatically add a mandatory title 
attribute to the sample type in SEEK. This title will be filled with the identifier of the openBIS 
**sample object** (not sample type!) will be added. The attribute name is specified in the config 
file and should selected before sample types are transferred to the respective instance:
* seek_openbis_sample_title=openBIS Name

By default, only sample types (not samples!) with names not already found in SEEK will be 
transferred and the user will be informed if duplicates are found. The option **--ignore-existing** 
can be used to transfer existing sample types a second time, although it is recommended to only use 
this option for testing purposes.

### Transferring openBIS objects and files to SEEK

The OpenBIS to Seek command transfers metadata and (optionally) data from openBIS to SEEK.
Experiments, samples and dataset information are always transferred together (as assays, samples and
one of several **asset types** in SEEK).

The script will try to find the provided **openbis ID** in experiments, samples or datasets and
fetch any missing information to create a SEEK node containing at least one assay (when an
experiment without samples and datasets is specified).

The seek-study needs to be provides to attach the assay. TODO: This information is also used to 
decide if the node(s) should be updated (if they exist for the provided study) or created anew.

Similarly, the title of the project in SEEK where nodes should be added, can either be provided via 
the config file as **'seek_default_project'** or via the command line using the **--seek-project** 
option.

Info in the created asset(s) always links back to the openBIS path of the respective dataset. 
The data itself can be transferred and stored in SEEK using the '-d' flag.

To completely exclude some dataset information from being transferred, a file ('--blacklist') 
containing dataset codes (from openBIS) can be specified. //TODO do this for samples/sample types

### Updating nodes in SEEK based on updates in openBIS

Updating nodes in SEEK uses the same general command, parameters and options. Unless otherwise 
specified (**--no-update** flag), the command will try to update existing nodes in SEEK (recognized 
by openBIS identifiers in their metadata, as well as the provided study name).

The updating of a node-structure is done based on the following rules:
1. if an assay contains the openBIS permID of the experiment AND is attached to specified study, 
its samples and assets are updated
2. samples are created, if the openBIS name of the respective sample is not found in a sample
attached to the assay in question
3. samples are updated, if their openBIS name is found in a sample attached to the asset and at
least one sample attribute is different in openBIS and SEEK
4. assets attached to the experiment or samples will be created, if they are missing from this assay
5. no existing sample or assets are deleted from SEEK, even if they are missing from openBIS

## Caveats and Future Options
