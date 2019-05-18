package app.gintec_rdl.momopoc.presentation;

import android.view.ViewGroup;

import lib.gintec_rdl.momo.model.Transaction;

public interface ModelViewProvider {
    boolean canHandleTransaction(Transaction transaction);
    int getTransactionType(Transaction transaction);
    TransactionModelView createModelView(ViewGroup parent, int type);
}
