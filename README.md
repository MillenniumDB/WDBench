# WDBench

In this repository you can find the data files and queries used in the benchmarking section for WDBench.

## Table of contents

- [Wikidata data](#wikidata-data)
- [Data Loading](#data-loading)
  - [Apache Jena](#data-loading-for-apache-jena)
  - [Virtuoso](#data-loading-for-virtuoso)
  - [Blazegraph](#data-loading-for-blazegraph)
  - [Neo4J](#data-loading-for-neo4j)
- [Wikidata queries](#wikidata-queries)
- [Running the benchmark](#running-the-benchmark)

# Wikidata data

The data used in this benchmark is based on the [Wikidata Truthy](https://iccl.inf.tu-dresden.de/web/Wikidata_SPARQL_Logs/en) from 2021-06-23. We cleaned the data removing all triples whose predicate is not a direct property (i.e `http://www.wikidata.org/prop/direct/P*`). The data is available to download from [Figshare](https://figshare.com/s/50b7544ad6b1f51de060).

The script to generate these data from the [original data](https://www.wikidata.org/wiki/Wikidata:Database_download) is in our [source folder](/src/database_generation/filter_direct_properties.py).

# Data loading

## Data loading for Apache Jena

### 1. Prerequisites

Apache Jena requires Java JDK (we used Openjdk 11, other versions might work as well)

The installation may be different depending on your Linux distribution. For Debian/Ubuntu based distributions:

- `sudo apt update`
- `sudo apt install openjdk-11-jdk`

### 2. Download Apache Jena

You can download Apache Jena from their [website](https://jena.apache.org/download/) . The file you need to download will look like `apache-jena-4.X.Y.tar.gz`, in our case, we used the version `4.1.0`, but this should also work for newer versions.

### 3. Extract and change into the project folder

- `tar -xf apache-jena-4.*.*.tar.gz`
- `cd apache-jena-4.*.*/`

### 4. Execute the bulk import

- `bin/tdbloader2 --loc=[path_of_new_db_folder] [path_of_nt_file]`

### 5. Import for leapfrog version

This step is necessary only if you want to use the Leapfrog Jena implementation, you can skip this otherwise.

Edit the text file `bin/tdbloader2index` and search for the lines:

```
generate_index "$K1 $K2 $K3" "$DATA_TRIPLES" SPO

generate_index "$K2 $K3 $K1" "$DATA_TRIPLES" POS

generate_index "$K3 $K1 $K2" "$DATA_TRIPLES" OSP
```

After those lines add:

```
generate_index "$K1 $K3 $K2" "$DATA_TRIPLES" SOP

generate_index "$K2 $K1 $K3" "$DATA_TRIPLES" PSO

generate_index "$K3 $K2 $K1" "$DATA_TRIPLES" OPS
```

Now you can execute the bulk import in the same way we did it before:

- `bin/tdbloader2 --loc=[path_of_new_db_folder] [path_of_nt_file]`

In order to be able to run the benchmark for Leapfrog Jena you also need to use a custom fuseki-server.jar
- Install openjdk-8, mvn and set `JAVA_HOME` to use java 8
- `git clone https://github.com/cirojas/jena-leapfrog`
- `cd jena-leapfrog`
- `mvn clean install -Drat.numUnapprovedLicenses=100 -Darguments="-Dmaven.javadoc.skip=true" -DskipTests`
- Use `jena-fuseki2/apache-jena-fuseki/target/apache-jena-fuseki-3.9.0.tar.gz` instead of the one you download normally.


## Data loading for Virtuoso

### 1. Edit the .nt

Virtuoso has a problem with geo-datatypes so we generated a new .nt file to prevent them from being parsed as a geo-datatype.

- `sed 's/#wktLiteral/#wktliteral/g' [path_of_nt_file] > [virtuoso_nt_file]`

### 2. Download Virtuoso

You can download Virtuoso from [their github](https://github.com/openlink/virtuoso-opensource/releases).
We used Virtuoso Open Source Edition, version 7.2.6.

- Download:
  - `wget https://github.com/openlink/virtuoso-opensource/releases/download/v7.2.6.1/virtuoso-opensource.x86_64-generic_glibc25-linux-gnu.tar.gz`
- Extract:
  - `tar -xf virtuoso-opensource.x86_64-generic_glibc25-linux-gnu.tar.gz`
- Enter to the folder:
  - `cd virtuoso-opensource`

### 3. Create configuration file

- We start from their example configuration file:

  - `cp database/virtuoso.ini.sample wikidata.ini`

- Edit `wikidata.ini` with a text editor, when you edit a path, we recomend using the absolute path:

  - replace every `../database/` with the path of the database folder you want to create.

  - add the path of folder where you have `[virtuoso_nt_file]` and the path of the database folder you want to create to `DirsAllowed`.

  - change `VADInstallDir` to the path of `virtuoso-opensource/vad`.

  - set `NumberOfBuffers`. For loading the data we used `7200000`, to run experiments we used `5450000`.

  - set `MaxDirtyBuffers`. For loading the data we used `5400000`, to run experiments we used `4000000`.

  - revise `ResultSetMaxRows`, our experiments set this to `1000000`

  - revise `MaxQueryCostEstimationTime`, our experiments commented this out with ';' before the line removing the limit

  - revise `MaxQueryExecutionTime`, our experiments used `600` for 10 minute timeouts

  - add at the end of the file these lines:

    ```
    [Flags]
    tn_max_memory = 2755359740
    ```

### 4. Load the data

- Start the server: `bin/virtuoso-t -c wikidata.ini +foreground`

  - This process won't end until you interrupt it (Ctrl+C). Let this execute until the import ends. Run the next command in another terminal.

- `bin/isql localhost:1111`

  And inside the `isql` console run:

  - `ld_dir('[path_to_virtuoso_folder]', '[virtuoso_nt_file]', 'http://wikidata.org/);`
  - `rdf_loader_run();`

## Data loading for Blazegraph

### 1. Prerequisites

You'll need the following prerequisites installed:

- Java JDK (with `$JAVA_HOME` defined and `$JAVA_HOME/bin` on `$PATH`)
- Maven
- Git

The installation may be different depending on your Linux distribution. For Debian/Ubuntu based distributions:

- `sudo apt update`
- `sudo apt install openjdk-11-jdk mvn git`

### 2. Split .nt file into smaller files

Blazegraph can't load big files in a reasonable time, so we need to split the .nt into smaller files (1M each)

- `mkdir splitted_nt`
- `cd splitted_nt`
- `split -l 1000000 -a 4 -d --additional-suffix=.nt [path_to_nt]`
- `cd ..`

### 3. Clone the Git repository and build

- `git clone --recurse-submodules https://gerrit.wikimedia.org/r/wikidata/query/rdf wikidata-query-rdf`
- `cd wikidata-query-rdf`
- `mvn package`
- `cd dist/target`
- `tar xvzf service-*-dist.tar.gz`
- `cd service-*/`
- `mkdir logs`

### 4. Edit the default script

- Edit the script file `runBlazegraph.sh` with any text editor.
  - configure main memory here: `HEAP_SIZE=${HEAP_SIZE:-"64g"}` (You may use other value depending on how much RAM your machine has)
  - set the log folder `LOG_DIR=${LOG_DIR:-"/path/to/logs"}`, replace `/path/to/logs` with the absolute path of the `logs` dir you created in the previous step.
  - add `-Dorg.wikidata.query.rdf.tool.rdf.RdfRepository.timeout=600` to the `exec java` command to specify the timeout (value is in seconds).
  - also change `-Dcom.bigdata.rdf.sparql.ast.QueryHints.analyticMaxMemoryPerQuery=0` which removes per-query memory limits.

### 5. Load the splitted data

- Start the server: `./runBlazegraph.sh`
  - This process won't end until you interrupt it (Ctrl+C). Let this execute until the import ends. Run the next command in another terminal.
- Start the import: `./loadRestAPI.sh -n wdq -d [path_of_splitted_nt_folder]`

## Data loading for Neo4J

### 1. Download Neo4J

- Download Neo4J community edition from their website https://neo4j.com/download-center/#community . We used the version 4.3.5 but this instructions might work for newer versions.
- Extract the downloaded file
  - `tar -xf neo4j-community-4.*.*-unix.tar.gz`
- Enter to the folder:
  - `cd neo4j-community-4.*.*/`
- Set the variable `$NEO4J_HOME` pointing to the Neo4J folder (using `export` and adding it to .bashrc/.zshrc)

### 2. Edit configuration file

Edit the text file `conf/neo4j.conf`

- Set `dbms.default_database=wikidata`
- Uncomment the line `dbms.security.auth_enabled=false`
- Add the line `dbms.transaction.timeout=10m`

### 3. Convert .nt to .csv files

Use the script [nt_to_neo4j.py](/src/database_generation/nt_to_neo4j.py) to generate the .csv files `entities.csv`, `literals.csv` and `edges.csv`

### 4. Bulk import and index

Execute the data import

```
bin/neo4j-admin import --database wikidata \
 --nodes=Entity=wikidata_csv/entities.csv \
 --nodes wikidata_csv/literals.csv \
 --relationships wikidata_csv/edges.csv \
 --delimiter "," --array-delimiter ";" --skip-bad-relationships true
```

You should have the `.csv` files in the `wikidata_csv` folder.

Now we have to create the index for entities:

- Start the server: `bin/neo4j console`
  - This process won't end until you interrupt it (Ctrl+C). Let this execute until the index creation is finished. Run the next command in another terminal.

- Open the cypher console:
  - `bin/cypher-shell`, and inside the console run the command:
    - `CREATE INDEX ON :Entity(id);`
    - Even though the above command returns immediately, you have to wait until is finished before interrupting the server. You can see the status of the index with the command `SHOW INDEXES;`

# Wikidata Queries

In this benchmark we have 5 sets of queries:
- Basic Graph Patterns (BGPs):
  - Single BGPs : 280 queries
  - Multiple BGPs: 681 queries
- Optionals: 498
- Property Paths : 660 queries
- C2RPQs: 539 queries

We provide the SPARQL queries in our [queries folder](/Queries). Also we provide the equivalent cypher property paths (it has fewer queries because some property paths cannot be expressed in cypher).

**Single BGPs**, **Multiple BGPs** and **Property Paths** are based on real queries extracted from the [Wikidata SPARQL query log](https://iccl.inf.tu-dresden.de/web/Wikidata_SPARQL_Logs/en). This log contains
millions of queries, but many of them are trivial to evaluate.
We thus decided to generate our benchmark from more challenging
cases, i.e., a smaller log of queries that timed-out on the [Wikidata
public endpoint](https://query.wikidata.org/). From these queries we extracted their BGPs and property paths removing duplicates (modulo isomorphism on query variables).
Then we filtered with the same criteria that we applied to the data, removing all queries having predicates that are not a direct property (`http://www.wikidata.org/prop/direct/P*`). Next, for property paths we removed queries that have both subject and object as variables and for BGPs we removed queries having a triple in which subject, predicate and object are variables.
Finally, we distinguish BGPs queries consisting of a single triple pattern (Single BGPs) from those containing more than one triple pattern (Multiple BGPs).

## Running the benchmark

Here we provide a description of the scripts we used for the execution of the queries.

Our scripts will execute a list of queries for a certain engine, one at a time, and register the time and number of results for each query in a csv file.

Every time you want to run a benchmark script you must clear the cache of the system before. To do this, run as root:
- `sync; echo 3 > /proc/sys/vm/drop_caches`

Each script has a **parameters section** near the beginning of the file, (e.g. database paths, output folder) make sure to edit the script to set them properly.


