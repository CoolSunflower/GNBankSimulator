package Bank;

import static Bank.Constants.NUM_BRANCHES;

import Bank.Exceptions.InsufficientFundsException;

// Asynchoronous implementation of all bank functions
public class BankDatabase{
    private BranchDatabase[] branches = new BranchDatabase[NUM_BRANCHES];

    // Constructor creates instances of BranchDatabase
    public BankDatabase(){
        for(int i = 0; i < branches.length; i++){
            branches[i] = new BranchDatabase(i, this);
        }
    }

    // This class gives asynchoronous implementation of all business logic functions
    // 1. Get Balance
    public double getBalance(String accountNumber){
        int branchId = getBranchIdFromAccountNumber(accountNumber);
        return branches[branchId].getBalance(accountNumber);
    }

    // 2. Deposit (Add Balance)
    public double deposit(String accountNumber, double amount) throws IllegalArgumentException {
        if(amount < 0){
            throw new IllegalArgumentException("Invalid amount provided: " + amount + " . Must be positive.");
        }
        int branchId = getBranchIdFromAccountNumber(accountNumber);
        return branches[branchId].addBalance(accountNumber, amount);
    }

    // 3. Withdraw Balance
    public double withdraw(String accountNumber, double amount) throws InsufficientFundsException, IllegalArgumentException {
        if(amount < 0){
            throw new IllegalArgumentException("Invalid amount provided: " + amount + " . Must be positive.");
        }
        int branchId = getBranchIdFromAccountNumber(accountNumber);
        return branches[branchId].withdrawBalance(accountNumber, amount);
    }

    // 4. Delete Account
    public Account deleteAccount(String accountNumber) {
        int branchId = getBranchIdFromAccountNumber(accountNumber);
        return branches[branchId].deleteAccount(accountNumber);
    }

    // 5. Add Customer (Create a new account with an initial deposit)
    public Account addCustomer(int branchId, double initialDeposit) throws IllegalArgumentException {
        if (branchId < 0 || branchId >= branches.length) {
            throw new IllegalArgumentException("Invalid branch id: " + branchId);
        }
        BranchDatabase branch = branches[branchId];
        return branch.createNewAccount(initialDeposit);
    }

    // 6. Transfer Money between two accounts (which may be in different branches)
    public void transferMoney(String sourceAccount, String destinationAccount, double amount) throws InsufficientFundsException, IllegalArgumentException{
        // 1: Withdraw money from the source account.
        int sourceBranchId = getBranchIdFromAccountNumber(sourceAccount);
        branches[sourceBranchId].withdrawBalance(sourceAccount, amount);

        // Deposit money into the destination account only if sufficient funds
        int destinationBranchId = getBranchIdFromAccountNumber(destinationAccount);
        try {
            // 2: Attempt to deposit into the destination account.
            branches[destinationBranchId].addBalance(destinationAccount, amount);
        } catch (RuntimeException depositException) {
            // 3: Deposit failed.
            // Roll back by depositing the withdrawn amount back to the source account.
            try {
                branches[sourceBranchId].addBalance(sourceAccount, amount);
                throw new RuntimeException("Transfer failed during deposit operation. Rollback succeeded.", depositException);
            } catch (RuntimeException rollbackException) {
                throw new RuntimeException("Critical error: Transfer failed and rollback failed.", rollbackException);
            }
        }
    }

    // 7. Transfer a customer account from one branch to another.
    // This is done by deleting the account from the source branch and
    // creating a new account in the destination branch with the same balance.
    public Account transferCustomerAccount(String accountNumber, int destinationBranchId) {
        int sourceBranchId = getBranchIdFromAccountNumber(accountNumber);
        if (destinationBranchId < 0 || destinationBranchId >= branches.length) {
            throw new IllegalArgumentException("Invalid destination branch id: " + destinationBranchId);
        }
        BranchDatabase destinationBranch = branches[destinationBranchId];

        double balance;
        synchronized(branches[sourceBranchId]){
            // Retrieve the account balance before deletion.
            balance = branches[sourceBranchId].getBalance(accountNumber);
            // Delete the account from the source branch.
            branches[sourceBranchId].deleteAccount(accountNumber);
        }

        // Create a new account in the destination branch with the same balance.
        return destinationBranch.createNewAccount(balance);
    }

    // * Helper method to extract the branch id from an account number
    private static final int getBranchIdFromAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.isEmpty()){
            throw new IllegalArgumentException("Invalid account number provided. " + accountNumber);
        }
        int branchId = Character.getNumericValue(accountNumber.charAt(0));
        if (branchId < 0 || branchId >= NUM_BRANCHES) {
            throw new IllegalArgumentException("Invalid branch id derived from account number. " + accountNumber);
        }
        return branchId;
    }

    public int getCurrentMaxCount(int branchId){
        // This function returns the max count variable of the branchId
        return branches[branchId].currentAccountNumber.get();
    }

    // Expose a method to get a reference to an Updater (Used for Simulation)
    public Updater getUpdater(int branchId, int updaterIndex) {
        if (branchId < 0 || branchId >= branches.length) {
            throw new IllegalArgumentException("Invalid branch id: " + branchId);
        }
        return branches[branchId].getUpdater(updaterIndex);
    }    
}