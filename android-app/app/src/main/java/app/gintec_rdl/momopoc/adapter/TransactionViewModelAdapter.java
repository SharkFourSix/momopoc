package app.gintec_rdl.momopoc.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import java.util.ArrayList;

import app.gintec_rdl.momopoc.presentation.ModelViewProviderRegistry;
import app.gintec_rdl.momopoc.presentation.TransactionModelView;
import lib.gintec_rdl.momo.model.Transaction;

public final class TransactionViewModelAdapter extends RecyclerView.Adapter<TransactionModelView> {
    private final ArrayList<Transaction> transactions;

    public TransactionViewModelAdapter() {
        transactions = new ArrayList<>();
    }

    @Override
    public int getItemViewType(int position) {
        return ModelViewProviderRegistry.getInstance().getTransactionType(transactions.get(position));
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @NonNull
    @Override
    public TransactionModelView onCreateViewHolder(@NonNull ViewGroup viewGroup, int type) {
        return ModelViewProviderRegistry.getInstance().createModelView(viewGroup, type);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionModelView transactionModelView, int position) {
        transactionModelView.bind(transactions.get(position));
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    public void addTransaction(Transaction transaction) {
        transactions.add(transaction);
        notifyItemInserted(transactions.size() - 1);
    }
}