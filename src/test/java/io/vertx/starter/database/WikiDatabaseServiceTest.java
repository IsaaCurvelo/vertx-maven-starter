package io.vertx.starter.database;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class WikiDatabaseServiceTest {

  private Vertx vertx;
  private WikiDatabaseService dbService;

  @Before
  public void setUp(TestContext context) {
    vertx = Vertx.vertx();

    JsonObject configs = new JsonObject()
      .put(WikiDatabaseVerticle.CONFIG_WIKIDB_JDBC_URL, "jdbc:hsqldb:mem:testdb;shutdown=true")
      .put(WikiDatabaseVerticle.CONFIG_WIKIDB_JDBC_MAX_POOL_SIZE, 4);

    vertx.deployVerticle(new WikiDatabaseVerticle(), new DeploymentOptions().setConfig(configs),
      context.asyncAssertSuccess(deploymentId -> {
        dbService = WikiDatabaseService.createProxy(vertx, WikiDatabaseVerticle.CONFIG_WIKIDB_QUEUE);
      }));
  }

  @Test
  public void crud_operations(TestContext context) {
    Async async = context.async();
    dbService.createPage("Test", "Some content", context.asyncAssertSuccess(void1 -> {

      dbService.fetchPage("Test", context.asyncAssertSuccess(fetchedJsonTestPage -> {
        context.assertTrue(fetchedJsonTestPage.getBoolean("found"));
        context.assertTrue(fetchedJsonTestPage.containsKey("id"));
        context.assertEquals("Some content", fetchedJsonTestPage.getString("rawContent"));

        dbService.savePage(fetchedJsonTestPage.getInteger("id"), "Something else",
          context.asyncAssertSuccess(void2 -> {

            dbService.fetchAllPages(context.asyncAssertSuccess(allPagesJsonArray -> {

              context.assertEquals(1, allPagesJsonArray.size());

              dbService.fetchPage("Test", context.asyncAssertSuccess(fetchedJsonTestPageAfterUpdate -> {

                context.assertEquals("Something else", fetchedJsonTestPageAfterUpdate.getString("rawContent"));

                dbService.deletePage(fetchedJsonTestPage.getInteger("id"), context.asyncAssertSuccess(void3 -> {

                  dbService.fetchAllPages(context.asyncAssertSuccess(allPagesAfterDelete -> {
                    context.assertTrue(allPagesAfterDelete.isEmpty());
                    async.complete();
                  }));
                }));
              }));
            }));
          }));
      }));
    }));
  }


  @After
  public void tearDown(TestContext context) {
    vertx.close();
  }
}
