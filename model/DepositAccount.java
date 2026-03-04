package model;

public final class DepositAccount extends BankAccount {
    private static double interestRate = 0.0; // e.g. 0.05 for 5%

    public DepositAccount(int accountId, String accountNumber, String surname, String firstName, double balance) {
        super(accountId, accountNumber, surname, firstName, AccountType.DEPOSIT, balance);
    }

    @Override
    public void withdraw(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be > 0");
        if (amount > getBalance()) throw new IllegalStateException("Withdrawal exceeds balance for Deposit account.");
        setBalance(getBalance() - amount);
    }

    public static double getInterestRate() { return interestRate; }

    public static void setInterestRate(double r) {
        if (r < 0) throw new IllegalArgumentException("Interest rate must be >= 0");
        interestRate = r;
    }

    public void applyInterest() {
        setBalance(getBalance() + getBalance() * interestRate);
    }
}
