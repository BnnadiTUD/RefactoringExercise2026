package ui.table;

import model.BankAccount;

import javax.swing.table.AbstractTableModel;
import java.util.List;

public final class AccountsTableModel extends AbstractTableModel {
    private final String[] cols = {"AccountID","AccountNumber","Surname","FirstName","AccountType","Balance","Overdraft"};
    private final List<BankAccount> rows;

    public AccountsTableModel(List<BankAccount> rows) {
        this.rows = rows;
    }

    @Override public int getRowCount() { return rows.size(); }
    @Override public int getColumnCount() { return cols.length; }
    @Override public String getColumnName(int column) { return cols[column]; }

    @Override public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false; // required by brief
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        BankAccount a = rows.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> a.getAccountId();
            case 1 -> a.getAccountNumber();
            case 2 -> a.getSurname();
            case 3 -> a.getFirstName();
            case 4 -> a.getType().display();
            case 5 -> a.getBalance();
            case 6 -> a.getOverdraft();
            default -> "";
        };
    }
}