/*
 * Copyright 2021 TiDB Project Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.tidb.bigdata.flink.connector.source.enumerator;

import io.tidb.bigdata.flink.connector.source.split.TiDBSourceSplit;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.apache.flink.core.io.SimpleVersionedSerializer;

public class TiDBSourceSplitEnumStateSerializer
    implements SimpleVersionedSerializer<TiDBSourceSplitEnumState> {

  public static final int CURRENT_VERSION = 0;

  @Override
  public int getVersion() {
    return CURRENT_VERSION;
  }

  @Override
  public byte[] serialize(TiDBSourceSplitEnumState state) throws IOException {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos)) {
      for (TiDBSourceSplit split : state.assignedSplits()) {
        split.serialize(dos);
      }
      dos.flush();
      return baos.toByteArray();
    }
  }

  @Override
  public TiDBSourceSplitEnumState deserialize(int version, byte[] bytes) throws IOException {
    if (version != CURRENT_VERSION) {
      throw new IOException(
          String.format(
              "The bytes are serialized with version %d, "
                  + "while this deserializer only supports version up to %d",
              version, CURRENT_VERSION));
    }
    try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bytes))) {
      Set<TiDBSourceSplit> splits = new HashSet<>();
      while (dis.available() > 0) {
        splits.add(TiDBSourceSplit.deserialize(dis));
      }
      return new TiDBSourceSplitEnumState(splits);
    }
  }
}
