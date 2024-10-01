/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package io.vertx.cassandra.tests;

import com.datastax.oss.driver.api.core.cql.Statement;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class ThreadingCheckTest extends CassandraClientTestBase {

  @Test
  public void checkStreamHandlers(TestContext testContext) throws Exception {
    initializeRandomStringKeyspace();
    insertRandomStrings(1);
    String query = "select random_string from random_strings.random_string_by_first_letter where first_letter = 'A'";
    Async async = testContext.async(1);
    client.queryStream(query).onComplete(testContext.asyncAssertSuccess(stream -> {
      checkContext(testContext);
      stream.endHandler(end -> {
        checkContext(testContext);
        async.countDown();
      }).exceptionHandler(throwable -> checkContext(testContext)).handler(item -> checkContext(testContext));
    }));
  }

  @Test
  public void checkPrepareAndQueryHandlers(TestContext testContext) {
    initializeNamesKeyspace();
    String insert = "INSERT INTO names.names_by_first_letter (first_letter, name) VALUES (?, ?)";
    client.prepare(insert).onComplete(testContext.asyncAssertSuccess(prep -> {
      checkContext(testContext);
      Statement statement = prep.bind("P", "Pavel");
      client.execute(statement).onComplete(testContext.asyncAssertSuccess(exec -> {
        checkContext(testContext);
        String select = "select NAME as n from names.names_by_first_letter where first_letter = 'P'";
        client.executeWithFullFetch(select).onComplete(testContext.asyncAssertSuccess(rows -> {
          checkContext(testContext);
          testContext.assertTrue(rows.iterator().next().getString("n").equals("Pavel"));
        }));
      }));
    }));
  }
}
