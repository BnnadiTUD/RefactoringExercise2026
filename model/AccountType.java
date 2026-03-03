package model;

public enum AccountType {
    CURRENT("Current"),
    DEPOSIT("Deposit");

    private String display;

    AccountType(String display) {
        this.display = display;
    }

    public String display() {
        return display;
    }

    public static AccountType fromDisplay(String s) {
        for (AccountType t : values()) {
            if (t.display.equalsIgnoreCase(s)) return t;
        }
        throw new IllegalArgumentException("Unknown type: " + s);
    }
}