package app.gintec_rdl.momopoc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lib.gintec_rdl.momo.model.*;
import spark.Request;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.nio.file.Files;

import static spark.Spark.*;

public class MomoPoc {
    private static final Gson mGson;

    static {
        mGson = new GsonBuilder().setDateFormat("dd/MM/yyyy HH:mm:ss").create();
    }

    public static void main(String[] args) {
        exception(Exception.class, (e, request, response) -> e.printStackTrace());

        // listen on standard port
        port(80);

        // Simulate transaction storage to a database or whatever
        post("/mpamba-transaction/:type", (request, response) -> {
            final Transaction transaction;
            switch (request.params("type")) {
                case "cash-in":
                    transaction = tx(request, MpambaCashInTransaction.class);
                    break;
                case "deposit":
                    transaction = tx(request, MpambaDepositTransaction.class);
                    break;
                case "debit":
                    transaction = tx(request, MpambaDebitTransaction.class);
                    break;
                case "credit":
                    transaction = tx(request, MpambaCreditTransaction.class);
                    break;
				case "cash-out":
					transaction = tx(request, MpambaCashOutTransaction.class);
					break;
                default:
                    transaction = null;
                    halt(HttpURLConnection.HTTP_BAD_REQUEST, "Unknown transaction type");
                    break;
            }
            store(transaction);
            halt(HttpURLConnection.HTTP_OK, "Transaction saved");
            return null;
        });

        // Simulates transaction retrieval from a database
        get("/transaction/:transaction-id", (request, response) -> {
            final String transactionId = request.params("transaction-id");
            if (!transactionId.matches("^[aA-zZ0-9\\-.]+$")) {
                halt(HttpURLConnection.HTTP_BAD_REQUEST, "Bad transaction ID format.");
            }
            final File transactionFile = new File("transactions", transactionId + ".tx");
            if (!Files.exists(transactionFile.toPath())) {
                halt(HttpURLConnection.HTTP_NOT_FOUND, "Transaction by that ID was not found");
            }
            response.status(HttpURLConnection.HTTP_OK);
            response.type("text/html");
            halt(200, "<pre>" + new String(Files.readAllBytes(transactionFile.toPath())) + "</pre>");
            return null;
        });
    }

    private static <T extends Transaction> T tx(Request request, Class<T> type) {
        return mGson.fromJson(request.body(), type);
    }

    private static void store(Transaction tx) {
        final File transactionFile = new File("transactions", tx.getTransactionId() + ".tx");
        try (PrintWriter printWriter = new PrintWriter(transactionFile.getAbsolutePath())) {
            if (tx instanceof MpambaCreditTransaction) {
                printWriter.printf("%-20s: %s%n", "Type", "Credit");
                printWriter.printf("%-20s: %s%n", "Recipient Phone", ((MpambaCreditTransaction) tx).getRecipientPhone());
                printWriter.printf("%-20s: %s%n", "Recipient Name", ((MpambaCreditTransaction) tx).getRecipientName());
                printWriter.printf("%-20s: %,.02f%n", "Amount", ((MpambaCreditTransaction) tx).getAmount());
                printWriter.printf("%-20s: %,.02f%n", "Fee", ((MpambaCreditTransaction) tx).getFee());
                printWriter.printf("%-20s: %,.02f%n", "Balance", ((MpambaCreditTransaction) tx).getBalance());
            } else if (tx instanceof MpambaDebitTransaction) {
                printWriter.printf("%-20s: %s%n", "Type", "Debit");
                printWriter.printf("%-20s: %s%n", "Sender Phone", ((MpambaDebitTransaction) tx).getSenderPhone());
                printWriter.printf("%-20s: %s%n", "Sender Name", ((MpambaDebitTransaction) tx).getSenderName());
                printWriter.printf("%-20s: %,.02f%n", "Amount", ((MpambaDebitTransaction) tx).getAmount());
                printWriter.printf("%-20s: %,.02f%n", "Balance", ((MpambaDebitTransaction) tx).getBalance());
            } else if (tx instanceof MpambaDepositTransaction) {
                printWriter.printf("%-20s: %s%n", "Type", "Deposit");
                printWriter.printf("%-20s: %s%n", "Source", ((MpambaDepositTransaction) tx).getSource());
                printWriter.printf("%-20s: %,.02f%n", "Amount", ((MpambaDepositTransaction) tx).getAmount());
                printWriter.printf("%-20s: %,.02f%n", "Fee", ((MpambaDepositTransaction) tx).getFee());
                printWriter.printf("%-20s: %,.02f%n", "Balance", ((MpambaDepositTransaction) tx).getBalance());
            } else if (tx instanceof MpambaCashInTransaction) {
                final MobileMoneyAgent agent = ((MpambaCashInTransaction) tx).getAgent();
                printWriter.printf("%-20s: %s%n", "Type", "Cash-In");
                printWriter.printf("%-20s: %s%n", "Agent Code", agent != null ? agent.getAgentCode() : "n/a");
                printWriter.printf("%-20s: %s%n", "Agent Name", agent != null ? agent.getAgentName() : "n/a");
                printWriter.printf("%-20s: %,.02f%n", "Amount", ((MpambaCashInTransaction) tx).getAmount());
                printWriter.printf("%-20s: %,.02f%n", "Fee", ((MpambaCashInTransaction) tx).getFee());
                printWriter.printf("%-20s: %,.02f%n", "Balance", ((MpambaCashInTransaction) tx).getBalance());
            } else if(tx instanceof MpambaCashOutTransaction){
                final MobileMoneyAgent agent = ((MpambaCashOutTransaction) tx).getAgent();
                printWriter.printf("%-20s: %s%n", "Type", "Cash-Out");
                printWriter.printf("%-20s: %s%n", "Agent Code", agent != null ? agent.getAgentCode() : "n/a");
                printWriter.printf("%-20s: %s%n", "Agent Name", agent != null ? agent.getAgentName() : "n/a");
                printWriter.printf("%-20s: %,.02f%n", "Amount", ((MpambaCashOutTransaction) tx).getAmount());
                printWriter.printf("%-20s: %,.02f%n", "Fee", ((MpambaCashOutTransaction) tx).getFee());
                printWriter.printf("%-20s: %,.02f%n", "Balance", ((MpambaCashOutTransaction) tx).getBalance());
            }
            printWriter.printf("%-20s: %s%n", "Date", tx.getDate() == null ? "n/a" : tx.getDate());
            printWriter.printf("%-20s: %s%n", "Transaction ID", tx.getTransactionId());
            printWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
