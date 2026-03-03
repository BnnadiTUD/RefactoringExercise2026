package model;

public final class DepositAccount extends BankAccount {
    private static double interestRate = 0.0; // e.g. 0.05 for 5%

    public DepositAccount(int accountId, String accountNumber, String surname, String firstName, double balance) {
        super(accountId, accountNumber, surname, firstName, AccountType.DEPOSIT, balance);
    }

    @Override
    public void withdraw(double amount) {
        setBalance(getBalance() - amount);
    }

    public static double getInterestRate() { return interestRate; }

    public static void setInterestRate(double r) {
        interestRate = r;
    }

    public void applyInterest() {
        setBalance(getBalance() + getBalance() * interestRate);
    }
}
