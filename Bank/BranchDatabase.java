package Bank;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import Bank.Exceptions.InsufficientFundsException;
import static Bank.Constants.NUM_UPDATERS_PER_BRANCH;

// This class implements all the database specific functions of a branch
public class BranchDatabase {
    private int branchId;
    private ConcurrentHashMap<String, Account> db = new ConcurrentHashMap<>(); // Stores all the accounts in a thread-safe manner
    public AtomicInteger currentAccountNumber = new AtomicInteger(0); // will be increased every time a new account is created
    private Updater[] updaters;

    // Constructor takes a bankdb object and uses it to construct new Updaters (since they need access to the bankdb)
    public BranchDatabase(int branchId, BankDatabase bankdb){
        this.branchId = branchId;
        this.updaters = new Updater[NUM_UPDATERS_PER_BRANCH];

        for(int i = 0; i < updaters.length; i++){
            updaters[i] = new Updater(i, branchId, bankdb); // Create Updater Object
        }
    }

    // createNewAccount creates a new Account Object and saves it in the concurrent hash map
    public Account createNewAccount(double balance){
        int accountNum = currentAccountNumber.getAndIncrement();

        String accountNumber = String.format("%d%09d", branchId, accountNum);
        Account account = new Account(accountNumber, balance);        
        db.put(accountNumber, account); 
        
        return account;
    }

    // Returns the current balance of the account identified by accountNumber
    public double getBalance(String accountNumber) throws IllegalArgumentException{
        Account account = db.get(accountNumber);
        if (account == null){
            throw new IllegalArgumentException("Account not found: " + accountNumber);
        }
        return account.getBalance();
    }

    // Adds the specified amount to the account's balance
    public double addBalance(String accountNumber, double amount) throws IllegalArgumentException{
        Account account = db.get(accountNumber);
        if (account == null) {
            throw new IllegalArgumentException("Account not found: " + accountNumber);
        }
        return account.deposit(amount);
    }

    // Deletes the account from the database
    public Account deleteAccount(String accountNumber) throws IllegalArgumentException{
        Account removed = db.remove(accountNumber);
        if (removed == null) {
            throw new IllegalArgumentException("Account not found: " + accountNumber);
        }
        return removed;
    }

    // Withdraws the specified amount from the account's balance.
    public double withdrawBalance(String accountNumber, double amount) throws IllegalArgumentException, InsufficientFundsException {
        Account account = db.get(accountNumber);
        if (account == null) {
            throw new IllegalArgumentException("Account not found: " + accountNumber);
        }
        return account.withdraw(amount);
    }

    // Expose a method to retrieve an Updater reference by index.
    public Updater getUpdater(int updaterIndex) throws IllegalArgumentException{
        if (updaterIndex < 0 || updaterIndex >= updaters.length) {
            throw new IllegalArgumentException("Invalid updater index: " + updaterIndex);
        }
        return updaters[updaterIndex];
    }
}