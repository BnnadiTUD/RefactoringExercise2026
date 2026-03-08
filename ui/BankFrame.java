package ui;

import model.*;
import service.BankSystem;
import ui.table.AccountsTableModel;

import javax.swing.*;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;

public final class BankFrame extends JFrame {

    private final BankSystem bank = new BankSystem();

    // Read-only fields for current record display
    private final JTextField tfId = roField();
    private final JTextField tfAccNum = roField();
    private final JTextField tfSurname = roField();
    private final JTextField tfFirst = roField();
    private final JTextField tfType = roField();
    private final JTextField tfBalance = roField();
    private final JTextField tfOverdraft = roField();
    private final JLabel status = new JLabel("(no file open)");

    private static JTextField roField() {
        JTextField t = new JTextField(18);
        t.setEditable(false);
        return t;
    }

    @FunctionalInterface
    private interface IORunnable {
        void run() throws IOException;
    }

    private void safe(Runnable r) {
        try {
            r.run();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void safeIO(IORunnable r) {
        try {
            r.run();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "I/O Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public BankFrame() {
        super("Bank Application");

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) {
                exitApplication();
            }
        });

        setJMenuBar(buildMenuBar());
        setContentPane(buildMainPanel());

        pack();
        setLocationRelativeTo(null);
        refresh();
    }

    private JPanel buildMainPanel() {
        JPanel form = new JPanel(new GridLayout(7, 2, 8, 8));
        form.setBorder(BorderFactory.createTitledBorder("Current Record (scroll via navigation)"));

        form.add(new JLabel("AccountID:"));      form.add(tfId);
        form.add(new JLabel("AccountNumber:"));  form.add(tfAccNum);
        form.add(new JLabel("Surname:"));        form.add(tfSurname);
        form.add(new JLabel("FirstName:"));      form.add(tfFirst);
        form.add(new JLabel("AccountType:"));    form.add(tfType);
        form.add(new JLabel("Balance:"));        form.add(tfBalance);
        form.add(new JLabel("Overdraft:"));      form.add(tfOverdraft);

        JPanel navButtons = new JPanel();
        JButton bFirst = new JButton("First");
        JButton bPrev  = new JButton("Previous");
        JButton bNext  = new JButton("Next");
        JButton bLast  = new JButton("Last");

        bFirst.addActionListener(e -> safe(() -> { bank.first(); refresh(); }));
        bPrev.addActionListener(e -> safe(() -> { bank.previous(); refresh(); }));
        bNext.addActionListener(e -> safe(() -> { bank.next(); refresh(); }));
        bLast.addActionListener(e -> safe(() -> { bank.last(); refresh(); }));

        navButtons.add(bFirst);
        navButtons.add(bPrev);
        navButtons.add(bNext);
        navButtons.add(bLast);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(status, BorderLayout.NORTH);
        root.add(form, BorderLayout.CENTER);
        root.add(navButtons, BorderLayout.SOUTH);
        return root;
    }

    private JMenuBar buildMenuBar() {
        JMenuBar mb = new JMenuBar();

        JMenu nav = new JMenu("Navigate");
        nav.add(mi("First", () -> { bank.first(); refresh(); }));
        nav.add(mi("Last", () -> { bank.last(); refresh(); }));
        nav.add(mi("Next", () -> { bank.next(); refresh(); }));
        nav.add(mi("Previous", () -> { bank.previous(); refresh(); }));
        nav.addSeparator();
        nav.add(mi("Find By Account Number", this::findByAccountNumber));
        nav.add(mi("Find By Surname", this::findBySurname));
        nav.add(mi("List All", this::listAll));
        mb.add(nav);

        JMenu rec = new JMenu("Records");
        rec.add(mi("Create", this::createRecord));
        rec.add(mi("Modify", this::modifyRecord));
        rec.add(mi("Delete", this::deleteRecord));
        rec.addSeparator();
        rec.add(mi("Set Overdraft", this::setOverdraft));
        rec.add(mi("Set Interest Rate", this::setInterestRate));
        mb.add(rec);

        JMenu tx = new JMenu("Transactions");
        tx.add(mi("Deposit", () -> amountDialog(true)));
        tx.add(mi("Withdraw", () -> amountDialog(false)));
        tx.add(mi("Calculate Interest", () -> safeIO(() -> {
            bank.calculateInterest();
            refresh();
        })));
        mb.add(tx);

        JMenu file = new JMenu("File");
        file.add(mi("Open", this::openFile));
        file.add(mi("Save", () -> { bank.save(); refresh(); }));
        file.add(mi("Save As", this::saveAs));
        mb.add(file);

        JMenu exit = new JMenu("Exit");
        exit.add(mi("Exit Application", this::exitApplication));
        mb.add(exit);

        return mb;
    }

    private JMenuItem mi(String text, Runnable r) {
        JMenuItem it = new JMenuItem(text);
        it.addActionListener(e -> safe(r));
        return it;
    }

    private void refresh() {
        File f = bank.getFile();
        String fileName = (f == null) ? "(no file open)" : f.getName();
        status.setText("File: " + fileName + (bank.isDirty() ? " *unsaved*" : ""));

        BankAccount a = bank.current();
        if (a == null) {
            tfId.setText("");
            tfAccNum.setText("");
            tfSurname.setText("");
            tfFirst.setText("");
            tfType.setText("");
            tfBalance.setText("");
            tfOverdraft.setText("");
            return;
        }

        tfId.setText(String.valueOf(a.getAccountId()));
        tfAccNum.setText(a.getAccountNumber());
        tfSurname.setText(a.getSurname());
        tfFirst.setText(a.getFirstName());
        tfType.setText(a.getType().display());
        tfBalance.setText(String.valueOf(a.getBalance()));
        tfOverdraft.setText(String.valueOf(a.getOverdraft()));
    }

    // ---------------- File menu ----------------

    private void openFile() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();

        safeIO(() -> {
            bank.open(f);
            refresh();
        });
    }

    private void saveAs() {
        requireFileOpenUI();

        JFileChooser fc = new JFileChooser();
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();

        safeIO(() -> {
            bank.saveAs(f);
            refresh();
        });
    }

    private void exitApplication() {
        if (bank.isDirty()) {
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "You have unsaved changes. Save before exiting?",
                    "Exit",
                    JOptionPane.YES_NO_CANCEL_OPTION
            );
            if (choice == JOptionPane.CANCEL_OPTION) return;
            if (choice == JOptionPane.YES_OPTION) safe(() -> { bank.save(); refresh(); });
        }
        dispose();
        System.exit(0);
    }

    // ---------------- Navmenu ----------------

    private void findByAccountNumber() {
        requireFileOpenUI();

        String accNum = JOptionPane.showInputDialog(this, "Enter AccountNumber (8 digits):");
        if (accNum == null) return;

        safeIO(() -> {
            BankAccount a = bank.findByAccountNumber(accNum.trim());
            if (a == null) {
                JOptionPane.showMessageDialog(this, "Not found.", "Find", JOptionPane.INFORMATION_MESSAGE);
            }
            refresh();
        });
    }

    private void findBySurname() {
        requireFileOpenUI();

        String surname = JOptionPane.showInputDialog(this, "Enter Surname:");
        if (surname == null) return;

        safe(() -> {
            List<BankAccount> matches = bank.findBySurname(surname.trim());
            if (matches.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No matches.", "Find By Surname", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            StringBuilder sb = new StringBuilder();
            for (BankAccount a : matches) sb.append(a).append("\n");

            JTextArea ta = new JTextArea(sb.toString(), 12, 60);
            ta.setEditable(false);
            JOptionPane.showMessageDialog(this, new JScrollPane(ta), "Matches", JOptionPane.INFORMATION_MESSAGE);
        });
    }

    private void listAll() {
        requireFileOpenUI();

        safe(() -> {
            AccountsTableModel model = new AccountsTableModel(bank.listAll());
            JTable table = new JTable(model);
            table.setRowSorter(new TableRowSorter<>(model)); // sortable on all fields
            table.setFillsViewportHeight(true);

            JDialog d = new JDialog(this, "List All (Sortable, Not Editable)", true);
            d.setLayout(new BorderLayout());
            d.add(new JScrollPane(table), BorderLayout.CENTER);

            JButton close = new JButton("Close");
            close.addActionListener(e -> d.dispose());
            JPanel bottom = new JPanel();
            bottom.add(close);
            d.add(bottom, BorderLayout.SOUTH);

            d.setSize(900, 320);
            d.setLocationRelativeTo(this);
            d.setVisible(true);
        });
    }

    // - Records menu ----------------

    private void createRecord() {
        requireFileOpenUI();

        JTextField accNum = new JTextField(10);
        JTextField surname = new JTextField(20);
        JTextField first = new JTextField(20);
        JComboBox<String> type = new JComboBox<>(new String[]{"Current", "Deposit"});

        Object[] msg = {
                "AccountNumber (8 digits):", accNum,
                "Surname (<=20):", surname,
                "FirstName (<=20):", first,
                "AccountType:", type
        };

        int ok = JOptionPane.showConfirmDialog(this, msg, "Create Record", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;

        safeIO(() -> {
            AccountType t = AccountType.fromDisplay((String) type.getSelectedItem());
            bank.create(accNum.getText().trim(), surname.getText().trim(), first.getText().trim(), t);
            refresh();
        });
    }

    private void modifyRecord() {
        requireFileOpenUI();

        JTextField accNum = new JTextField(10);
        JTextField surname = new JTextField(20);
        JTextField first = new JTextField(20);
        JComboBox<String> type = new JComboBox<>(new String[]{"Current", "Deposit"});

        Object[] msg = {
                "AccountNumber (8 digits):", accNum,
                "New Surname (<=20):", surname,
                "New FirstName (<=20):", first,
                "New AccountType:", type
        };

        int ok = JOptionPane.showConfirmDialog(this, msg, "Modify Record", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;

        safeIO(() -> {
            AccountType t = AccountType.fromDisplay((String) type.getSelectedItem());
            bank.modify(accNum.getText().trim(), surname.getText().trim(), first.getText().trim(), t);
            refresh();
        });
    }

    private void deleteRecord() {
        requireFileOpenUI();

        String accNum = JOptionPane.showInputDialog(this, "Enter AccountNumber (8 digits) to delete:");
        if (accNum == null) return;

        int confirm = JOptionPane.showConfirmDialog(this, "Delete account " + accNum + "?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        safeIO(() -> {
            bank.delete(accNum.trim());
            refresh();
        });
    }

    private void setOverdraft() {
        requireFileOpenUI();

        JTextField accNum = new JTextField(10);
        JTextField od = new JTextField(10);

        Object[] msg = {
                "AccountNumber (8 digits):", accNum,
                "Overdraft (>=0):", od
        };

        int ok = JOptionPane.showConfirmDialog(this, msg, "Set Overdraft (Current only)", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;

        safeIO(() -> {
            double v = Double.parseDouble(od.getText().trim());
            bank.setOverdraft(accNum.getText().trim(), v);
            refresh();
        });
    }

    private void setInterestRate() {
        requireFileOpenUI();

        String current = String.valueOf(DepositAccount.getInterestRate());
        String s = JOptionPane.showInputDialog(this, "Enter interest rate (e.g. 0.05 for 5%):", current);
        if (s == null) return;

        safe(() -> {
            double r = Double.parseDouble(s.trim());
            bank.setInterestRate(r);
            refresh();
        });
    }

    // ---------------- Trans menu ----------------

    private void amountDialog(boolean isDeposit) {
        requireFileOpenUI();

        JTextField accNum = new JTextField(10);
        JTextField amt = new JTextField(10);

        Object[] msg = {
                "AccountNumber (8 digits):", accNum,
                "Amount (>0):", amt
        };

        int ok = JOptionPane.showConfirmDialog(this, msg, isDeposit ? "Deposit" : "Withdraw", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;

        safeIO(() -> {
            double a = Double.parseDouble(amt.getText().trim());
            if (isDeposit) bank.deposit(accNum.getText().trim(), a);
            else bank.withdraw(accNum.getText().trim(), a);
            refresh();
        });
    }

    // ---------------- Helpers ----------------

    private void requireFileOpenUI() {
        if (bank.getFile() == null) throw new IllegalStateException("Open a file first (File → Open).");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BankFrame().setVisible(true));
    }
}