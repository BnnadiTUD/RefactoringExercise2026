package persistence;

import model.*;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

public final class RandomAccessBankFile implements AutoCloseable {
    public static final int TABLE_SIZE = 25;

    public static final int EMPTY_ID = 0;
    public static final int DELETED_ID = -1;

    // Fixed ASCII lengths
    private static final int ACCNUM_LEN = 8;
    private static final int NAME_LEN = 20;
    private static final int TYPE_LEN = 7; // "Current"/"Deposit"

    // Record: int + accNum + surname + firstName + type + balance + overdraft
    public static final int RECORD_SIZE = 4 + ACCNUM_LEN + NAME_LEN + NAME_LEN + TYPE_LEN + 8 + 8;

    private final File file;
    private final RandomAccessFile raf;

    public RandomAccessBankFile(File file) throws IOException {
        this.file = file;
        this.raf = new RandomAccessFile(file, "rw");
        ensureCapacity();
    }

    public File getFile() { return file; }

    private void ensureCapacity() throws IOException {
        long needed = (long) TABLE_SIZE * RECORD_SIZE;
        if (raf.length() < needed) {
            raf.setLength(needed);
            for (int i = 0; i < TABLE_SIZE; i++) {
                writeRaw(i, EMPTY_ID, "", "", "", "", 0.0, 0.0);
            }
        }
    }

    private long pos(int slot) { return (long) slot * RECORD_SIZE; }

    public static int hash(String accNum) {
        long v = Long.parseLong(accNum);
        return (int) (v % TABLE_SIZE);
    }

    private static final class Meta {
        final int id;
        final String accNum;
        Meta(int id, String accNum) { this.id = id; this.accNum = accNum; }
    }

    private Meta readMeta(int slot) throws IOException {
        raf.seek(pos(slot));
        int id = raf.readInt();
        String acc = readFixed(ACCNUM_LEN).trim();
        return new Meta(id, acc);
    }

    // Find existing record slot (hash + linear probe)
    public int findSlot(String accNum) throws IOException {
        int start = hash(accNum);
        for (int i = 0; i < TABLE_SIZE; i++) {
            int slot = (start + i) % TABLE_SIZE;
            Meta m = readMeta(slot);

            if (m.id == EMPTY_ID) return -1; 
            if (m.id > 0 && m.accNum.equals(accNum)) return slot;
        }
        return -1;
    }

    // Find slot for inserting new accNum (EMPTY or first DELETED).
    public int findInsertSlot(String accNum) throws IOException {
        int start = hash(accNum);
        int firstDeleted = -1;

        for (int i = 0; i < TABLE_SIZE; i++) {
            int slot = (start + i) % TABLE_SIZE;
            Meta m = readMeta(slot);

            if (m.id > 0 && m.accNum.equals(accNum)) {
                throw new IllegalStateException("Account number already exists.");
            }
            if (m.id == DELETED_ID && firstDeleted == -1) firstDeleted = slot;
            if (m.id == EMPTY_ID) return (firstDeleted != -1) ? firstDeleted : slot;
        }

        return (firstDeleted != -1) ? firstDeleted : -1;
    }

    public BankAccount read(int slot) throws IOException {
        raf.seek(pos(slot));

        int id = raf.readInt();
        String accNum = readFixed(ACCNUM_LEN).trim();
        String surname = readFixed(NAME_LEN).trim();
        String first = readFixed(NAME_LEN).trim();
        String typeStr = readFixed(TYPE_LEN).trim();
        double balance = raf.readDouble();
        double overdraft = raf.readDouble();

        if (id == EMPTY_ID || id == DELETED_ID) return null;

        AccountType type = AccountType.fromDisplay(typeStr);
        if (type == AccountType.CURRENT) {
            return new CurrentAccount(id, accNum, surname, first, balance, overdraft);
        }
        return new DepositAccount(id, accNum, surname, first, balance);
    }

    public void write(int slot, BankAccount a) throws IOException {
        if (a.getType() == AccountType.CURRENT) {
            writeRaw(slot, a.getAccountId(), a.getAccountNumber(), a.getSurname(), a.getFirstName(),
                    a.getType().display(), a.getBalance(), a.getOverdraft());
        } else {
            writeRaw(slot, a.getAccountId(), a.getAccountNumber(), a.getSurname(), a.getFirstName(),
                    a.getType().display(), a.getBalance(), 0.0);
        }
    }

    public void delete(int slot) throws IOException {
        writeRaw(slot, DELETED_ID, "", "", "", "", 0.0, 0.0);
    }

    public BankAccount[] readAll() throws IOException {
        BankAccount[] out = new BankAccount[TABLE_SIZE];
        for (int i = 0; i < TABLE_SIZE; i++) out[i] = read(i);
        return out;
    }

    private void writeRaw(int slot, int id, String acc, String sur, String first,
                          String type, double bal, double od) throws IOException {
        raf.seek(pos(slot));
        raf.writeInt(id);
        writeFixed(acc, ACCNUM_LEN);
        writeFixed(sur, NAME_LEN);
        writeFixed(first, NAME_LEN);
        writeFixed(type, TYPE_LEN);
        raf.writeDouble(bal);
        raf.writeDouble(od);
    }

    private String readFixed(int len) throws IOException {
        byte[] b = new byte[len];
        raf.readFully(b);
        return new String(b, StandardCharsets.US_ASCII);
    }

    private void writeFixed(String s, int len) throws IOException {
        String t = (s == null) ? "" : s;
        if (t.length() > len) t = t.substring(0, len);
        while (t.length() < len) t += " ";
        raf.write(t.getBytes(StandardCharsets.US_ASCII), 0, len);
    }

    @Override
    public void close() throws IOException {
        raf.close();
    }
}