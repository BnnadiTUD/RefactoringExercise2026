package model;

public final class CurrentAccount extends BankAccount {
    private double overdraft; // >=0

    public CurrentAccount(int accountId, String accountNumber, String surname, String firstName, 
    		double balance, double overdraft) {
        super(accountId, accountNumber, surname, firstName, AccountType.CURRENT, balance);
        this.overdraft = overdraft;
    }

    @Override
    public void withdraw(double amount) {
        double newBal = getBalance() - amount;
        setBalance(newBal);
    }

    @Override
    public double getOverdraft() {
        return overdraft;
    }

    @Override
    public void setOverdraft(double v) {
        overdraft = v;
    }
}