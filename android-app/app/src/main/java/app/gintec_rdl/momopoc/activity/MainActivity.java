package app.gintec_rdl.momopoc.activity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.Locale;
import java.util.Objects;

import app.gintec_rdl.momopoc.R;
import app.gintec_rdl.momopoc.adapter.TransactionViewModelAdapter;
import app.gintec_rdl.momopoc.presentation.ModelViewProvider;
import app.gintec_rdl.momopoc.presentation.ModelViewProviderRegistry;
import app.gintec_rdl.momopoc.presentation.TransactionModelView;
import app.gintec_rdl.momopoc.service.SmsListenerService;
import lib.gintec_rdl.momo.model.MpambaCashInTransaction;
import lib.gintec_rdl.momo.model.MpambaCashOutTransaction;
import lib.gintec_rdl.momo.model.MpambaCreditTransaction;
import lib.gintec_rdl.momo.model.MpambaDebitTransaction;
import lib.gintec_rdl.momo.model.MpambaDepositTransaction;
import lib.gintec_rdl.momo.model.Transaction;

public class MainActivity extends AppCompatActivity {
    private static final int RECEIVE_SMS_PERMISSION_REQUEST_CODE = 100;

    private AppCompatEditText txtHost, txtPort;
    private RecyclerView recyclerView;

    private TransactionViewModelAdapter adapter;

    private final BroadcastReceiver transactionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (SmsListenerService.ACTION_NEW_TRANSACTION.equalsIgnoreCase(intent.getAction())) {
                adapter.addTransaction((Transaction) intent.getSerializableExtra(SmsListenerService.EXTRA_TRANSACTION));
                recyclerView.scrollToPosition(adapter.getItemCount() - 1);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recyclerView = findViewById(R.id.recyclerView);
        txtHost = findViewById(R.id.txt_host);
        txtPort = findViewById(R.id.txt_port);
        findViewById(R.id.btn_save).setOnClickListener(new View.OnClickListener() {
            @SuppressWarnings("ConstantConditions")
            @Override
            public void onClick(View view) {
                if (SmsListenerService.isRunning()) {
                    String host = txtHost.getText().toString();
                    int port = Integer.valueOf(txtPort.getText().toString());
                    boolean updated = false;
                    if (!TextUtils.isEmpty(host)) {
                        SmsListenerService.host(host);
                        updated = true;
                    }
                    if (port > 0) {
                        SmsListenerService.port(port);
                        updated = true;
                    }
                    if (updated) {
                        Toast.makeText(MainActivity.this, "Updated", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        
        ModelViewProviderRegistry registry = ModelViewProviderRegistry.getInstance();
        registry.registerModelViewProvider(MpambaTransactionModelViewProvider.class);
        recyclerView.setAdapter(adapter = new TransactionViewModelAdapter());

        /*
        MpambaCashInTransaction cashInTransaction = new MpambaCashInTransaction();
        cashInTransaction.setTransactionId("67AEDB9O6IH74ZH89");
        cashInTransaction.setDate(new Date());
        cashInTransaction.setAgent(new MobileMoneyAgent("12345", "John Doe-From-Android"));
        cashInTransaction.setAmount(45000);
        cashInTransaction.setFee(0);
        cashInTransaction.setBalance(1.00);

        MpambaDepositTransaction depositTransaction = new MpambaDepositTransaction();
        depositTransaction.setTransactionId("4523452345");
        depositTransaction.setDate(new Date());
        depositTransaction.setAmount(23423);
        depositTransaction.setFee(234);
        depositTransaction.setBalance(0);
        depositTransaction.setSource("National Bank");

        MpambaCreditTransaction creditTransaction = new MpambaCreditTransaction();
        creditTransaction.setTransactionId("5KJ4HJHU234234");
        creditTransaction.setDate(new Date());
        creditTransaction.setAmount(23424);
        creditTransaction.setBalance(234324);
        creditTransaction.setFee(24234);
        creditTransaction.setRecipientName("Ms. Cutey Sidechick");
        creditTransaction.setRecipientPhone("088 666 6666");

        MpambaDebitTransaction debitTransaction = new MpambaDebitTransaction();
        debitTransaction.setTransactionId("98WQEHBDJHHJEWER87");
        debitTransaction.setDate(new Date());
        debitTransaction.setSenderName("Janet Doe");
        debitTransaction.setSenderPhone("088 111 1111");
        debitTransaction.setAmount(1.0);
        debitTransaction.setBalance(2.5);

        MpambaCashOutTransaction cashOutTransaction = new MpambaCashOutTransaction();
        cashOutTransaction.setTransactionId("454323452345");
        cashOutTransaction.setDate(new Date());
        cashOutTransaction.setAgent(new MobileMoneyAgent("1234567", "AGENT SMITH"));
        cashOutTransaction.setAmount(10000);
        cashOutTransaction.setFee(380);
        cashOutTransaction.setBalance(0);

        adapter.addTransaction(cashInTransaction);
        adapter.addTransaction(depositTransaction);
        adapter.addTransaction(creditTransaction);
        adapter.addTransaction(debitTransaction);
        adapter.addTransaction(cashOutTransaction);*/
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(
            transactionReceiver,
            new IntentFilter(SmsListenerService.ACTION_NEW_TRANSACTION)
        );
        // Can't get away from this one :D
        foreverAskForPermission();
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(transactionReceiver);
    }

    private void foreverAskForPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED) {
            if (!SmsListenerService.isRunning()) {
                SmsListenerService.start(this);
            } else {
                txtHost.setText(SmsListenerService.host());
                txtPort.setText(String.format(Locale.US, "%d", SmsListenerService.port()));
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.RECEIVE_SMS},
                RECEIVE_SMS_PERMISSION_REQUEST_CODE
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case RECEIVE_SMS_PERMISSION_REQUEST_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    SmsListenerService.start(this);
                }
                break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private static final class MpambaCashInTransactionModelView extends TransactionModelView {
        private AppCompatTextView txtTransactionId;
        private AppCompatTextView txtDate;
        private AppCompatTextView txtAgent;
        private AppCompatTextView txtAmount;
        private AppCompatTextView txtFee;
        private AppCompatTextView txtBalance;

        MpambaCashInTransactionModelView(@NonNull View itemView) {
            super(itemView);
            txtTransactionId = itemView.findViewById(R.id.txt_transactionId);
            txtDate = itemView.findViewById(R.id.txt_date);
            txtAgent = itemView.findViewById(R.id.txt_agent);
            txtAmount = itemView.findViewById(R.id.txt_amount);
            txtFee = itemView.findViewById(R.id.txt_fee);
            txtBalance = itemView.findViewById(R.id.txt_balance);
        }

        @Override
        public void bind(Transaction transaction) {
            txtTransactionId.setText(transaction.getTransactionId());
            txtDate.setText(Objects.toString(transaction.getDate()));
            txtAgent.setText(Objects.toString(((MpambaCashInTransaction) transaction).getAgent(), "n/a"));
            txtAmount.setText(String.format(Locale.US, "%,.02f", ((MpambaCashInTransaction) transaction).getAmount()));
            txtFee.setText(String.format(Locale.US, "%,.02f", ((MpambaCashInTransaction) transaction).getFee()));
            txtBalance.setText(String.format(Locale.US, "%,.02f", ((MpambaCashInTransaction) transaction).getBalance()));
        }
    }

    private static final class MpambaDepositTransactionModelView extends TransactionModelView {
        private AppCompatTextView txtTransactionId;
        private AppCompatTextView txtDate;
        private AppCompatTextView txtSource;
        private AppCompatTextView txtAmount;
        private AppCompatTextView txtFee;
        private AppCompatTextView txtBalance;

        MpambaDepositTransactionModelView(@NonNull View itemView) {
            super(itemView);
            txtTransactionId = itemView.findViewById(R.id.txt_transactionId);
            txtDate = itemView.findViewById(R.id.txt_date);
            txtSource = itemView.findViewById(R.id.txt_source);
            txtAmount = itemView.findViewById(R.id.txt_amount);
            txtFee = itemView.findViewById(R.id.txt_fee);
            txtBalance = itemView.findViewById(R.id.txt_balance);
        }

        @Override
        public void bind(Transaction transaction) {
            txtTransactionId.setText(transaction.getTransactionId());
            txtDate.setText(Objects.toString(transaction.getDate()));
            txtSource.setText(Objects.toString(((MpambaDepositTransaction) transaction).getSource(), "n/a"));
            txtAmount.setText(String.format(Locale.US, "%,.02f", ((MpambaDepositTransaction) transaction).getAmount()));
            txtFee.setText(String.format(Locale.US, "%,.02f", ((MpambaDepositTransaction) transaction).getFee()));
            txtBalance.setText(String.format(Locale.US, "%,.02f", ((MpambaDepositTransaction) transaction).getBalance()));
        }
    }

    private static final class MpambaCreditTransactionModelView extends TransactionModelView {
        private AppCompatTextView txtTransactionId;
        private AppCompatTextView txtDate;
        private AppCompatTextView txtRecipientPhone;
        private AppCompatTextView txtRecipientName;
        private AppCompatTextView txtAmount;
        private AppCompatTextView txtFee;
        private AppCompatTextView txtBalance;

        MpambaCreditTransactionModelView(@NonNull View itemView) {
            super(itemView);
            txtTransactionId = itemView.findViewById(R.id.txt_transactionId);
            txtDate = itemView.findViewById(R.id.txt_date);
            txtRecipientName = itemView.findViewById(R.id.txt_recipient_name);
            txtRecipientPhone = itemView.findViewById(R.id.txt_recipient_phone);
            txtAmount = itemView.findViewById(R.id.txt_amount);
            txtFee = itemView.findViewById(R.id.txt_fee);
            txtBalance = itemView.findViewById(R.id.txt_balance);
        }

        @Override
        public void bind(Transaction transaction) {
            txtTransactionId.setText(transaction.getTransactionId());
            txtDate.setText(Objects.toString(transaction.getDate()));
            txtRecipientPhone.setText(((MpambaCreditTransaction) transaction).getRecipientPhone());
            txtRecipientName.setText(((MpambaCreditTransaction) transaction).getRecipientName());
            txtAmount.setText(String.format(Locale.US, "%,.02f", ((MpambaCreditTransaction) transaction).getAmount()));
            txtFee.setText(String.format(Locale.US, "%,.02f", ((MpambaCreditTransaction) transaction).getFee()));
            txtBalance.setText(String.format(Locale.US, "%,.02f", ((MpambaCreditTransaction) transaction).getBalance()));
        }
    }

    private static final class MpambaDebitTransactionModelView extends TransactionModelView {
        private AppCompatTextView txtTransactionId;
        private AppCompatTextView txtDate;
        private AppCompatTextView txtSenderPhone;
        private AppCompatTextView txtSenderName;
        private AppCompatTextView txtAmount;
        private AppCompatTextView txtBalance;

        MpambaDebitTransactionModelView(@NonNull View itemView) {
            super(itemView);
            txtTransactionId = itemView.findViewById(R.id.txt_transactionId);
            txtDate = itemView.findViewById(R.id.txt_date);
            txtSenderName = itemView.findViewById(R.id.txt_sender_name);
            txtSenderPhone = itemView.findViewById(R.id.txt_sender_phone);
            txtAmount = itemView.findViewById(R.id.txt_amount);
            txtBalance = itemView.findViewById(R.id.txt_balance);
        }

        @Override
        public void bind(Transaction transaction) {
            txtTransactionId.setText(transaction.getTransactionId());
            txtDate.setText(Objects.toString(transaction.getDate()));
            txtSenderPhone.setText(((MpambaDebitTransaction) transaction).getSenderPhone());
            txtSenderName.setText(((MpambaDebitTransaction) transaction).getSenderName());
            txtAmount.setText(String.format(Locale.US, "%,.02f", ((MpambaDebitTransaction) transaction).getAmount()));
            txtBalance.setText(String.format(Locale.US, "%,.02f", ((MpambaDebitTransaction) transaction).getBalance()));
        }
    }

    private static final class MpambaCashOutTransactionModelView extends TransactionModelView {
        private AppCompatTextView txtTransactionId;
        private AppCompatTextView txtDate;
        private AppCompatTextView txtAgent;
        private AppCompatTextView txtAmount;
        private AppCompatTextView txtFee;
        private AppCompatTextView txtBalance;

        MpambaCashOutTransactionModelView(@NonNull View itemView) {
            super(itemView);
            txtTransactionId = itemView.findViewById(R.id.txt_transactionId);
            txtDate = itemView.findViewById(R.id.txt_date);
            txtAgent = itemView.findViewById(R.id.txt_agent);
            txtAmount = itemView.findViewById(R.id.txt_amount);
            txtFee = itemView.findViewById(R.id.txt_fee);
            txtBalance = itemView.findViewById(R.id.txt_balance);
        }

        @Override
        public void bind(Transaction transaction) {
            txtTransactionId.setText(transaction.getTransactionId());
            txtDate.setText(Objects.toString(transaction.getDate()));
            txtAgent.setText(Objects.toString(((MpambaCashOutTransaction) transaction).getAgent(), "n/a"));
            txtAmount.setText(String.format(Locale.US, "%,.02f", ((MpambaCashOutTransaction) transaction).getAmount()));
            txtFee.setText(String.format(Locale.US, "%,.02f", ((MpambaCashOutTransaction) transaction).getFee()));
            txtBalance.setText(String.format(Locale.US, "%,.02f", ((MpambaCashOutTransaction) transaction).getBalance()));
        }
    }

    public static final class MpambaTransactionModelViewProvider implements ModelViewProvider {
        private static final int TYPE_CASH_IN = 1;
        private static final int TYPE_DEPOSIT = 2;
        private static final int TYPE_CREDIT = 3;
        private static final int TYPE_DEBIT = 4;
        private static final int TYPE_CASHOUT = 5;

        @Override
        public boolean canHandleTransaction(Transaction transaction) {
            return (transaction instanceof MpambaCashInTransaction)
                || (transaction instanceof MpambaDebitTransaction)
                || (transaction instanceof MpambaCreditTransaction)
                || (transaction instanceof MpambaDepositTransaction)
                || (transaction instanceof MpambaCashOutTransaction);
        }

        @Override
        public int getTransactionType(Transaction transaction) {
            if (transaction instanceof MpambaCashInTransaction) return TYPE_CASH_IN;
            if (transaction instanceof MpambaDebitTransaction) return TYPE_DEBIT;
            if (transaction instanceof MpambaCreditTransaction) return TYPE_CREDIT;
            if (transaction instanceof MpambaDepositTransaction) return TYPE_DEPOSIT;
            if(transaction instanceof MpambaCashOutTransaction) return TYPE_CASHOUT;
            throw new IllegalArgumentException("Unknown transaction type " + transaction.getClass());
        }

        @Override
        public TransactionModelView createModelView(ViewGroup parent, int type) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            switch (type) {
                case TYPE_CASH_IN:
                    return new MpambaCashInTransactionModelView(inflate(parent, R.layout.mpamba_cash_in, inflater));
                case TYPE_DEPOSIT:
                    return new MpambaDepositTransactionModelView(inflate(parent, R.layout.mpamba_deposit, inflater));
                case TYPE_CREDIT:
                    return new MpambaCreditTransactionModelView(inflate(parent, R.layout.mpamba_credit, inflater));
                case TYPE_DEBIT:
                    return new MpambaDebitTransactionModelView(inflate(parent, R.layout.mpamba_debit, inflater));
                case TYPE_CASHOUT:
                    return new MpambaCashOutTransactionModelView(inflate(parent, R.layout.mpamba_cash_out, inflater));
            }
            throw new IllegalArgumentException("Unknown transaction type " + type);
        }

        private View inflate(ViewGroup parent, @LayoutRes int resource, LayoutInflater inflater) {
            return inflater.inflate(resource, parent, false);
        }
    }
}