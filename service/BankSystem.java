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

    
//operayions for the files
    
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

   
}

