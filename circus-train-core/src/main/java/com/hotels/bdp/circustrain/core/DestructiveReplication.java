package com.hotels.bdp.circustrain.core;

import org.apache.thrift.TException;

import com.hotels.bdp.circustrain.api.CircusTrainException;
import com.hotels.bdp.circustrain.api.Replication;
import com.hotels.bdp.circustrain.api.conf.TableReplication;
import com.hotels.bdp.circustrain.core.replica.DestructiveReplica;
import com.hotels.bdp.circustrain.core.source.DestructiveSource;

public class DestructiveReplication implements Replication {

  private final ReplicationFactoryImpl upsertReplicationFactory;
  private final TableReplication tableReplication;
  private final DestructiveSource destructiveSource;
  private final DestructiveReplica destructiveReplica;
  private final String eventId;

  public DestructiveReplication(
      ReplicationFactoryImpl upsertReplicationFactory,
      TableReplication tableReplication,
      String eventId,
      DestructiveSource destructiveSource,
      DestructiveReplica destructiveReplica) {
    this.upsertReplicationFactory = upsertReplicationFactory;
    this.tableReplication = tableReplication;
    this.eventId = eventId;
    this.destructiveSource = destructiveSource;
    this.destructiveReplica = destructiveReplica;
  }

  @Override
  public void replicate() throws CircusTrainException {
    try {
      if (!destructiveReplica.tableIsUnderCircusTrainControl()) {
        throw new CircusTrainException("Replica table '"
            + destructiveReplica.getQualifiedTableName()
            + "' is not controlled by circus train aborting replication, check configuration for correct replica name");
      }
      if (destructiveSource.tableExists()) {
        Replication replication = upsertReplicationFactory.newInstance(tableReplication);
        destructiveReplica.dropDeletedPartitions(destructiveSource.getPartitionNames());
        // do normal replication
        replication.replicate();
      } else {
        destructiveReplica.dropTable();
      }
    } catch (TException e) {
      throw new CircusTrainException(e);
    }
  }

  @Override
  public String name() {
    return "destructive-" + tableReplication.getQualifiedReplicaName();
  }

  @Override
  public String getEventId() {
    return eventId;
  }

}