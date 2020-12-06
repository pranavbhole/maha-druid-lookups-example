// Licensed under the terms of the Apache License 2.0. Please see LICENSE file in project root for terms.
package org.maha_druid_lookups_example;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.GeneratedMessageV3;
import com.yahoo.maha.maha_druid_lookups.server.lookup.namespace.schema.protobuf.DefaultProtobufSchemaFactory;

public class ExampleProtobufSchemaFactory extends DefaultProtobufSchemaFactory {
    public ExampleProtobufSchemaFactory() {
        super(ImmutableMap.<String, GeneratedMessageV3>of("customer_lookup", CustomerProtos.Customer.getDefaultInstance()));
    }
}

// org.maha_druid_lookups_example.ExampleProtobufSchemaFactory
