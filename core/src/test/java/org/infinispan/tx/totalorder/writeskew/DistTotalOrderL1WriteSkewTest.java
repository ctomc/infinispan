package org.infinispan.tx.totalorder.writeskew;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.VersioningScheme;
import org.infinispan.container.versioning.DistL1WriteSkewTest;
import org.infinispan.test.fwk.CleanupAfterMethod;
import org.infinispan.transaction.TransactionProtocol;
import org.infinispan.util.concurrent.IsolationLevel;
import org.testng.annotations.Test;

import javax.transaction.Transaction;

import static org.testng.Assert.assertEquals;

/**
 * A simple write skew check test for total order based protocol in distributed mode and with L1 cache enabled
 *
 * @author Pedro Ruivo
 * @since 5.3
 */
@Test(groups = "functional", testName = "tx.totalorder.DistTotalOrderL1WriteSkewTest")
@CleanupAfterMethod
public class DistTotalOrderL1WriteSkewTest extends DistL1WriteSkewTest {
   public void transactionCleanupWithWriteSkew() throws Exception {
      cache(0).put("k", "v");
      tm(0).begin();
      assertEquals("v", cache(0).get("k"));
      cache(0).put("k", "v2");
      Transaction suspend = tm(0).suspend();

      cache(0).put("k", "v3");
      tm(0).resume(suspend);
      try {
         tm(0).commit();
         assert false;
      } catch (Throwable e) {
         //expected
         e.printStackTrace();
      }
      assertEquals("v3", cache(0).get("k"));
      assertEventuallyEquals(1, "k", "v3");
      assertNoTransactions();
   }

   @Override
   protected void decorate(ConfigurationBuilder builder) {
      super.decorate(builder);
      builder.transaction().transactionProtocol(TransactionProtocol.TOTAL_ORDER)
            .recovery().disable();
      builder.locking().isolationLevel(IsolationLevel.REPEATABLE_READ).writeSkewCheck(true)
            .versioning().enable().scheme(VersioningScheme.SIMPLE);
   }
}
