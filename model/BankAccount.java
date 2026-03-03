package model;

public abstract class BankAccount {

    private int accountId;            // auto-generated unique
    private String accountNumber;     // 8 digits 
    private String surname;           // 20 chars
    private String firstName;         // 20 chars
    private AccountType type;         // Current or depo
    private double balance;           // starts at 0.0

    protected BankAccount(int accountId, String accountNumber, String surname, String firstName, 
    		AccountType type, double balance) {
        this.accountId = accountId;
        this.accountNumber = accountNumber;
        this.surname = surname;
        this.firstName = firstName;
        this.type = type;
        this.balance = balance;
    }

    public int getAccountId() { 
    	return accountId; 
    	}
    
    public void setAccountId(int accountId) { 
    	this.accountId = accountId; 
    	}

    public String getAccountNumber() { 
    	return accountNumber; 
    	}
    
    public void setAccountNumber(String accountNumber) { 
    	this.accountNumber = accountNumber; 
    	}

    public String getSurname() { 
    	return surname; 
    	}
    
    public void setSurname(String surname) { 
    	this.surname = surname; 
    	}

    public String getFirstName() { 
    	return firstName; 
    	}
    
    public void setFirstName(String firstName) 
    { this.firstName = firstName; 
    }

    public AccountType getType() { 
    	return type; 
    	}
    
    public void setType(AccountType type) { 
    	this.type = type; 
    	}

    public double getBalance() {
    	return balance; 
    	}
    
    public void setBalance(double balance) {
    	this.balance = balance; 
    	}

    public void deposit(double amount) {
        balance += amount;
    }

    public abstract void withdraw(double amount);

    // Overdraft applies only to  a currant acc
    public double getOverdraft() { 
    	return 0.0; 
    	}
    
    public void setOverdraft(double v) { 
}

    @Override
    public String toString() {
        return "AccountID=" + accountId +
                ", AccountNumber=" + accountNumber +
                ", Surname=" + surname +
                ", FirstName=" + firstName +
                ", Type=" + type.display() +
                ", Balance=" + balance +
                ", Overdraft=" + getOverdraft();
    }
}