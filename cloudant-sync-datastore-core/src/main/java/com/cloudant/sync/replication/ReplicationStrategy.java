/**
 * Copyright (c) 2013 Cloudant, Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package com.cloudant.sync.replication;

import com.cloudant.sync.datastore.DatastoreException;
import com.cloudant.sync.event.EventBus;


interface ReplicationStrategy extends Runnable {

    void setCancel();

    boolean isReplicationTerminated();
    
    EventBus getEventBus();

    String getReplicationId() throws DatastoreException;

    int getDocumentCounter();

    int getBatchCounter();

    String getRemote();

}
