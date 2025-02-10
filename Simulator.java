// Bank Imports
import static Bank.Constants.*;
import Bank.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap;

public class Simulator {
    // Simulation Parameters
    private int NUM_INITIAL_ACCOUNTS_PER_BRANCH = 10000;
    private int NUM_TRANSACTIONS_PER_UPDATER = 1000000;

    // Other fixed simulation parameters
    private static final double MIN_INITIAL_BALANCE = 100.0;
    private static final double MAX_INITIAL_BALANCE = 10000.0;
    private static final double MIN_TRANSACTION_AMOUNT = 1.0;
    private static final double MAX_TRANSACTION_AMOUNT = 1000.0;

    // Core objects passed to simulator
    private BankDatabase bank;
    private Updater[][] updaters;
    private Thread[][] updaterThreads;
    private String metrics;

    // Logging?
    private boolean toPrint;

    public Simulator(BankDatabase bank, Updater[][] updaters, Thread[][] updaterThreads, int numInitialAccounts, int numTransactions, boolean saveLogs) {
        this.bank = bank;
        this.updaters = updaters;
        this.updaterThreads = updaterThreads;
        this.NUM_INITIAL_ACCOUNTS_PER_BRANCH = numInitialAccounts;
        this.NUM_TRANSACTIONS_PER_UPDATER = numTransactions;
        this.toPrint = saveLogs;

        // Now signal query logging information to each updater by submitting a special LOGGING query
        for (int branch = 0; branch < NUM_BRANCHES; branch++) {
            for (int upd = 0; upd < NUM_UPDATERS_PER_BRANCH; upd++) {
                updaters[branch][upd].submitQuery(new Query(Query.QueryType.LOGGING, saveLogs ? "CONTINUE" : "STOP" ));
            }
        }
    }

    // Initializes each branch with NUM_INITIAL_ACCOUNTS_PER_BRANCH accounts with random initial balances
    public void initializeAccounts() {
        Random rand = new Random();

        for (int branchId = 0; branchId < NUM_BRANCHES; branchId++) {
            for (int i = 0; i < NUM_INITIAL_ACCOUNTS_PER_BRANCH; i++) {
                // Random initial balance between MIN_INITIAL_BALANCE and MAX_INITIAL_BALANCE
                double initialBalance = MIN_INITIAL_BALANCE + (MAX_INITIAL_BALANCE - MIN_INITIAL_BALANCE) * rand.nextDouble();

                // Directly add to database here
                bank.addCustomer(branchId, initialBalance);
            }

            if(this.toPrint){
                LocalTime now = LocalTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
                String formattedTime = now.format(formatter);
                System.out.println(formattedTime + " Branch " + branchId + " initialized with " + NUM_INITIAL_ACCOUNTS_PER_BRANCH + " accounts.");            
            }
        }

    }

    /*
     * Starts the simulation:
     *  - For each updater, generates NUM_TRANSACTIONS_PER_UPDATER random queries (according to the given probabilities)
     *  - Submits them to the updater
     *  - After random query generation, sends a shutdown query to each updater
     *  - Waits for all updater threads to finish
     *  - Restarts the threads for user simulation 
     *  - Returns the total execution time and metrics string
     */
    public Map.Entry<String, String> simulate() throws InterruptedException {
        if(toPrint){
            LocalTime now = LocalTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
            String formattedTime = now.format(formatter);
            System.out.println(formattedTime + " Simulation starting...");
        }
        long startTime = System.currentTimeMillis();

        // Launch query generation for each updater concurrently
        List<Thread> queryGeneratorThreads = new ArrayList<>();

        for (int branch = 0; branch < NUM_BRANCHES; branch++) {
            for (int upd = 0; upd < NUM_UPDATERS_PER_BRANCH; upd++) {
                // Create an instance of query generator
                QueryGenerator qg = new QueryGenerator(updaters[branch][upd], branch, NUM_TRANSACTIONS_PER_UPDATER);

                Thread t = new Thread(qg, "QueryGenerator-" + branch + "-" + upd);
                queryGeneratorThreads.add(t);

                // Start query generator on seperate thread
                t.start();
            }
        }

        // Wait for all query generator threads to finish
        for (Thread t : queryGeneratorThreads) {
            t.join();
        }

        // Now signal shutdown to each updater by submitting a special shutdown query
        for (int branch = 0; branch < NUM_BRANCHES; branch++) {
            for (int upd = 0; upd < NUM_UPDATERS_PER_BRANCH; upd++) {
                updaters[branch][upd].submitQuery(new Query(Query.QueryType.SHUTDOWN, ""));
            }
        }

        // Wait for all updater threads to finish processing
        for (int branch = 0; branch < NUM_BRANCHES; branch++) {
            for (int upd = 0; upd < NUM_UPDATERS_PER_BRANCH; upd++) {
                updaterThreads[branch][upd].join();
            }
        }

        long endTime = System.currentTimeMillis();
        long execTime = endTime - startTime;

        if(toPrint){
            System.out.println("[SUCCESS] Simulation completed in " + (execTime) + " ms.");
        }

        // Once simulation is complete we need to ensure that the updaters are restarted
        for(int i = 0; i < NUM_BRANCHES; i++){
            for(int j = 0; j < NUM_UPDATERS_PER_BRANCH; j++){
                updaters[i][j] = bank.getUpdater(i, j);
                updaterThreads[i][j] = new Thread(updaters[i][j], "Updater: " + i + " " + "j");
                updaterThreads[i][j].start();
            }
        }

        // Get simulation metrics
        metrics = getMetrics();

        // Return execution time and metrics
        return new AbstractMap.SimpleEntry<String, String>((endTime - startTime) + "", metrics);
    }

    // Helper class that generates and submits random queries to a given updater
    private class QueryGenerator implements Runnable {
        private Updater updater;
        private int branchId;
        private int numTransactions;
        private Random random;

        public QueryGenerator(Updater updater, int branchId, int numTransactions) {
            this.updater = updater;
            this.branchId = branchId;
            this.numTransactions = numTransactions;
            this.random = new Random();
        }

        @Override
        public void run() {
            for (int i = 0; i < numTransactions; i++) {
                double r = random.nextDouble();
                Query query = null;

                if (r < 0.3) { // BALANCE_CHECK: pick a random account from this branch.
                    String acc = generateRandomAccount(branchId);
                    query = new Query(Query.QueryType.BALANCE_CHECK, acc);
                } else if (r < 0.53) { // DEPOSIT: random account, random deposit amount.
                    String acc = generateRandomAccount(branchId);
                    double amount = randomAmount();
                    query = new Query(Query.QueryType.DEPOSIT, acc, amount);
                } else if (r < 0.76) { // WITHDRAWAL: random account, random amount.
                    String acc = generateRandomAccount(branchId);
                    double amount = randomAmount();
                    query = new Query(Query.QueryType.WITHDRAWAL, acc, amount);
                } else if (r < 0.99) { // TRANSFER_MONEY: source from this branch, destination from a random branch.
                    String src = generateRandomAccount(branchId);
                    int destBranch = random.nextInt(NUM_BRANCHES);
                    String dest = generateRandomAccount(destBranch);
                    double amount = randomAmount();
                    query = new Query(Query.QueryType.TRANSFER_MONEY, src, dest, amount);
                } else if (r < 0.993) { // ADD_CUSTOMER: add a new customer with an initial deposit.
                    double initialDeposit = randomAmount();
                    query = new Query(Query.QueryType.ADD_CUSTOMER, "", initialDeposit);
                } else if (r < 0.996) { // DELETE_CUSTOMER: random account from this branch.
                    String acc = generateRandomAccount(branchId);
                    query = new Query(Query.QueryType.DELETE_CUSTOMER, acc);
                } else { // TRANSFER_CUSTOMER_ACCOUNT: random account from this branch and a destination branch different from this.
                    String acc = generateRandomAccount(branchId);
                    int destBranch;
                    do {
                        destBranch = random.nextInt(NUM_BRANCHES);
                    } while (destBranch == branchId);
                    query = new Query(Query.QueryType.TRANSFER_CUSTOMER_ACCOUNT, acc, destBranch);
                }

                // Send query object to the updaters Blocking Queue
                updater.submitQuery(query);
            }
        }

        private String generateRandomAccount(int branchId) {
            int accNum = random.nextInt(bank.getCurrentMaxCount(branchId));
            return String.format("%d%09d", branchId, accNum);
        }

        private double randomAmount() {
            return MIN_TRANSACTION_AMOUNT + (MAX_TRANSACTION_AMOUNT - MIN_TRANSACTION_AMOUNT) * random.nextDouble();
        }
    }

    private String getMetrics(){
        StringBuilder val = new StringBuilder();

        // Get unsuccessful transactions count from each updater
        int totalTransactions = 0;
        int unsuccessfulTransactions = 0;
        for(int bid = 0; bid < NUM_BRANCHES; bid++){
            for(int upd = 0; upd < NUM_UPDATERS_PER_BRANCH; upd++){
                totalTransactions += NUM_TRANSACTIONS_PER_UPDATER;
                unsuccessfulTransactions += updaters[bid][upd].getNumUnsuccessfulQueries();
            }
        }

        // Calculate successful and unsuccessful transactions
        int successfulTransactions = totalTransactions - unsuccessfulTransactions;

        // Calculate percentages
        double successPercentage = ((totalTransactions > 0) & (successfulTransactions > 0)) 
            ? ((double) successfulTransactions / totalTransactions) * 100 
            : 0.0;
        double unsuccessfulPercentage = ((totalTransactions > 0) & (unsuccessfulTransactions > 0)) 
            ? ((double) unsuccessfulTransactions / totalTransactions) * 100 
            : 0.0;

        // Construct the result string
        val.append("Total number of transactions = ").append(totalTransactions).append("\n")
        .append("Total number of successful transactions = ").append(successfulTransactions).append("\n")
        .append("Total number of unsuccessful transactions = ").append(unsuccessfulTransactions).append("\n")
        .append("% of successful transactions = ").append(String.format("%.2f", successPercentage)).append("%\n")
        .append("% of unsuccessful transactions = ").append(String.format("%.2f", unsuccessfulPercentage)).append("%\n");

        return val.toString();
    }
}
