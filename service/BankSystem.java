package service;

import model.*;
import persistence.RandomAccessBankFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class BankSystem {
    private RandomAccessBankFile store;//reads/writes fixed-size records in a 25-slot hashed RandomAccessFile.
    private BankAccount[] table = new BankAccount[RandomAccessBankFile.TABLE_SIZE];//An in-memory array of size 25 mirroring the file slots. Each index corresponds to a file slot. 
    private int currentSlot = -1;//cursor for nav
    private boolean dirty = false;//sees if changes are made
    private int nextAccountId = 1;//auto gen id

    public boolean isDirty() { return dirty; }
    public File getFile() { return store == null ? null : store.getFile(); }

    public BankAccount current() {
        if (currentSlot < 0) return null;
        return table[currentSlot];
      
    }

    //--------------------HELPERS
    
    private void requireOpen() {
        if (store == null) throw new IllegalStateException("Open a file first (File → Open).");
    }

    private int mustFindSlot(String accNum) throws IOException {
        int slot = store.findSlot(accNum);
        if (slot == -1) throw new IllegalStateException("Account not found.");
        return slot;
    }

    private int maxId() {
        int m = 0;
        for (BankAccount a : table) if (a != null) m = Math.max(m, a.getAccountId());
        return m;
    }

    private int firstSlot() {
        for (int i = 0; i < table.length; i++) if (table[i] != null) return i;
        return -1;
    }

    private int lastSlot() {
        for (int i = table.length - 1; i >= 0; i--) if (table[i] != null) return i;
        return -1;
    }

    
//operayions for the files---------------------
    
    public void open(File f) throws IOException {
        closeIfOpen();//close any existing fule
        store = new RandomAccessBankFile(f);//create or open the file
        table = store.readAll();//load all25 slots into mem
        nextAccountId = maxId() + 1;
        currentSlot = firstSlot(); //sets cursor to first account
        dirty = false;
    }

    public void save() {
        requireOpen();
        //  persist on each write; Save clears dirty to satisfy brief workflow.
        dirty = false;
    }

    public void saveAs(File f) throws IOException {
        requireOpen();
        try (RandomAccessBankFile out = new RandomAccessBankFile(f)) {
            for (int i = 0; i < table.length; i++) {
                if (table[i] != null) out.write(i, table[i]);
            }
        }
        dirty = false;
    }

    public void closeIfOpen() throws IOException {
        if (store != null) store.close();
        store = null;
        table = new BankAccount[RandomAccessBankFile.TABLE_SIZE];
        currentSlot = -1;
        dirty = false;
    }


 //------------CRUD based functions for accounts and user validation
    
    public static void validateAccountNumber(String s) {
        if (s == null || !s.matches("\\d{8}")) {
            throw new IllegalArgumentException("Account number must be exactly 8 digits.");
        }
    }

    public static void validateName(String s, String label) {
        if (s == null || s.trim().isEmpty()) throw new IllegalArgumentException(label + " is required.");
        if (s.length() > 20) throw new IllegalArgumentException(label + " must be <= 20 chars.");
    }

    public BankAccount create(String accNum, String surname, String firstName, AccountType type) throws IOException {
        requireOpen();
        validateAccountNumber(accNum);
        validateName(surname, "Surname");
        validateName(firstName, "First name");

        int slot = store.findInsertSlot(accNum);
        if (slot == -1) throw new IllegalStateException("File is full (25 records).");

        int id = nextAccountId++;
        BankAccount a = (type == AccountType.CURRENT)
                ? new CurrentAccount(id, accNum, surname, firstName, 0.0, 0.0)
                : new DepositAccount(id, accNum, surname, firstName, 0.0);

        table[slot] = a;
        store.write(slot, a);
        dirty = true;
        currentSlot = slot;
        return a;
    }

    public void modify(String accNum, String surname, String firstName, AccountType type) throws IOException {
        requireOpen();
        validateAccountNumber(accNum);
        validateName(surname, "Surname");
        validateName(firstName, "First name");

        int slot = mustFindSlot(accNum);
        BankAccount old = table[slot];

        // Keep id/accountNumber/balance; allow type swap by converting object
        BankAccount updated;
        if (type == AccountType.CURRENT) {
            double od = (old instanceof CurrentAccount ca) ? ca.getOverdraft() : 0.0;
            updated = new CurrentAccount(old.getAccountId(), old.getAccountNumber(), surname, firstName, old.getBalance(), od);
        } else {
            updated = new DepositAccount(old.getAccountId(), old.getAccountNumber(), surname, firstName, old.getBalance());
        }

        table[slot] = updated;
        store.write(slot, updated);
        dirty = true;
        currentSlot = slot;
    }

    public void delete(String accNum) throws IOException {
        requireOpen();
        validateAccountNumber(accNum);

        int slot = mustFindSlot(accNum);
        table[slot] = null;
        store.delete(slot);
        dirty = true;
        currentSlot = firstSlot();
    }

    // ---------------- Overdraft / Interest ----------------

    public void setOverdraft(String accNum, double overdraft) throws IOException {
        requireOpen();
        validateAccountNumber(accNum);
        if (overdraft < 0) throw new IllegalArgumentException("Overdraft must be >= 0");

        int slot = mustFindSlot(accNum);
        BankAccount a = table[slot];
        if (!(a instanceof CurrentAccount ca)) throw new IllegalStateException("Overdraft applies only to Current accounts.");
        ca.setOverdraft(overdraft);

        store.write(slot, ca);
        dirty = true;
    }

    public void setInterestRate(double rate) {
        if (rate < 0) throw new IllegalArgumentException("Interest rate must be >= 0");
        DepositAccount.setInterestRate(rate);
        dirty = true;
    }

    public void calculateInterest() throws IOException {
        requireOpen();
        for (int i = 0; i < table.length; i++) {
            if (table[i] instanceof DepositAccount da) {
                da.applyInterest();
                store.write(i, da);
                dirty = true;
            }
        }
    }

    // ---------------- Trans --

    public void deposit(String accNum, double amount) throws IOException {
        requireOpen();
        validateAccountNumber(accNum);
        if (amount <= 0) throw new IllegalArgumentException("Amount must be > 0");

        int slot = mustFindSlot(accNum);
        BankAccount a = table[slot];
        a.deposit(amount);
        store.write(slot, a);
        dirty = true;
        currentSlot = slot;
    }

}

