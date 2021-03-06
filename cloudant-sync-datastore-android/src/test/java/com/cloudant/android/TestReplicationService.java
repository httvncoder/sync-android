package com.cloudant.android;

import android.content.Context;
import android.util.Log;

import com.cloudant.sync.datastore.Datastore;
import com.cloudant.sync.datastore.DatastoreManager;
import com.cloudant.sync.datastore.DatastoreNotCreatedException;
import com.cloudant.sync.replication.WifiPeriodicReplicationReceiver;
import com.cloudant.sync.replication.PeriodicReplicationService;
import com.cloudant.sync.replication.Replicator;
import com.cloudant.sync.replication.ReplicatorBuilder;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

public class TestReplicationService extends PeriodicReplicationService {

    private static final String TASKS_DATASTORE_NAME = "tasks";
    private static final String DATASTORE_MANGER_DIR = "data";
    private static final String TAG = "TestReplicationService";

    class TestReceiver extends WifiPeriodicReplicationReceiver<TestReplicationService> {
        public TestReceiver() {
            super(TestReplicationService.class);
        }
    }

    public TestReplicationService() {
        super(TestReceiver.class);
    }

    /* Only used for test purposes. */
    public TestReplicationService(Context baseContext) {
        this();
        attachBaseContext(baseContext);
    }

    protected int getBoundIntervalInSeconds() {
        return 60;
    }

    protected int getUnboundIntervalInSeconds() {
        return 120;
    }
    
    protected boolean startReplicationOnBind() {
        return true;
    }
}
