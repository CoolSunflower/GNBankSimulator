package Bank;

import Bank.Exceptions.InsufficientFundsException;

// Main account class with all methods synchronised on the current object of the class
public class Account{
    private String accountNumber;
    private double balance;

    public Account(String accountNumber, double balance){
        this.accountNumber = accountNumber;
        this.balance = balance;
    }

    public synchronized double withdraw (double amount) throws InsufficientFundsException{
        if(amount > this.balance){
            throw new InsufficientFundsException(amount, balance);
        }

        balance -= amount;
        return balance;
    }
    
    public synchronized double deposit(double amount){
        balance += amount;
        return balance;
    }

    public synchronized double getBalance(){ return balance; }
    public String getAccountNumber(){ return accountNumber; }
}