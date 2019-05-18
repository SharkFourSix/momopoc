package app.gintec_rdl.momopoc.service;


import org.junit.Test;

import java.util.Date;

import lib.gintec_rdl.momo.model.MobileMoneyAgent;
import lib.gintec_rdl.momo.model.MpambaCashInTransaction;

import static org.junit.Assert.assertTrue;

public class ProcessorThreadTest {

    @Test
    public void testPostMethod(){
        SmsListenerService.ProcessorThread processorThread
            = new SmsListenerService.ProcessorThread(null, null);

        MpambaCashInTransaction cashInTransaction = new MpambaCashInTransaction();
        cashInTransaction.setTransactionId("67AEDB9O6IH74ZH89");
        cashInTransaction.setDate(new Date());
        cashInTransaction.setAgent(new MobileMoneyAgent("12345", "John Doe-From-Android"));
        cashInTransaction.setAmount(45000);
        cashInTransaction.setFee(0);
        cashInTransaction.setBalance(1.00);

        processorThread.setHost("localhost");
        processorThread.setPort(80);
        assertTrue(processorThread.sendTransactionToServer(cashInTransaction, "cash-in"));
    }
}