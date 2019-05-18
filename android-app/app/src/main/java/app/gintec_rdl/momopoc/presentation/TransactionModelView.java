package app.gintec_rdl.momopoc.presentation;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import lib.gintec_rdl.momo.model.Transaction;

public abstract class TransactionModelView extends RecyclerView.ViewHolder {

    public TransactionModelView(@NonNull View itemView) {
        super(itemView);
    }

    public abstract void bind(Transaction transaction);
}