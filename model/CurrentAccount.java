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
        if (amount <= 0) throw new IllegalArgumentException("Amount must be > 0");
        double newBal = getBalance() - amount;
        if (newBal < -overdraft) throw new IllegalStateException("Withdrawal exceeds overdraft limit.");
        setBalance(newBal);
    }

    @Override
    public double getOverdraft() {
        return overdraft;
    }

    @Override
    public void setOverdraft(double v) {
        if (v < 0) throw new IllegalArgumentException("Overdraft must be >= 0");
        overdraft = v;
    }
}