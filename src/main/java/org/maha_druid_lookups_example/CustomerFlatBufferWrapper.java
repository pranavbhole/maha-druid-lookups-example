package org.maha_druid_lookups_example;

import com.google.common.collect.ImmutableMap;
import com.google.flatbuffers.FlatBufferBuilder;
import com.google.flatbuffers.Table;
import com.yahoo.maha.maha_druid_lookups.server.lookup.namespace.schema.flatbuffer.FlatBufferValue;
import com.yahoo.maha.maha_druid_lookups.server.lookup.namespace.schema.flatbuffer.FlatBufferWrapper;

import java.nio.ByteBuffer;
import java.util.Map;

public class CustomerFlatBufferWrapper extends FlatBufferWrapper {


    Map<String, Integer> fieldNameToOffsetMap = ImmutableMap.<String, Integer> builder()
            .put("id", 0)
            .put("name", 1)
            .put("status", 2)
            .build();

    @Override
    public Map<String, Integer> getFieldNameToFieldOffsetMap() {
        return fieldNameToOffsetMap;
    }

    @Override
    public String readFieldValue(String fieldName, Table customerFb) {
        Customer customer = (Customer) customerFb;
        if (!fieldNameToOffsetMap.containsKey(fieldName)) {
            return null;
        }
        int offset = fieldNameToOffsetMap.get(fieldName);
        switch(offset) {
            case 0:
                return new Long(customer.id()).toString();
            case 1:
                return customer.name();
            case 2:
                return customer.status();
            default:
                return null;
        }
    }

    @Override
    public Table getFlatBuffer(byte[] flatBufferBytes) {
        return Customer.getRootAsCustomer(ByteBuffer.wrap(flatBufferBytes));
    }

    @Override
    public FlatBufferBuilder createFlatBuffer(Map<String, FlatBufferValue> nameToValueMap) {
        FlatBufferBuilder bufferBuilder = new FlatBufferBuilder(512);

        //Create Index in the buffer builder
        for (Map.Entry<String,FlatBufferValue> entry : nameToValueMap.entrySet()) {
            if (fieldNameToOffsetMap.containsKey(entry.getKey())) {
                int index = bufferBuilder.createString(entry.getValue().getValue());
                entry.getValue().setOffsetInBuffer(index);
            }
        }

        // add indices to values in buffer builder
        Customer.startCustomer(bufferBuilder);
        for (Map.Entry<String, FlatBufferValue> entry : nameToValueMap.entrySet()) {
            int fbFieldOffset = fieldNameToOffsetMap.get(entry.getKey());
            bufferBuilder.addOffset(fbFieldOffset, entry.getValue().getOffsetInBuffer(), 0);
        }

        int endRoot = Customer.endCustomer(bufferBuilder);
        Customer.finishCustomerBuffer(bufferBuilder, endRoot);
        return bufferBuilder;
    }
}