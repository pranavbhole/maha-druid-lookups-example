package org.maha_druid_lookups_example;

import com.google.protobuf.Message;
import org.rocksdb.*;
import org.zeroturnaround.zip.ZipUtil;
import org.zeroturnaround.zip.commons.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Hello world!
 *
 */
public class CreateExampleRocksDBInstance
{
    public static void main( String[] args ) throws IOException {
        try {
            String rocksDbPath = "target/example-rocksdb";

            File f = new File(rocksDbPath);
            if (f.exists()) {
                FileUtils.forceDelete(f);
            }

            Options options = new Options();
            options.setCreateIfMissing(true);
            options.optimizeForPointLookup(2 * 1024 * 1024 * 1024);
            options.setCompressionType(CompressionType.ZSTD_COMPRESSION);
            options.setBottommostCompressionType(CompressionType.ZSTD_COMPRESSION);
            options.setMaxOpenFiles(-1);
            options.setMemTableConfig(new HashSkipListMemTableConfig());
            options.setAllowConcurrentMemtableWrite(false);
            RocksDB rocksDB =  RocksDB.open(options, rocksDbPath);
            String status[] = {"ACTIVE", "DELETED"};
            for (int i=1; i<=100; i++) {
                String id = String.valueOf(i);
                Message message = CustomerProtos.Customer.newBuilder()
                        .setId(id)
                        .setAddress("address_"+id)
                        .setName("name_"+id)
                        .setLastUpdated(""+System.currentTimeMillis())
                        .setStatus(status[i%2])
                        .build();
                rocksDB.put(id.getBytes(),message.toByteArray());
            }
            CustomerProtos.Customer  customer = CustomerProtos.Customer.parseFrom(rocksDB.get("1".getBytes()));
            if ("address_1".equals(customer.getAddress())) {
               System.out.println("Rocksdb Instance creation Success!");
            } else {
                throw new IllegalStateException("Something went wrong in creating instance");
            }
            rocksDB.close();
            zipRocksDb(rocksDbPath);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void zipRocksDb(String path) throws IOException {
        //202012050000
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd0000")).toString();
        File file = new File(String.format("target/load_time=%s/rocksdb.zip", dateStr));
        FileUtils.forceMkdir(new File(file.getParent()));
        ZipUtil.pack(new File(path), file);
        File success = new File(String.format("target/load_time=%s/", dateStr), "_SUCCESS");
        success.createNewFile();
        FileOutputStream out = new FileOutputStream(success);
        out.write("test".getBytes());
        out.close();
        //org.apache.commons.io.FileUtils.writeStringToFile(success, "_SUCCESS", Charset.defaultCharset());
        System.out.println("Created Zip file of size "+file.length());
    }
}
