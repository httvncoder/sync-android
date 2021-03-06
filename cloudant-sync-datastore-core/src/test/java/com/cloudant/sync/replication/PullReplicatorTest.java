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

import com.cloudant.common.RequireRunningCouchDB;
import com.cloudant.common.TestOptions;
import com.cloudant.http.interceptors.CookieInterceptor;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Category(RequireRunningCouchDB.class)
public class PullReplicatorTest extends ReplicationTestBase {

    URI source;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        source = getCouchConfig(getDbName()).getRootUri();

        prepareTwoDocumentsInRemoteDB();
    }

    private void prepareTwoDocumentsInRemoteDB() {
        Bar bar1 = BarUtils.createBar(remoteDb, "Tom", 31);
        couchClient.create(bar1);
        Bar bar2 = BarUtils.createBar(remoteDb, "Jerry", 52);
        couchClient.create(bar2);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void start_StartedThenComplete() throws InterruptedException {
        Replicator replicator = super.getPullBuilder().build();

        TestReplicationListener listener = new TestReplicationListener();
        Assert.assertEquals(Replicator.State.PENDING, replicator.getState());
        replicator.getEventBus().register(listener);
        replicator.start();
        Assert.assertEquals(Replicator.State.STARTED, replicator.getState());

        while(replicator.getState() != Replicator.State.COMPLETE && replicator.getState() != Replicator.State.ERROR) {
            Thread.sleep(1000);
        }

        Assert.assertEquals(Replicator.State.COMPLETE, replicator.getState());
        Assert.assertEquals(2, datastore.getDocumentCount());

        Assert.assertTrue(listener.finishCalled);
        Assert.assertFalse(listener.errorCalled);
    }

    @Test
    public void testRequestInterceptors() throws Exception {

        InterceptorCallCounter interceptorCallCounter = new InterceptorCallCounter();
        Replicator replicator = super.getPullBuilder()
                .addRequestInterceptors(interceptorCallCounter).build();

        runReplicationUntilComplete(replicator);
        Assert.assertTrue(interceptorCallCounter.interceptorRequestTimesCalled >= 1);

    }

    @Test
    public void testResponseInterceptors() throws Exception {

        InterceptorCallCounter interceptorCallCounter = new InterceptorCallCounter();
        Replicator replicator = super.getPullBuilder()
                .addResponseInterceptors(interceptorCallCounter).build();

        runReplicationUntilComplete(replicator);
        Assert.assertTrue(interceptorCallCounter.interceptorResponseTimesCalled >= 1);
    }

    @Test
    public void testPullReplicationCreatedSuccessfullyWithoutFilter() throws Exception {

        Replicator replicator = ReplicatorBuilder.pull()
                .from(this.source)
                .to(this.datastore)
                .build();

        Assert.assertNotNull(replicator);
    }

    @Test
    public void testPullReplicationCreatedSuccessfullyWithFilter() throws Exception {

        Replicator replicator = ReplicatorBuilder.pull()
                .from(this.source)
                .to(this.datastore)
                .filter(new PullFilter("a_filter"))
                .build();

        Assert.assertNotNull(replicator);
    }

    @Test
    public void testPullReplicationCreatedSuccessfullyWithFilterAndParams() throws Exception {

        Map<String,String> params = new HashMap<String,String>();
        params.put("a","parameter");
        Replicator replicator = ReplicatorBuilder.pull()
                .from(this.source)
                .to(this.datastore)
                .filter(new PullFilter("a_filter",params))
                .build();

        Assert.assertNotNull(replicator);
    }

    @Test
    public void testPullReplicationCreatedSuccessfullyWithFilterAndEmptyParams() throws Exception {

        Map<String,String> params = new HashMap<String,String>();
        Replicator replicator = ReplicatorBuilder.pull()
                .from(this.source)
                .to(this.datastore)
                .filter(new PullFilter("a_filter",params))
                .build();

        Assert.assertNotNull(replicator);
    }

    @Test
    public void testRequestInterceptorsThroughBuilder() throws Exception {

        InterceptorCallCounter interceptorCallCounter = new InterceptorCallCounter();

        TestReplicationListener listener = new TestReplicationListener();
        ReplicatorBuilder replicatorBuilder = ReplicatorBuilder.pull()
                .to(this.datastore)
                .from(this.remoteDb.couchClient.getRootUri())
                .addRequestInterceptors(interceptorCallCounter)
                .addResponseInterceptors(interceptorCallCounter);
        if (TestOptions.COOKIE_AUTH) {
            CookieInterceptor ci = new CookieInterceptor(TestOptions.COUCH_USERNAME, TestOptions.COUCH_PASSWORD);
            replicatorBuilder.addRequestInterceptors(ci);
            replicatorBuilder.addResponseInterceptors(ci);
        }
        Replicator replicator = replicatorBuilder.build();

        replicator.getEventBus().register(listener);
        replicator.start();

        while(replicator.getState() != Replicator.State.COMPLETE && replicator.getState() != Replicator.State.ERROR) {
            Thread.sleep(50);
        }

        Assert.assertEquals(Replicator.State.COMPLETE, replicator.getState());
        Assert.assertFalse(listener.errorCalled);
        Assert.assertTrue(listener.finishCalled);

        //check that the response and request interceptors have been called.
        Assert.assertTrue(interceptorCallCounter.interceptorResponseTimesCalled >= 1);
        Assert.assertTrue(interceptorCallCounter.interceptorRequestTimesCalled >= 1);

    }

    @Test
    public void replicatorCanBeReused() throws Exception {
        ReplicatorBuilder replicatorBuilder = super.getPullBuilder();
        Replicator replicator = replicatorBuilder.build();
        ReplicationStrategy replicationStrategy = ((ReplicatorImpl)replicator).strategy;
        replicator.start();
        // replicate 2 docs created at test setup
        while(replicator.getState() != Replicator.State.COMPLETE && replicator.getState() != Replicator.State.ERROR) {
            Thread.sleep(50);
        }
        // check document counter has been incremented
        Assert.assertEquals(2, replicationStrategy.getDocumentCounter());
        Bar bar3 = BarUtils.createBar(remoteDb, "Test", 52);
        couchClient.create(bar3);
        replicator.start();
        ReplicationStrategy replicationStrategy2 = ((ReplicatorImpl)replicator).strategy;
        // replicate 3rd doc
        while(replicator.getState() != Replicator.State.COMPLETE && replicator.getState() != Replicator.State.ERROR) {
            Thread.sleep(50);
        }
        // check document counter has been reset since last replication and incremented
        Assert.assertEquals(1, replicationStrategy2.getDocumentCounter());
        Assert.assertEquals(3, this.datastore.getDocumentCount());
    }

    @Test
    public void replicatorBuilderAddsCookieInterceptorCustomPort() throws Exception {
        ReplicatorBuilder.Pull p = ReplicatorBuilder.pull().
                from(new URI("http://🍶:🍶@some-host:123/path%2Fsome-path-日本")).
                to(datastore);
        ReplicatorImpl r = (ReplicatorImpl)p.build();
        // check that user/pass has been removed
        Assert.assertEquals("http://some-host:123/path%2Fsome-path-日本",
                (((CouchClientWrapper)(((PullStrategy)r.strategy).sourceDb)).
                        getCouchClient().
                        getRootUri()).
                        toString()
                );
        assertCookieInterceptorPresent(p, "name=%F0%9F%8D%B6&password=%F0%9F%8D%B6");
    }

    @Test
    public void replicatorBuilderAddsCookieInterceptorDefaultPort() throws Exception {
        ReplicatorBuilder.Pull p = ReplicatorBuilder.pull().
                from(new URI("http://🍶:🍶@some-host/path%2Fsome-path-日本")).
                to(datastore);
        ReplicatorImpl r = (ReplicatorImpl)p.build();
        // check that user/pass has been removed
        Assert.assertEquals("http://some-host:80/path%2Fsome-path-日本",
                (((CouchClientWrapper) (((PullStrategy) r.strategy).sourceDb)).
                        getCouchClient().
                        getRootUri()).
                        toString()
        );
       assertCookieInterceptorPresent(p, "name=%F0%9F%8D%B6&password=%F0%9F%8D%B6");
    }

}
