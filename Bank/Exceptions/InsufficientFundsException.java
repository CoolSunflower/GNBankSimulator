package Bank.Exceptions;

public class InsufficientFundsException extends Exception{

    public InsufficientFundsException() {
        super("Insufficient funds to withdraw");
    }
    
    public InsufficientFundsException(double amount, double balance) {
        super("Insufficient funds to withdraw. Current Balance: " + balance + " . Tried to withdraw: " + amount);
    }    
}
