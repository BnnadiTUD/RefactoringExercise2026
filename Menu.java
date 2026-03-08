import javax.swing.SwingUtilities;

import ui.BankFrame;

public class Menu {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            BankFrame frame = new BankFrame();
            frame.setVisible(true);
        });
    }
}