# maha-druid-lookups-example

* This is example project about how to deploy RocksDB based maha-druid-lookups to druid. 
This example is using the Serialization format as ProtoBuf. maha-druid-lookups also supports the FlatBuffer, I will add another example on FlatBuffer schema. 

* FlatBuffer performs very well if you have large number of lookup entries (saves lot of gc time as FlatBuffer dont have to DeSer the entire message like protobuf, it can chery pick the field values based on offset, on historical/broker).


* What are Maha-Druid-Lookups? 
  
    `An extension to druid which provides for MongoDB, JDBC and RocksDB (for high cardinality dimensions) based lookups. For RocksDB, the lookups provide an interface to update entities via Kafka topics using the same protobuf format utilized for reading the RocksDB lookups. The default behavior for RocksDB is to grab a snapshot from HDFS daily and apply updates from Kafka from beginning of topic retention period.
    `      
    
  To Learn more visit :  https://github.com/yahoo/maha/tree/master/druid-lookups  


## Learn druid rocksdb lookup example
 
  * Follow getting started guide for maha-druid-lookups until "Starting up Druid"
  * We are creating rocksdb look up based on the following example protobuf schema called Customer
  ```$xslt
package org.maha_druid_lookups_example;

option java_outer_classname = "CustomerProtos";

message Customer {
    optional string id = 1;
    optional string name = 2;
    optional string address = 3;
    optional string status = 4;
    optional string last_updated = 5;

```
* created java protobuf class called CustomerProtos
* create ProtoBuf schema factory which inherit the default factory impl from maha
```package org.maha_druid_lookups_example;
   
   import com.google.common.collect.ImmutableMap;
   import com.google.protobuf.GeneratedMessageV3;
   import com.yahoo.maha.maha_druid_lookups.server.lookup.namespace.schema.protobuf.DefaultProtobufSchemaFactory;
   
   public class ExampleProtobufSchemaFactory extends DefaultProtobufSchemaFactory {
       public ExampleProtobufSchemaFactory() {
           super(ImmutableMap.<String, GeneratedMessageV3>of("customer_lookup", CustomerProtos.Customer.getDefaultInstance()));
       }
   }
```

# Step 1: 
Clone repo ```cd maha-druid-lookups-example``` and run 
```mvn clean install```

# Step 2: 
 * Run the class CreateExampleRocksDBInstance which creates example rocksdb with Customer loaded from key 1 to 100.
  ```
    mvn exec:java -Dexec.mainClass="org.maha_druid_lookups_example.CreateExampleRocksDBInstance"
  ```
 * You will find that ```target/load_time=202012050000/rocksdb.zip``` is created
 * In this example, we are going to use local file system in the place of real hdfs cluster
 * Rocksdb Lookups expect _SUCCESS marker in the path 
 ```target/load_time=202012050000/_SUCCESS```, create an empty file as success marker
 using ```echo "" > target/load_time=202012050000/_SUCCESS ```
 
# Step 3:
* Configure Schema factory for Cutomers, set the following config in common.runtime.properties in _common conf in druid
```druid.lookup.maha.namespace.schemaFactory=org.maha_druid_lookups_example.ExampleProtobufSchemaFactory```
* schemaFactory configure the namespace to ProtoBuf class mapping, this tells maha-druid-lookups to DeSer values based on the Proto schema 
* Place your core.site.xml to druid _common conf dir, in the example we are using local file system, thus copy
 conf/druid/single-server/micro-quickstart/_common/core-site.xml  
```
<configuration>
       <property>
           <name>fs.defaultFS</name>
           <value>file:///</value>
       </property>
   </configuration>
``` 
to core-site.xml 

# Step 3:
* Place target/maha-druid-lookups-example-1.0-SNAPSHOT.jar to extensions/maha-druid-lookups/

```extensions/maha-druid-lookups/maha-druid-lookups-example-1.0-SNAPSHOT.jar ```
* Restart Druid Service

# Step 4:
* Create Rocksdb lookup on historical node, maha-druid-lookups expect that rocksdb.zip and _SUCCESS marker to be present under dir 
`/load_time=202012050000/` where Ts in formatted as local time `yyyyMMdd0000`. Load Time allows lookup to have multiple revision exists on hdfs.
 Maha-druid-lookups attempts to download the latest available lookup from today or the day before. 


```
{
  "historical": {
    "customer_lookup": {
      "version": "v0",
      "lookupExtractorFactory": {
        "type": "cachedNamespace",
        "extractionNamespace": {
          "type": "maharocksdb",
          "namespace": "customer_lookup",
          "lookupName": "customer_lookup",
          "rocksDbInstanceHDFSPath": "/Users/pranavbhole/git/maha-druid-lookups-example/target",
          "lookupAuditingHDFSPath": "/tmp",
          "pollPeriod": "PT30S",
          "cacheEnabled": true
        }
      }
    }
  }
}
```

Post the above json to 
```http://localhost:8081/druid/coordinator/v1/lookups/config```
as per getting started guide from maha-druid-lookups. 

# Step 5: 
* wait for some time and tail druid historical logs, you will see that maha-druid-lookups has downloaded the zip file from local dir with hdfs client

```$xslt
2020-12-06T06:55:22,366 INFO [main] org.apache.druid.java.util.common.lifecycle.Lifecycle - Successfully started lifecycle [module]
2020-12-06T06:55:22,721 INFO [NodeRoleWatcher[COORDINATOR]] org.apache.druid.curator.discovery.CuratorDruidNodeDiscoveryProvider$NodeRoleWatcher - Node[http://localhost:8081] of role[coordinator] detected.
2020-12-06T06:55:23,513 INFO [qtp700249373-84] com.yahoo.maha.maha_druid_lookups.query.lookup.namespace.RocksDBExtractionNamespace - no input from overrideLookupServiceHosts
2020-12-06T06:55:23,515 WARN [qtp700249373-84] com.yahoo.maha.maha_druid_lookups.server.lookup.namespace.schema.flatbuffer.FlatBufferSchemaFactoryProvider - Implementation of FlatBufferSchemaFactory class name is black in the MahaNamespaceExtractionConfig, considering default implementation
2020-12-06T06:55:23,524 WARN [qtp700249373-84] com.yahoo.maha.maha_druid_lookups.server.lookup.namespace.schema.flatbuffer.FlatBufferSchemaFactoryProvider - Implementation of FlatBufferSchemaFactory class name is black in the MahaNamespaceExtractionConfig, considering default implementation
2020-12-06T06:55:23,528 INFO [LookupExtractorFactoryContainerProvider-MainThread] com.yahoo.maha.maha_druid_lookups.query.lookup.MahaLookupExtractorFactory -  Received request [RocksDBExtractionNamespace{rocksDbInstanceHDFSPath='/Users/pranavbhole/git/maha-druid-lookups-example/target', lookupAuditingHDFSPath='/', namespace='customer_lookup', pollPeriod=PT30S, kafkaTopic='null', cacheEnabled=true, lookupAuditingEnabled=false, lookupName='customer_lookup', tsColumn='null', missingLookupConfig=null, lastUpdatedTime=-1, cacheActionRunner=CacheActionRunner{}, overrideLookupServiceHosts=[], randomLocalPathSuffixEnabled=false}]
2020-12-06T06:55:23,528 INFO [LookupExtractorFactoryContainerProvider-MainThread] com.yahoo.maha.maha_druid_lookups.server.lookup.namespace.cache.MahaNamespaceExtractionCacheManager - [customer_lookup] is new
2020-12-06T06:55:23,529 INFO [LookupExtractorFactoryContainerProvider-MainThread] com.yahoo.maha.maha_druid_lookups.server.lookup.namespace.cache.MahaNamespaceExtractionCacheManager - Trying to update namespace [customer_lookup]
2020-12-06T06:55:23,529 INFO [LookupExtractorFactoryContainerProvider-MainThread] com.yahoo.maha.maha_druid_lookups.server.lookup.namespace.cache.MahaNamespaceExtractionCacheManager - Passed through namespace: RocksDBExtractionNamespace{rocksDbInstanceHDFSPath='/Users/pranavbhole/git/maha-druid-lookups-example/target', lookupAuditingHDFSPath='/', namespace='customer_lookup', pollPeriod=PT30S, kafkaTopic='null', cacheEnabled=true, lookupAuditingEnabled=false, lookupName='customer_lookup', tsColumn='null', missingLookupConfig=null, lastUpdatedTime=-1, cacheActionRunner=CacheActionRunner{}, overrideLookupServiceHosts=[], randomLocalPathSuffixEnabled=false}
with concrete className: com.yahoo.maha.maha_druid_lookups.query.lookup.namespace.RocksDBExtractionNamespace
2020-12-06T06:55:23,548 INFO [MahaNamespaceExtractionCacheManager-0] com.yahoo.maha.maha_druid_lookups.server.lookup.namespace.RocksDBManager - successMarkerPath [/Users/pranavbhole/git/maha-druid-lookups-example/target/load_time=202012050000/_SUCCESS], lastUpdate [0]
2020-12-06T06:55:23,554 ERROR [MahaNamespaceExtractionCacheManager-0] com.yahoo.maha.maha_druid_lookups.server.lookup.namespace.RocksDBManager - hdfsPath [/Users/pranavbhole/git/maha-druid-lookups-example/target/load_time=202012050000/rocksdb.zip]
2020-12-06T06:55:23,554 ERROR [MahaNamespaceExtractionCacheManager-0] com.yahoo.maha.maha_druid_lookups.server.lookup.namespace.RocksDBManager - localZippedFileNameWithPath [/tmp/customer_lookup/202012050000rocksdb_.zip]
2020-12-06T06:55:23,555 INFO [MahaNamespaceExtractionCacheManager-0] com.yahoo.maha.maha_druid_lookups.server.lookup.namespace.RocksDBManager - starting new instance for namespace[customer_lookup]...
2020-12-06T06:55:23,555 INFO [MahaNamespaceExtractionCacheManager-0] com.yahoo.maha.maha_druid_lookups.server.lookup.namespace.RocksDBManager - Downloading RocksDB instance from [/Users/pranavbhole/git/maha-druid-lookups-example/target/load_time=202012050000/rocksdb.zip] to [/tmp/customer_lookup/202012050000rocksdb_.zip]
2020-12-06T06:55:23,586 INFO [MahaNamespaceExtractionCacheManager-0] com.yahoo.maha.maha_druid_lookups.server.lookup.namespace.RocksDBManager - Downloaded RocksDB instance from [/Users/pranavbhole/git/maha-druid-lookups-example/target/load_time=202012050000/rocksdb.zip] to [/tmp/customer_lookup/202012050000rocksdb_.zip]
2020-12-06T06:55:23,586 INFO [MahaNamespaceExtractionCacheManager-0] com.yahoo.maha.maha_druid_lookups.server.lookup.namespace.RocksDBManager - Unzipping RocksDB instance [/tmp/customer_lookup/202012050000rocksdb_.zip]
2020-12-06T06:55:23,613 INFO [MahaNamespaceExtractionCacheManager-0] com.yahoo.maha.maha_druid_lookups.server.lookup.namespace.RocksDBManager - Unzipped RocksDB instance [/tmp/customer_lookup/202012050000rocksdb_.zip]
2020-12-06T06:55:23,613 INFO [MahaNamespaceExtractionCacheManager-0] com.yahoo.maha.maha_druid_lookups.server.lookup.namespace.RocksDBManager - Cleaned up [/tmp/customer_lookup/202012050000rocksdb_.zip]
2020-12-06T06:55:23,628 INFO [MahaNamespaceExtractionCacheManager-0] com.yahoo.maha.maha_druid_lookups.server.lookup.namespace.RocksDBManager - 
** Compaction Stats [default] **
Level    Files   Size     Score Read(GB)  Rn(GB) Rnp1(GB) Write(GB) Wnew(GB) Moved(GB) W-Amp Rd(MB/s) Wr(MB/s) Comp(sec) CompMergeCPU(sec) Comp(cnt) Avg(sec) KeyIn KeyDrop
----------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  L0      1/0    2.28 KB   0.2      0.0     0.0      0.0       0.0      0.0       0.0   1.0      0.0      3.3      0.00              0.00         1    0.001       0      0
 Sum      1/0    2.28 KB   0.0      0.0     0.0      0.0       0.0      0.0       0.0   1.0      0.0      3.3      0.00              0.00         1    0.001       0      0
 Int      0/0    0.00 KB   0.0      0.0     0.0      0.0       0.0      0.0       0.0   0.0      0.0      0.0      0.00              0.00         0    0.000       0      0

** Compaction Stats [default] **
Priority    Files   Size     Score Read(GB)  Rn(GB) Rnp1(GB) Write(GB) Wnew(GB) Moved(GB) W-Amp Rd(MB/s) Wr(MB/s) Comp(sec) CompMergeCPU(sec) Comp(cnt) Avg(sec) KeyIn KeyDrop
-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
User      0/0    0.00 KB   0.0      0.0     0.0      0.0       0.0      0.0       0.0   0.0      0.0      3.3      0.00              0.00         1    0.001       0      0
Uptime(secs): 0.0 total, 0.0 interval
Flush(GB): cumulative 0.000, interval 0.000
AddFile(GB): cumulative 0.000, interval 0.000
AddFile(Total Files): cumulative 0, interval 0
AddFile(L0 Files): cumulative 0, interval 0
AddFile(Keys): cumulative 0, interval 0
Cumulative compaction: 0.00 GB write, 0.38 MB/s write, 0.00 GB read, 0.00 MB/s read, 0.0 seconds
Interval compaction: 0.00 GB write, 0.00 MB/s write, 0.00 GB read, 0.00 MB/s read, 0.0 seconds
Stalls(count): 0 level0_slowdown, 0 level0_slowdown_with_compaction, 0 level0_numfiles, 0 level0_numfiles_with_compaction, 0 stop for pending_compaction_bytes, 0 slowdown for pending_compaction_bytes, 0 memtable_compaction, 0 memtable_slowdown, interval 0 total count

** File Read Latency Histogram By Level [default] **

** DB Stats **
Uptime(secs): 0.0 total, 0.0 interval
Cumulative writes: 0 writes, 0 keys, 0 commit groups, 0.0 writes per commit group, ingest: 0.00 GB, 0.00 MB/s
Cumulative WAL: 0 writes, 0 syncs, 0.00 writes per sync, written: 0.00 GB, 0.00 MB/s
Cumulative stall: 00:00:0.000 H:M:S, 0.0 percent
Interval writes: 0 writes, 0 keys, 0 commit groups, 0.0 writes per commit group, ingest: 0.00 MB, 0.00 MB/s
Interval WAL: 0 writes, 0 syncs, 0.00 writes per sync, written: 0.00 MB, 0.00 MB/s
Interval stall: 00:00:0.000 H:M:S, 0.0 percent

2020-12-06T06:55:23,629 INFO [MahaNamespaceExtractionCacheManager-0] com.yahoo.maha.maha_druid_lookups.server.lookup.namespace.cache.MahaNamespaceExtractionCacheManager - Namespace [customer_lookup] successfully updated. preVersion [null], newVersion [202012050000]
```

* We are all set, let's query `status` field from `customer_lookup` namespace for `key=1`

```
curl "http://localhost:8083/druid/v1/namespaces/customer_lookup?namespaceclass=com.yahoo.maha.maha_druid_lookups.query.lookup.namespace.RocksDBExtractionNamespace&key=2&valueColumn=status&debug=true"
```
output: 
```$xslt
pranavbhole@C02ZM03XLVDV tools % curl "http://localhost:8083/druid/v1/namespaces/customer_lookup?namespaceclass=com.yahoo.maha.maha_druid_lookups.query.lookup.namespace.RocksDBExtractionNamespace&key=2&valueColumn=status&debug=true"
ACTIVE%         
```
*  you can also query lookups druid json with extraction function 
*  I have included all config files in `src/main/conf`
* if you want to build the proto then install protoc on your machine
and run from maha-druid-lookups-example dir.

```protoc src/main/resources/Customer.proto --java_out=src/main/java/. ```

this will compile Customer proto and will generate java class CustomerProtos in package `org.maha_druid_lookups_example`






 






