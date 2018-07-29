/*
 * Copyright (C) 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.codelabs.dataflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.services.bigquery.model.TableRow;
import com.google.codelabs.dataflow.utils.CustomPipelineOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.bigquery.TableRowJsonCoder;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.FlatMapElements;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.TypeDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Dataflow command-line options must be specified:
//   --project=<your project ID>
//   --sinkProject=<your project ID>
//   --stagingLocation=gs://<your staging bucket>
//   --runner=DataflowPipelineRunner
//   --streaming=true
//   --numWorkers=3
//   --zone=<your compute zone>
// You can launch the pipeline from the command line using:
// mvn exec:java -Dexec.mainClass="com.google.codelabs.dataflow.AllRides" -e -Dexec.args="<your arguments>"

@SuppressWarnings("serial")
public class AllRides {
  private static final Logger LOG = LoggerFactory.getLogger(AllRides.class);

  // ride format from PubSub
  // {
  // "ride_id":"a60ba4d8-1501-4b5b-93ee-b7864304d0e0",
  // "latitude":40.66684000000033,
  // "longitude":-73.83933000000202,
  // "timestamp":"2016-08-31T11:04:02.025396463-04:00",
  // "meter_reading":14.270274,
  // "meter_increment":0.019336415,
  // "ride_status":"enroute" / "pickup" / "dropoff"
  // "passenger_count":2
  // }

  public void run(CustomPipelineOptions options) {
    Pipeline p = Pipeline.create(options);
    p.apply("read from pubsub",
            PubsubIO.readStrings().
                    fromTopic(String.format("projects/%s/topics/%s", options.getSourceProject(), options.getSourceTopic())).
                    withTimestampAttribute("ts"))
     .apply("write to pubsub",
             PubsubIO.writeStrings().
                     to(String.format("projects/%s/topics/%s", options.getSinkProject(), options.getSinkTopic())));
    p.run();
  }

  public static void main(String[] args) {
    CustomPipelineOptions options =
        PipelineOptionsFactory.fromArgs(args).withValidation().as(CustomPipelineOptions.class);
    AllRides rides = new AllRides();
    rides.run(options);
  }
}
