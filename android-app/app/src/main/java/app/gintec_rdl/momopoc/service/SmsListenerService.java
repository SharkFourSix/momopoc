package app.gintec_rdl.momopoc.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import app.gintec_rdl.momopoc.R;
import lib.gintec_rdl.momo.extraction.ExtractionService;
import lib.gintec_rdl.momo.extraction.TransactionExtractor;
import lib.gintec_rdl.momo.extractors.MpambaTransactionExtractor;
import lib.gintec_rdl.momo.model.MpambaCashInTransaction;
import lib.gintec_rdl.momo.model.MpambaCashOutTransaction;
import lib.gintec_rdl.momo.model.MpambaCreditTransaction;
import lib.gintec_rdl.momo.model.MpambaDebitTransaction;
import lib.gintec_rdl.momo.model.MpambaDepositTransaction;
import lib.gintec_rdl.momo.model.Transaction;

public final class SmsListenerService extends Service {
    private static final String NOTIFICATION_CHANNEL_ID = "momo";
    private static final int NOTIFICATION_ID = 100;
    private static final String TAG = "SmsListenerService";

    public static final String ACTION_NEW_TRANSACTION = "lib.gintec_rdl.momo.action.ACTION_NEW_TRANSACTION";
    public static final String EXTRA_TRANSACTION = "transaction";

    private LinkedBlockingQueue<SmsMessage> smsQueue;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;
    private ProcessorThread processorThread;
    private int transactionCount = 0;

    private static SmsListenerService serviceInstance;

    interface TransactionObserver {
        void onTransactionProcessed(Transaction transaction);
    }

    @Override
    public void onCreate() {
        serviceInstance = this;
        initializeNotificationComponents();
        TransactionObserver observer = new TransactionObserver() {
            @Override
            public void onTransactionProcessed(Transaction transaction) {
                transactionCount++;
                updateNotification();
                LocalBroadcastManager.getInstance(SmsListenerService.this)
                    .sendBroadcast(new Intent(ACTION_NEW_TRANSACTION)
                        .putExtra(EXTRA_TRANSACTION, transaction));
            }
        };
        (processorThread = new ProcessorThread(smsQueue = new LinkedBlockingQueue<>(), observer)).start();

        // Register extractors
        ExtractionService.getInstance().registerExtractor("MPAMBA", MpambaTransactionExtractor.class);

        IntentFilter filter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
        filter.setPriority(999);
        registerReceiver(smsBroadcastReceiver, filter);
    }

    private void initializeNotificationComponents(){
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationBuilder = new NotificationCompat.Builder(
            this,
            NOTIFICATION_CHANNEL_ID
        );
        final CharSequence title = getString(R.string.extraction_service_name);
        notificationBuilder.setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setAutoCancel(false)
            .setContentText(getString(R.string.processed_transactions, transactionCount))
            .setOngoing(true);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            final NotificationChannel serviceNotificationChannel = new NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                title,
                NotificationManager.IMPORTANCE_DEFAULT
            );
            serviceNotificationChannel.enableVibration(true);
            serviceNotificationChannel.setDescription(getString(R.string.extraction_service_description));
            notificationManager.createNotificationChannel(serviceNotificationChannel);
        }
        startForeground(NOTIFICATION_ID, notificationBuilder.build());
    }

    private void updateNotification(){
        notificationBuilder.setContentText(getString(R.string.processed_transactions, transactionCount));
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    public static boolean isRunning(){
        try {
            return serviceInstance != null && serviceInstance.dummy();
        }catch (NullPointerException e){
            return false;
        }
    }

    public static void start(Context context){
        context.startService(new Intent(context, SmsListenerService.class));
    }

    private boolean dummy(){
        return true;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        serviceInstance = null;
        unregisterReceiver(smsBroadcastReceiver);
        processorThread.stopThread();
        notificationManager.cancelAll();
    }

    private final BroadcastReceiver smsBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final Bundle extras = intent.getExtras();
            if(extras != null){
                final Object[] smsExtras = (Object[])extras.get("pdus");
                if(smsExtras != null){
                    for (Object smsExtra : smsExtras) {
                        smsQueue.offer(SmsMessage.createFromPdu((byte[]) smsExtra));
                    }
                }
            }
        }
    };

    public static void host(String host) {
        if (isRunning()){
            serviceInstance.processorThread.setHost(host);
        }
    }

    public static void port(int port){
        if(isRunning()){
            serviceInstance.processorThread.setPort(port);
        }
    }

    public static String host(){
        return isRunning() ? serviceInstance.processorThread.getHost() : null;
    }

    public static int port(){
        return isRunning() ? serviceInstance.processorThread.getPort() : 0;
    }

    public static final class ProcessorThread extends Thread {
        private boolean mStop;
        private final BlockingQueue<SmsMessage> queue;
        private int port;
        private String host;
        private final Gson gson;
        private final TransactionObserver observer;

        ProcessorThread(BlockingQueue<SmsMessage> queue, TransactionObserver observer) {
            this.queue = queue;
            this.observer = observer;
            this.gson = new GsonBuilder().setDateFormat("dd/MM/yyyy HH:mm:ss").create();
        }

        int getPort() {
            return port;
        }

        void setPort(int port) {
            this.port = port;
        }

        String getHost() {
            return host;
        }

        void setHost(String host) {
            this.host = host;
        }

        void stopThread(){
            queue.clear();
            mStop = true;
            interrupt();
        }

        @Override
        public void run(){
            mStop = false;
            Log.v(TAG, "Processor thread started");
            while(!mStop){
                try {
                    final SmsMessage message = queue.take();
                    final String source = message.getOriginatingAddress();
                    final String body = message.getMessageBody();
                    if(!TextUtils.isEmpty(source) && !TextUtils.isEmpty(body)){
                        final TransactionExtractor extractor = ExtractionService.getInstance().getExtractor(source);
                        if(extractor instanceof MpambaTransactionExtractor){
                            try{
                                final Transaction tx = extractor.extract(source, body, null);
                                final String transactionType;
                                if(tx instanceof MpambaDepositTransaction){
                                    transactionType = "deposit";
                                }else if(tx instanceof MpambaCreditTransaction){
                                    transactionType = "credit";
                                }else if(tx instanceof MpambaCashInTransaction){
                                    transactionType = "cash-in";
                                }else if(tx instanceof MpambaDebitTransaction){
                                    transactionType = "debit";
                                }else if(tx instanceof MpambaCashOutTransaction){
                                    transactionType = "cash-out";
                                }else{
                                    throw new Exception("Unknown transaction type");
                                }
                                if(sendTransactionToServer(tx, transactionType)){
                                    Log.e(TAG, "Failed to send transaction to server " + tx);
                                }
                                observer.onTransactionProcessed(tx);
                            }catch (Exception e){
                                Log.e(TAG, e.getMessage(), e);
                            }
                        }
                    }
                }catch(InterruptedException e){
                    Log.w(TAG, e.getMessage(), e);
                }
            }
            Log.v(TAG, "Processor thread stopped");
        }

        boolean sendTransactionToServer(Transaction transaction, String type) {
            // Use some other library like ok-http or Volley
            HttpURLConnection huc = null;
            OutputStream outputStream = null;
            int responseCode = -1;
            try {
                URL url = new URL(String.format(Locale.US, "http://%s:%d/mpamba-transaction/%s", host, port, type));
                huc = (HttpURLConnection)url.openConnection();
                huc.setUseCaches(false);
                huc.setRequestMethod("POST");
                huc.setRequestProperty("Content-Type", "application/json");
                huc.setDoInput(true);
                huc.setDoOutput(true);
                outputStream = huc.getOutputStream();
                outputStream.write(gson.toJson(transaction).getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
                responseCode = huc.getResponseCode();
            }catch (Exception e){
                Log.e(TAG, e.getMessage(), e);
            }finally {
                if(outputStream != null){
                    try {
                        outputStream.close();
                    }catch (IOException ignored){
                    }
                }
                if(huc != null){
                    huc.disconnect();
                }
            }
            return responseCode == HttpURLConnection.HTTP_OK;
        }
    }
}
