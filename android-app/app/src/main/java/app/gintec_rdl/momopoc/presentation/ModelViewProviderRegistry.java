package app.gintec_rdl.momopoc.presentation;

import android.view.ViewGroup;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import lib.gintec_rdl.momo.model.Transaction;

public final class ModelViewProviderRegistry {

    private static class ProviderContainer {
        static int NEXT_ID = 1;

        int id;
        ModelViewProvider provider;

        ProviderContainer(ModelViewProvider provider) {
            this.id = NEXT_ID++;
            this.provider = provider;
        }
    }

    private final Set<ProviderContainer> registry;

    private static ModelViewProviderRegistry instance;

    private ModelViewProviderRegistry() {
        registry = Collections.synchronizedSet(new LinkedHashSet<ProviderContainer>());
    }

    public static ModelViewProviderRegistry getInstance() {
        synchronized (ModelViewProviderRegistry.class) {
            return (instance == null ? instance = new ModelViewProviderRegistry() : instance);
        }
    }

    public void registerModelViewProvider(Class<? extends ModelViewProvider> clazz) {
        synchronized (registry) {
            try {
                registry.add(new ProviderContainer(clazz.newInstance()));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public int getTransactionType(Transaction transaction) {
        synchronized (registry) {
            for (ProviderContainer container : registry) {
                if (container.provider.canHandleTransaction(transaction)) {
                    final int providerId = container.id;
                    final int type = container.provider.getTransactionType(transaction);
                    return ((providerId << 16) & 0xFFFF0000) | (type & 0x0000FFFF);
                }
            }
        }
        throw new RuntimeException(String.format(Locale.US, "No provider registered for %s", transaction.getClass()));
    }

    public TransactionModelView createModelView(ViewGroup parent, int viewType) {
        final int id = (viewType >> 16) & 0x0000FFFF;
        final int type = viewType & 0x0000FFFF;
        synchronized (registry) {
            for (ProviderContainer container : registry) {
                if (id == container.id) {
                    return container.provider.createModelView(parent, type);
                }
            }
        }
        throw new RuntimeException("No provider found for given type");
    }
}
