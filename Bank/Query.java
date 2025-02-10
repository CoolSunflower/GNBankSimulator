package Bank;

import java.util.concurrent.CompletableFuture;

// Query objects are passed to the Updaters either by the Simulator or by the user
public class Query {

    public enum QueryType {
        BALANCE_CHECK,
        DEPOSIT,
        WITHDRAWAL,
        TRANSFER_MONEY,
        ADD_CUSTOMER,
        DELETE_CUSTOMER,
        TRANSFER_CUSTOMER_ACCOUNT,
        SHUTDOWN,
        LOGGING
    }
    
    private final QueryType type;
    private final String accountNumber;
    private final double amount;
    private final String destinationAccountNumber;
    private final int destinationBranchId;
    
    // For interactive queries: if non-null, the updater will complete this future.
    private final CompletableFuture<String> responseFuture;
        
    // Constructors for simulation queries (no response needed)
    public Query(QueryType type, String accountNumber) { this(type, accountNumber, 0, null, -1, null); }
    public Query(QueryType type, String accountNumber, double amount) { this(type, accountNumber, amount, null, -1, null); }
    public Query(QueryType type, String sourceAccount, String destinationAccount, double amount) { this(type, sourceAccount, amount, destinationAccount, -1, null); }
    public Query(QueryType type, String accountNumber, int destinationBranchId) { this(type, accountNumber, 0, null, destinationBranchId, null); }
    
    // Constructor for interactive queries (with a response future)
    public Query(QueryType type, String accountNumber, double amount, CompletableFuture<String> responseFuture) { this(type, accountNumber, amount, null, -1, responseFuture); }
    public Query(QueryType type, String accountNumber, CompletableFuture<String> responseFuture) { this(type, accountNumber, 0, null, -1, responseFuture); }
    public Query(QueryType type, String sourceAccount, String destinationAccount, double amount, CompletableFuture<String> responseFuture) { this(type, sourceAccount, amount, destinationAccount, -1, responseFuture); }
    public Query(QueryType type, String accountNumber, int destinationBranchId, CompletableFuture<String> responseFuture) { this(type, accountNumber, 0, null, destinationBranchId, responseFuture); }
    
    // Main constructor with all options
    private Query(QueryType type, String accountNumber, double amount, String destinationAccountNumber,
                  int destinationBranchId, CompletableFuture<String> responseFuture) {
        this.type = type;
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.destinationAccountNumber = destinationAccountNumber;
        this.destinationBranchId = destinationBranchId;
        this.responseFuture = responseFuture;
    }

    // Public get-set methods
    public QueryType getType() { return type; }
    public String getAccountNumber() { return accountNumber; }
    public double getAmount() { return amount; }
    public String getDestinationAccountNumber() { return destinationAccountNumber; }
    public int getDestinationBranchId() { return destinationBranchId; }
    public CompletableFuture<String> getResponseFuture() { return responseFuture; }
}
