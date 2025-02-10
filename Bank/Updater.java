package Bank;

import java.util.concurrent.BlockingQueue;
import Bank.Exceptions.InsufficientFundsException;
import java.util.concurrent.LinkedBlockingQueue;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

// Main Updater class
public class Updater implements Runnable{
    // Declarations
    private int updaterId;
    private int branchId;
    private BankDatabase bankdb;

    // Query Queue which is continously polled by the Updater
    private BlockingQueue<Query> queryQueue;

    // Misc. Parameters
    public static final int QUEUE_CAP = 1000;
    private boolean toPrint = false;
    private volatile Integer numUnsuccessfulQueries = 0;

    // Constructs an Updater
    public Updater(int updaterId, int branchId, BankDatabase bankdb) {
        this.updaterId = updaterId;
        this.branchId = branchId;
        this.bankdb = bankdb; // the constructor calls the functions exposed by the bank
        this.queryQueue = new LinkedBlockingQueue<>(QUEUE_CAP);
        this.numUnsuccessfulQueries = 0;
    }

    // Adds a query to this updater's queue (This method is used by everyone to send a query to the Updater)
    public void submitQuery(Query query) {
        try {
            queryQueue.put(query);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void run() {
        // Continuously process queries.
        while (true) {
            try {
                Query query = queryQueue.take(); // Block until a query is available
                if(query.getType() == Query.QueryType.SHUTDOWN){
                    if(toPrint){
                        LocalTime now = LocalTime.now();
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
                        String formattedTime = now.format(formatter);
                        System.out.println(formattedTime + " Updater [" + branchId + "-" + updaterId + "] completed its tasks.");
                    }
                    break;
                }

                processQuery(query);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    // Processes a single query by examining its type and then calling bank functions
    private void processQuery(Query query){
        String result = "";
        try {
            switch(query.getType()) {
                case BALANCE_CHECK: {
                    double balance = bankdb.getBalance(query.getAccountNumber());
                    result = "Balance for account " + query.getAccountNumber() + ": " + balance;
                    break;
                }
                case DEPOSIT: {
                    double newBalance = bankdb.deposit(query.getAccountNumber(), query.getAmount());
                    result = "Deposited " + query.getAmount() + " into account " + query.getAccountNumber()
                            + ". New balance: " + newBalance;
                    break;
                }
                case WITHDRAWAL: {
                    double newBalance = bankdb.withdraw(query.getAccountNumber(), query.getAmount());
                    result = "Withdrew " + query.getAmount() + " from account " + query.getAccountNumber()
                            + ". New balance: " + newBalance;
                    break;
                }
                case TRANSFER_MONEY: {
                    bankdb.transferMoney(query.getAccountNumber(), query.getDestinationAccountNumber(), query.getAmount());
                    result = "Transferred " + query.getAmount() + " from account " + query.getAccountNumber()
                            + " to account " + query.getDestinationAccountNumber();
                    break;
                }
                case ADD_CUSTOMER: {
                    // For ADD_CUSTOMER, we use the branch id of this updater.
                    Account newAccount = bankdb.addCustomer(branchId, query.getAmount());
                    result = "Added new customer at branch " + branchId 
                            + " with account number " + newAccount.getAccountNumber()
                            + " and initial deposit " + query.getAmount();
                    break;
                }
                case DELETE_CUSTOMER: {
                    Account deletedAccount = bankdb.deleteAccount(query.getAccountNumber());
                    result = "Deleted account " + deletedAccount.getAccountNumber();
                    break;
                }
                case TRANSFER_CUSTOMER_ACCOUNT: {
                    Account transferredAccount = bankdb.transferCustomerAccount(query.getAccountNumber(), query.getDestinationBranchId());
                    result = "Transferred account " + query.getAccountNumber() 
                            + " to branch " + query.getDestinationBranchId() 
                            + ". New account number: " + transferredAccount.getAccountNumber();
                    break;
                }
                case LOGGING: {
                    if(query.getAccountNumber() == "CONTINUE"){
                        this.toPrint = true;
                        result = "Set logging to true";
                    }else if(query.getAccountNumber() == "STOP"){
                        this.toPrint = false;
                        result = "Set logging to false";
                    }
                    break;
                }
                default: {
                    result = "Unknown query type: " + query.getType();
                    break;
                }
            }
        } catch (InsufficientFundsException e) {
            result = "Insufficient funds for account " + query.getAccountNumber() + ": " + e.getMessage();
            numUnsuccessfulQueries++;
        } catch (IllegalArgumentException e) {
            result = "Error processing query: " + e.getMessage();
            numUnsuccessfulQueries++;
        } catch (RuntimeException e){
            result = "Runtime exception occured: " + query + e.getStackTrace();
            numUnsuccessfulQueries++;
        }

        // Get current time in 24-hour format
        LocalTime now = LocalTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
        String formattedTime = now.format(formatter);

        // If this is an interactive query, then complete the response future
        if (query.getResponseFuture() != null) {
            result = formattedTime + " " + result;
            query.getResponseFuture().complete(result);
        } else if(this.toPrint){
            System.out.println(formattedTime + " Updater[" + branchId + "-" + updaterId + "]: " + result);
        }
    }

    public Integer getNumUnsuccessfulQueries(){ 
        Integer ret = numUnsuccessfulQueries;
        numUnsuccessfulQueries = 0;
        return ret;
    }
}
