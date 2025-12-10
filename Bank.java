import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.io.*;
import java.sql.*;
import java.util.regex.Pattern;

// Node for Binary Search Tree to store names for efficient searching
class NameBSTNode {
    String name;
    int userId;
    NameBSTNode left, right;

    public NameBSTNode(String name, int userId) {
        this.name = name;
        this.userId = userId;
        left = right = null;
    }
}

// Abstract class for common banking operations using Strategy Pattern
abstract class BankingOperations {
    public abstract void performTransaction(User user, double amount) throws Exception;
    public abstract String getTransactionType();
}

// Concrete class for withdrawal operations
class WithdrawOperation extends BankingOperations {
    @Override
    public void performTransaction(User user, double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive.");
        }
        if (user.getBalance() < amount) {
            throw new IllegalArgumentException("Insufficient balance.");
        }
        double newBalance = user.getBalance() - amount;
        user.setBalance(newBalance);
    }

    @Override
    public String getTransactionType() {
        return "Withdraw";
    }
}

// Concrete class for deposit operations
class DepositOperation extends BankingOperations {
    @Override
    public void performTransaction(User user, double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive.");
        }
        double newBalance = user.getBalance() + amount;
        user.setBalance(newBalance);
    }

    @Override
    public String getTransactionType() {
        return "Deposit";
    }
}

// Main Bank class
class Bank {
    // Data structures to store user and account information
    public static final HashMap<Integer, User> users = new HashMap<>();
    public static final HashMap<Integer, List<User>> userAccounts = new HashMap<>();
    public static final HashMap<Integer, Integer> accountNoToUserId = new HashMap<>();
    public static final TreeMap<String, List<Integer>> nameToUsers = new TreeMap<>();
    public static int nextUserId = 24001;
    public static int nextAccountNo = 24002170;
    public static final String ADMIN_ID = "admin";
    public static final String ADMIN_PASSWORD = "admin123";
    public static String dburl;
    public static String dbuser;
    public static String dbpass;
    public static Connection con;
    public static Scanner scanner = new Scanner(System.in);
    public static NameBSTNode nameBST = null;

    public static void main(String[] args) throws Exception {
        // Database connection details
        dburl = "jdbc:mysql://localhost:3306/bank";
        dbuser = "root";
        dbpass = "";

        // Initialize database connection
        initializeDatabase();
        // Load accounts from database
        loadAccountsFromDatabase();

        boolean exit = true;

        // Main menu loop
        while (exit) {
            System.out.println("\n=== BANK MANAGEMENT SYSTEM ===");
            System.out.println("1) Add New User");
            System.out.println("2) Add Account to Existing User");
            System.out.println("3) User Login");
            System.out.println("4) Admin Panel");
            System.out.println("5) Card Application");
            System.out.println("6) Exit");
            System.out.print("Enter your choice: ");

            int choice =0 ;
            try {
                choice = scanner.nextInt();
            } catch (Exception e) {
                System.out.println("Enter number only");
            }

            scanner.nextLine(); // Consume newline

            switch (choice) {
                case 1:
                    addUser();
                    break;
                case 2:
                    addAccountToExistingUser();
                    break;
                case 3:
                    userLogin();
                    break;
                case 4:
                    adminLogin();
                    break;
                case 5:
                    cardApplication();
                    break;
                case 6:
                    exit = false;
                    saveAccountsToFile();
                    System.out.println("Thank you for using Bank Management System!");
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    // Check if date of birth is in a leap year
    private static boolean isLeapYear(LocalDate dob) {
        int year = dob.getYear();
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
    }

    // Validate email with multiple domains
    private static boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return Pattern.compile(emailRegex).matcher(email).matches();
    }

    // CARD APPLICATION METHOD
    private static void cardApplication() {
        System.out.println("\n=== CARD APPLICATION ===");

        System.out.print("Do you have an existing account? (yes/no): ");
        String hasAccount = scanner.nextLine().toLowerCase();

        if (hasAccount.equals("yes")) {
            System.out.print("Enter your User ID: ");
            int userId = scanner.nextInt();
            scanner.nextLine();

            if (!users.containsKey(userId)) {
                System.out.println("User not found.");
                return;
            }

            System.out.println("Select an account to apply for card:");
            List<User> accounts = userAccounts.get(userId);
            for (int i = 0; i < accounts.size(); i++) {
                User account = accounts.get(i);
                System.out.println((i+1) + ") Account No: " + account.getAccountNo() +
                        " - Type: " + account.getAccountType() +
                        " - Balance: ₹" + account.getBalance());
            }
            System.out.print("Enter account choice: ");
            int accountChoice = scanner.nextInt();
            scanner.nextLine();

            if (accountChoice < 1 || accountChoice > accounts.size()) {
                System.out.println("Invalid account choice.");
                return;
            }

            User account = accounts.get(accountChoice - 1);

            System.out.println("1) Debit Card Application");
            System.out.println("2) Credit Card Application");
            System.out.print("Enter choice: ");
            int cardChoice = scanner.nextInt();
            scanner.nextLine();

            boolean success = false;
            if (cardChoice == 1) {
                if (account.hasDebitCard()) {
                    System.out.println("This account already has a debit card.");
                    return;
                }
                account.setHasDebitCard(true);
                System.out.println("Debit card application submitted successfully.");
                success = true;
            } else if (cardChoice == 2) {
                if (account.hasCreditCard()) {
                    System.out.println("This account already has a credit card.");
                    return;
                }
                account.setHasCreditCard(true);
                System.out.println("Credit card application submitted successfully.");
                success = true;
            } else {
                System.out.println("Invalid choice.");
            }

            if (success) {
                try {
                    PreparedStatement ps = con.prepareStatement(
                            "UPDATE accounts SET hasDebitCard = ?, hasCreditCard = ? WHERE accountNo = ?");
                    ps.setInt(1, account.hasDebitCard() ? 1 : 0);
                    ps.setInt(2, account.hasCreditCard() ? 1 : 0);
                    ps.setInt(3, account.getAccountNo());
                    ps.executeUpdate();
                    ps.close();
                } catch (SQLException e) {
                    System.out.println("Error updating card status: " + e.getMessage());
                }
            }
        } else {
            System.out.println("Please create an account first to apply for a card.");
        }
    }

    // USER LOGIN
    private static void userLogin() {
        System.out.println("\n=== USER LOGIN ===");

        System.out.print("Enter User ID: ");
        int userId ;
        try {
            userId = scanner.nextInt();
            scanner.nextLine(); // Consume newline
        } catch (Exception e) {
            System.out.println("Invalid User ID. Please enter a valid numeric User ID.");
            scanner.nextLine(); // Clear invalid input
            return;
        }

        if (!users.containsKey(userId)) {
            System.out.println("User not found.");
            return;
        }

        User user = users.get(userId);

        System.out.print("Enter Password: ");
        String password = scanner.nextLine();

        if (!password.equals(user.getPassword())) {
            System.out.println("Incorrect password.");
            return;
        }

        // Show accounts for this user
        List<User> accounts = userAccounts.get(userId);
        if (accounts == null || accounts.isEmpty()) {
            System.out.println("No accounts found for this user.");
            return;
        }

        System.out.println("Select an account to login:");
        for (int i = 0; i < accounts.size(); i++) {
            User account = accounts.get(i);
            System.out.println((i+1) + ") Account No: " + account.getAccountNo() +
                    " - Type: " + account.getAccountType() +
                    " - Balance: ₹" + account.getBalance());
        }
        System.out.print("Enter account choice: ");
        int accountChoice = scanner.nextInt();
        scanner.nextLine();

        if (accountChoice < 1 || accountChoice > accounts.size()) {
            System.out.println("Invalid account choice.");
            return;
        }

        User account = accounts.get(accountChoice - 1);

        System.out.print("Enter MPIN (6 digits): ");
        String mpin = scanner.nextLine();

        if (!mpin.equals(account.getMpin())) {
            System.out.println("Incorrect MPIN.");
            return;
        }

        // Successful login - show user menu
        userMenu(user, account);
    }

    // USER MENU
    private static void userMenu(User user, User account) {
        boolean logout = false;

        while (!logout) {
            System.out.println("\n=== USER DASHBOARD ===");
            System.out.println("Welcome, " + user.getFirstName() + " " + user.getLastName() + "!");
            System.out.println("Account No: " + account.getAccountNo());
            System.out.println("Balance: ₹" + account.getBalance());
            System.out.println("\n1) Withdraw Money");
            System.out.println("2) Deposit Money");
            System.out.println("3) Transfer Money");
            System.out.println("4) Check Balance");
            System.out.println("5) View Account Details");
            System.out.println("6) Change Password");
            System.out.println("7) Apply for Card");
            System.out.println("8) Switch Account");
            System.out.println("9) View Mutual Funds (Savings Account Only)");
            System.out.println("10) Logout");
            System.out.print("Enter your choice: ");

            int choice =0 ;
            try {
                choice = scanner.nextInt();
            } catch (Exception e) {
                System.out.println("Enter number only");
            }
            scanner.nextLine(); // Consume newline

            try {
                switch (choice) {
                    case 1:
                        withdrawMoney(account);
                        break;
                    case 2:
                        depositMoney(account);
                        break;
                    case 3:
                        transferMoney(user, account);
                        break;
                    case 4:
                        checkBalance(account);
                        break;
                    case 5:
                        checkDetails(user, account);
                        break;
                    case 6:
                        changePassword(user);
                        break;
                    case 7:
                        applyForCard(account);
                        break;
                    case 8:
                        // Switch to another account
                        List<User> accounts = userAccounts.get(user.getUserId());
                        System.out.println("Select an account:");
                        for (int i = 0; i < accounts.size(); i++) {
                            User acc = accounts.get(i);
                            System.out.println((i+1) + ") Account No: " + acc.getAccountNo() +
                                    " - Type: " + acc.getAccountType() +
                                    " - Balance: ₹" + acc.getBalance());
                        }
                        System.out.print("Enter account choice: ");
                        int accountChoice = scanner.nextInt();
                        scanner.nextLine();

                        if (accountChoice < 1 || accountChoice > accounts.size()) {
                            System.out.println("Invalid account choice.");
                            break;
                        }

                        User newAccount = accounts.get(accountChoice - 1);
                        System.out.print("Enter MPIN for account " + newAccount.getAccountNo() + ": ");
                        String mpin = scanner.nextLine();

                        if (!mpin.equals(newAccount.getMpin())) {
                            System.out.println("Incorrect MPIN.");
                            break;
                        }

                        account = newAccount;
                        System.out.println("Switched to account: " + account.getAccountNo());
                        break;
                    case 9:
                        viewMutualFunds(account);
                        break;
                    case 10:
                        logout = true;
                        System.out.println("Logged out successfully.");
                        break;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            } catch (Exception e) {
                System.out.println("An error occurred: " + e.getMessage());
            }
        }
    }

    // View mutual funds for savings accounts
    private static void viewMutualFunds(User account) {
        System.out.println("\n=== MUTUAL FUNDS ===");

        // Only available for savings accounts
        if ("Savings".equalsIgnoreCase(account.getAccountType())) {
            System.out.println("Mutual Fund Options (Coming Soon):");
            System.out.println("1) Equity Funds");
            System.out.println("2) Debt Funds");
            System.out.println("3) Hybrid Funds");
            System.out.println("4) ELSS (Tax Saving) Funds");
            System.out.println("\nNote: Mutual fund services will be available soon.");
        } else {
            System.out.println("Mutual funds are only available for Savings accounts.");
            System.out.println("Your account type is: " + account.getAccountType());
        }
    }

    // APPLY FOR CARD FROM USER MENU
    private static void applyForCard(User account) {
        System.out.println("\n=== CARD APPLICATION ===");

        System.out.println("1) Debit Card Application");
        System.out.println("2) Credit Card Application");
        System.out.print("Enter choice: ");
        int cardChoice = scanner.nextInt();
        scanner.nextLine();

        boolean success = false;
        if (cardChoice == 1) {
            if (account.hasDebitCard()) {
                System.out.println("This account already has a debit card.");
                return;
            }
            account.setHasDebitCard(true);
            System.out.println("Debit card application submitted successfully.");
            success = true;
        } else if (cardChoice == 2) {
            if (account.hasCreditCard()) {
                System.out.println("This account already has a credit card.");
                return;
            }
            account.setHasCreditCard(true);
            System.out.println("Credit card application submitted successfully.");
            success = true;
        } else {
            System.out.println("Invalid choice.");
        }

        if (success) {
            try {
                PreparedStatement ps = con.prepareStatement(
                        "UPDATE accounts SET hasDebitCard = ?, hasCreditCard = ? WHERE accountNo = ?");
                ps.setInt(1, account.hasDebitCard() ? 1 : 0);
                ps.setInt(2, account.hasCreditCard() ? 1 : 0);
                ps.setInt(3, account.getAccountNo());
                ps.executeUpdate();
                ps.close();
            } catch (SQLException e) {
                System.out.println("Error updating card status: " + e.getMessage());
            }
        }
    }

    // ADMIN LOGIN
    private static void adminLogin() throws Exception {
        System.out.println("\n=== ADMIN LOGIN ===");

        System.out.print("Enter Admin ID: ");
        String adminId = scanner.nextLine();

        System.out.print("Enter Admin Password: ");
        String adminPassword = scanner.nextLine();

        if (!adminId.equals(ADMIN_ID) || !adminPassword.equals(ADMIN_PASSWORD)) {
            System.out.println("Invalid admin credentials.");
            return;
        }
        adminPanel();
    }

    // CHANGE PASSWORD
    private static void changePassword(User user) throws Exception {
        System.out.println("\n=== CHANGE PASSWORD ===");

        System.out.print("Enter current password: ");
        String currentPassword = scanner.nextLine();

        if (!currentPassword.equals(user.getPassword())) {
            System.out.println("Incorrect current password.");
            return;
        }

        String newPassword;
        String confirmPassword;
        while (true) {
            System.out.print("Enter new password: ");
            newPassword = scanner.nextLine();
            System.out.print("Confirm new password: ");
            confirmPassword = scanner.nextLine();
            if (newPassword.equals(confirmPassword)) {
                break;
            }
            System.out.println("Passwords do not match. Please try again.");
        }

        // Update password in database
        PreparedStatement ps = con.prepareStatement("UPDATE users SET password = ? WHERE userId = ?");
        ps.setString(1, newPassword);
        ps.setInt(2, user.getUserId());
        ps.executeUpdate();
        ps.close();

        // Update local object
        user.password = newPassword;

        System.out.println("Password changed successfully.");
    }

    // MODIFIED WITHDRAW METHOD (with User parameter)
    private static void withdrawMoney(User account) throws Exception {
        System.out.println("\n=== WITHDRAW MONEY ===");

        System.out.print("Enter amount to withdraw: ₹");
        double amount = scanner.nextDouble();
        scanner.nextLine(); // Consume newline

        // Use abstract class for withdrawal operation
        BankingOperations withdrawOp = new WithdrawOperation();

        try {
            withdrawOp.performTransaction(account, amount);

            // Update database
            PreparedStatement ps = con.prepareStatement("UPDATE accounts SET balance = ? WHERE accountNo = ?");
            ps.setDouble(1, account.getBalance());
            ps.setInt(2, account.getAccountNo());
            ps.executeUpdate();
            ps.close();

            String transaction = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) +
                    " - Withdraw: ₹" + amount + ", New Balance: ₹" + account.getBalance();
            account.addTransaction(transaction);

            // Update database transaction history
            ps = con.prepareStatement("INSERT INTO transactions VALUES (?, ?)");
            ps.setInt(1, account.getAccountNo());
            ps.setString(2, transaction);
            ps.executeUpdate();
            ps.close();

            System.out.println("Withdrawal successful. New balance: ₹" + account.getBalance());
        } catch (SQLException e) {
            if (e.getMessage().contains("Balance cannot be negative")) {
                System.out.println("Withdrawal failed: Balance cannot be negative.");
            } else {
                System.out.println("Error during withdrawal: " + e.getMessage());
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Withdrawal failed: " + e.getMessage());
        }
    }

    // MODIFIED DEPOSIT METHOD (with User parameter)
    private static void depositMoney(User account) throws Exception {
        System.out.println("\n=== DEPOSIT MONEY ===");

        System.out.print("Enter amount to deposit: ₹");
        double amount = scanner.nextDouble();
        scanner.nextLine(); // Consume newline

        // Use abstract class for deposit operation
        BankingOperations depositOp = new DepositOperation();

        try {
            depositOp.performTransaction(account, amount);

            // Update database
            PreparedStatement ps = con.prepareStatement("UPDATE accounts SET balance = ? WHERE accountNo = ?");
            ps.setDouble(1, account.getBalance());
            ps.setInt(2, account.getAccountNo());
            ps.executeUpdate();
            ps.close();

            String transaction = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) +
                    " - Deposit: ₹" + amount + ", New Balance: ₹" + account.getBalance();
            account.addTransaction(transaction);

            // Update database transaction history
            ps = con.prepareStatement("INSERT INTO transactions VALUES (?, ?)");
            ps.setInt(1, account.getAccountNo());
            ps.setString(2, transaction);
            ps.executeUpdate();
            ps.close();

            System.out.println("Deposit successful. New balance: ₹" + account.getBalance());
        } catch (SQLException e) {
            if (e.getMessage().contains("Balance cannot be negative")) {
                System.out.println("Deposit failed: Balance cannot be negative.");
            } else {
                System.out.println("Error during deposit: " + e.getMessage());
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Deposit failed: " + e.getMessage());
        }
    }

    // MODIFIED TRANSFER METHOD (with User parameters)
    private static void transferMoney(User fromUser, User fromAccount) throws Exception {
        System.out.println("\n=== TRANSFER MONEY ===");
        System.out.println("1) Transfer via Bank Account");
        System.out.println("2) Transfer via UPI (Mobile Number)");
        System.out.print("Enter choice: ");
        int transferChoice=0 ;
        try{
            transferChoice = scanner.nextInt();
        } catch (Exception e) {
            System.out.println("Enter valid number");
        }

        scanner.nextLine(); // Consume newline

        int toAccountNo ;
        User toAccount = null;
        User toUser = null;

        if (transferChoice == 1) {
            // Bank transfer
            System.out.print("Enter recipient's Account No: ");
            toAccountNo = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            if (!accountNoToUserId.containsKey(toAccountNo)) {
                System.out.println("Recipient account not found.");
                return;
            }

            int toUserId = accountNoToUserId.get(toAccountNo);
            toUser = users.get(toUserId);

            // Find the account with the specified account number
            List<User> toAccounts = userAccounts.get(toUserId);
            for (User acc : toAccounts) {
                if (acc.getAccountNo() == toAccountNo) {
                    toAccount = acc;
                    break;
                }
            }

            if (toAccount == null) {
                System.out.println("Recipient account not found.");
                return;
            }
        } else if (transferChoice == 2) {
            // UPI transfer via mobile number
            System.out.print("Enter recipient's Mobile Number: ");
            String mobileNumber = scanner.nextLine();

            // Search for user by mobile number
            for (User user : users.values()) {
                if (user.getMobileNumber().equals(mobileNumber)) {
                    toUser = user;
                    break;
                }
            }

            if (toUser == null) {
                System.out.println("Recipient not found with provided mobile number.");
                return;
            }

            // Get the first account of the recipient (or let them choose if multiple)
            List<User> toAccounts = userAccounts.get(toUser.getUserId());
            if (toAccounts == null || toAccounts.isEmpty()) {
                System.out.println("Recipient has no accounts.");
                return;
            }

            // For simplicity, use the first account
            toAccount = toAccounts.getFirst();
            toAccountNo = toAccount.getAccountNo();

            System.out.print("Enter MPIN: ");
            String mpin = scanner.nextLine();

            if (!mpin.equals(fromAccount.getMpin())) {
                System.out.println("Incorrect MPIN.");
                return;
            }
        } else {
            System.out.println("Invalid choice.");
            return;
        }

        if (fromAccount.getAccountNo() == toAccountNo) {
            System.out.println("Cannot transfer to same account.");
            return;
        }

        System.out.print("Enter amount to transfer: ₹");
        double amount = scanner.nextDouble();
        scanner.nextLine();

        if (amount <= 0) {
            System.out.println("Amount must be positive.");
            return;
        }

        // Use abstract class for withdrawal operation
        BankingOperations withdrawOp = new WithdrawOperation();
        BankingOperations depositOp = new DepositOperation();

        try {
            con.setAutoCommit(false);

            // Withdraw from sender
            withdrawOp.performTransaction(fromAccount, amount);

            PreparedStatement ps = con.prepareStatement("UPDATE accounts SET balance = ? WHERE accountNo = ?");
            ps.setDouble(1, fromAccount.getBalance());
            ps.setInt(2, fromAccount.getAccountNo());
            ps.executeUpdate();

            // Deposit to receiver
            depositOp.performTransaction(toAccount, amount);

            ps.setDouble(1, toAccount.getBalance());
            ps.setInt(2, toAccountNo);
            ps.executeUpdate();

            con.commit();
            con.setAutoCommit(true);

            String fromTransaction = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) +
                    " - Transfer to " + toUser.getFirstName() + " " + toUser.getLastName() +
                    " (" + toAccountNo + "): ₹" + amount +
                    ", New Balance: ₹" + fromAccount.getBalance();

            String toTransaction = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) +
                    " - Transfer from " + fromUser.getFirstName() + " " + fromUser.getLastName() +
                    " (" + fromAccount.getAccountNo() + "): ₹" + amount +
                    ", New Balance: ₹" + toAccount.getBalance();

            fromAccount.addTransaction(fromTransaction);
            toAccount.addTransaction(toTransaction);

            ps = con.prepareStatement("INSERT INTO transactions VALUES (?, ?)");
            ps.setInt(1, fromAccount.getAccountNo());
            ps.setString(2, fromTransaction);
            ps.executeUpdate();

            ps.setInt(1, toAccountNo);
            ps.setString(2, toTransaction);
            ps.executeUpdate();
            ps.close();

            System.out.println("Transfer successful.");
            System.out.println("Your new balance: ₹" + fromAccount.getBalance());

        } catch (SQLException e) {
            try {
                con.rollback();
                con.setAutoCommit(true);
            } catch (SQLException ex) {
                System.out.println("Error during rollback: " + ex.getMessage());
            }

            if (e.getMessage().contains("Balance cannot be negative")) {
                System.out.println("Transfer failed: Balance cannot be negative.");
            } else {
                System.out.println("Error during transfer: " + e.getMessage());
            }
        } catch (IllegalArgumentException e) {
            try {
                con.rollback();
                con.setAutoCommit(true);
            } catch (SQLException ex) {
                System.out.println("Error during rollback: " + ex.getMessage());
            }
            System.out.println("Transfer failed: " + e.getMessage());
        }
    }

    // MODIFIED CHECK BALANCE (with User parameter)
    private static void checkBalance(User account) {
        System.out.println("\n=== ACCOUNT BALANCE ===");
        System.out.println("Current balance: ₹" + account.getBalance());
    }

    // MODIFIED CHECK DETAILS (with User parameters)
    private static void checkDetails(User user, User account) {
        System.out.println("\n=== ACCOUNT DETAILS ===");
        System.out.println("User ID: " + user.getUserId());
        System.out.println("Account No: " + account.getAccountNo());
        System.out.println("Name: " + user.getFirstName() + " " + user.getLastName());
        System.out.println("DOB: " + user.getDob() + (isLeapYear(user.getDob()) ? " (Leap Year)" : ""));
        System.out.println("Mobile: " + user.getMobileNumber());
        System.out.println("Email: " + user.getEmail());
        System.out.println("Account Type: " + account.getAccountType());
        System.out.println("Balance: ₹" + account.getBalance());

        System.out.println("\nTRANSACTION HISTORY (Last 10):");
        for (String transaction : account.getTransactionHistory()) {
            System.out.println(transaction);
        }

        System.out.println("\nSCHEMES:");
        for (String scheme : account.getSchemes()) {
            System.out.println("- " + scheme);
        }

        System.out.println("\nCARDS & LOANS:");
        System.out.println("Debit Card: " + (account.hasDebitCard() ? "Yes" : "No"));
        System.out.println("Credit Card: " + (account.hasCreditCard() ? "Yes" : "No"));
        System.out.println("Loan: " + (account.hasLoan() ? "Yes" : "No"));

        // Show mutual funds info for savings accounts
        if ("Savings".equalsIgnoreCase(account.getAccountType())) {
            System.out.println("\nMUTUAL FUNDS: Coming Soon");
        }
    }

    // TO INITIALIZE DATABASE AND CREATE TABLES
    private static void initializeDatabase() throws SQLException {
        con = DriverManager.getConnection(dburl, dbuser, dbpass);
        Statement statement = con.createStatement();

        // Create users table (personal information)
        String sql = "CREATE TABLE IF NOT EXISTS users (" +
                "userId INTEGER PRIMARY KEY, " +
                "firstName TEXT, " +
                "lastName TEXT, " +
                "dob TEXT, " +
                "mobileNumber TEXT UNIQUE, " +
                "email TEXT, " +
                "aadhaar TEXT UNIQUE, " +
                "pan TEXT UNIQUE, " +
                "password TEXT ," +
                "mpin TEXT)";
        statement.execute(sql);

        // Create accounts table (account-specific information)
        sql = "CREATE TABLE IF NOT EXISTS accounts (" +
                "accountNo INTEGER PRIMARY KEY, " +
                "userId INTEGER, " +
                "accountType TEXT, " +
                "balance REAL, " +
                "hasCreditCard INTEGER, " +
                "hasDebitCard INTEGER, " +
                "hasLoan INTEGER, " +
                "mpin TEXT, " +
                "FOREIGN KEY (userId) REFERENCES users(userId) ON DELETE CASCADE)";
        statement.execute(sql);

        // Create transactions table (child table)
        sql = "CREATE TABLE IF NOT EXISTS transactions (" +
                "accountNo INTEGER, " +
                "transaction TEXT, " +
                "FOREIGN KEY (accountNo) REFERENCES accounts(accountNo) ON DELETE CASCADE)";
        statement.execute(sql);

        // Create schemes table (child table)
        sql = "CREATE TABLE IF NOT EXISTS schemes (" +
                "accountNo INTEGER, " +
                "scheme TEXT, " +
                "FOREIGN KEY (accountNo) REFERENCES accounts(accountNo) ON DELETE CASCADE)";
        statement.execute(sql);

        statement.close();
    }

    // LOAD ACCOUNTS FROM DATABASE
    private static void loadAccountsFromDatabase() throws Exception {
        Statement statement = con.createStatement();

        // Load users
        ResultSet rs = statement.executeQuery("SELECT * FROM users");
        while (rs.next()) {
            int userId = rs.getInt("userId");
            String firstName = rs.getString("firstName");
            String lastName = rs.getString("lastName");
            String dobStr = rs.getString("dob");
            String mobileNumber = rs.getString("mobileNumber");
            String email = rs.getString("email");
            String aadhaar = rs.getString("aadhaar");
            String pan = rs.getString("pan");
            String password = rs.getString("password");
            String mpin = rs.getString("mpin");

            LocalDate dob = LocalDate.parse(dobStr);

            User user = new User(userId, 0, firstName, lastName, dob, 0, mobileNumber, email, aadhaar, pan, "", password,mpin);
            users.put(userId, user);

            String fullName = (firstName + " " + lastName).toLowerCase();
            if (!nameToUsers.containsKey(fullName)) {
                nameToUsers.put(fullName, new ArrayList<>());
            }
            nameToUsers.get(fullName).add(userId);

            if (userId >= nextUserId) nextUserId = userId + 1;
        }

        // Load accounts
        rs = statement.executeQuery("SELECT * FROM accounts");
        while (rs.next()) {
            int accountNo = rs.getInt("accountNo");
            int userId = rs.getInt("userId");
            String accountType = rs.getString("accountType");
            double balance = rs.getDouble("balance");
            boolean hasCreditCard = rs.getInt("hasCreditCard") == 1;
            boolean hasDebitCard = rs.getInt("hasDebitCard") == 1;
            boolean hasLoan = rs.getInt("hasLoan") == 1;
            String mpin = rs.getString("mpin");

            User user = new User(userId, accountNo, "", "", LocalDate.now(), balance, "", "", "", "", accountType, "",mpin);
            user.setHasCreditCard(hasCreditCard);
            user.setHasDebitCard(hasDebitCard);
            user.setHasLoan(hasLoan);
            user.setMpin(mpin);

            // Load transactions
            PreparedStatement ps = con.prepareStatement("SELECT transaction FROM transactions WHERE accountNo = ?");
            ps.setInt(1, accountNo);
            ResultSet rsTransactions = ps.executeQuery();
            while (rsTransactions.next()) {
                user.addTransaction(rsTransactions.getString("transaction"));
            }
            rsTransactions.close();
            ps.close();

            // Load schemes
            ps = con.prepareStatement("SELECT scheme FROM schemes WHERE accountNo = ?");
            ps.setInt(1, accountNo);
            ResultSet rsSchemes = ps.executeQuery();
            while (rsSchemes.next()) {
                user.addScheme(rsSchemes.getString("scheme"));
            }
            rsSchemes.close();
            ps.close();

            // Add to userAccounts map
            if (!userAccounts.containsKey(userId)) {
                userAccounts.put(userId, new ArrayList<>());
            }
            userAccounts.get(userId).add(user);

            accountNoToUserId.put(accountNo, userId);

            if (accountNo >= nextAccountNo) nextAccountNo = accountNo + 1;
        }

        // Build name BST
        nameBST = null;
        for (User user : users.values()) {
            String fullName = (user.getFirstName() + " " + user.getLastName()).toLowerCase();
            nameBST = insertIntoBST(nameBST, fullName, user.getUserId());
        }
    }

    // TO INSERT IN BST
    private static NameBSTNode insertIntoBST(NameBSTNode node, String name, int userId) {
        if (node == null) {
            return new NameBSTNode(name, userId);
        }

        int comparison = name.compareTo(node.name);
        if (comparison < 0) {
            node.left = insertIntoBST(node.left, name, userId);
        } else if (comparison > 0) {
            node.right = insertIntoBST(node.right, name, userId);
        } else {
            node.right = insertIntoBST(node.right, name, userId);
        }
        return node;
    }

    // SEARCH FROM BST
    private static void searchInBST(NameBSTNode node, String name, List<Integer> userIds) {
        if (node == null) {
            return;
        }

        int comparison = name.compareTo(node.name);
        if (comparison == 0) {
            userIds.add(node.userId);
            searchInBST(node.left, name, userIds);
            searchInBST(node.right, name, userIds);
        } else if (comparison < 0) {
            searchInBST(node.left, name, userIds);
        } else {
            searchInBST(node.right, name, userIds);
        }
    }

    // ADD NEW USER
    private static void addUser() {
        System.out.println("\n=== ADD NEW USER ===");

        System.out.print("First name: ");
        String firstName = scanner.nextLine();

        System.out.print("Last name: ");
        String lastName = scanner.nextLine();

        LocalDate dob = null;
        while (dob == null) {
            System.out.print("Date of Birth (YYYY-MM-DD): ");
            String dobStr = scanner.nextLine();
            try {
                dob = LocalDate.parse(dobStr);
                if (dob.isAfter(LocalDate.now().minusYears(18))) {
                    System.out.println("User must be at least 18 years old.");
                    dob = null;
                }
            } catch (Exception e) {
                System.out.println("Invalid date format. Please use YYYY-MM-DD.");
            }
        }

        // Check if DOB is in a leap year
        if (isLeapYear(dob)) {
            System.out.println("Note: You were born in a leap year!");
        }

        String mobileNumber;
        while (true) {
            System.out.print("Mobile number (10 digits): ");
            mobileNumber = scanner.nextLine();
            if (mobileNumber.length() == 10) {
                // Check if mobile number already exists
                boolean exists = false;
                for (User user : users.values()) {
                    if (user.getMobileNumber().equals(mobileNumber)) {
                        exists = true;
                        break;
                    }
                }
                if (exists) {
                    System.out.println("Mobile number already registered. Please use a different number.");
                } else {
                    break;
                }
            } else {
                System.out.println("Invalid mobile number. Must be 10 digits.");
            }
        }

        String email;
        while (true) {
            System.out.print("Email: ");
            email = scanner.nextLine();
            if (isValidEmail(email)) {
                break;
            }
            System.out.println("Invalid email format. Please enter a valid email address.");
        }

        String aadhaar;
        while (true) {
            System.out.print("Aadhaar Card Number (12 digits): ");
            aadhaar = scanner.nextLine();
            if (aadhaar.length() == 12) {
                // Check if Aadhaar already exists
                boolean exists = false;
                for (User user : users.values()) {
                    if (user.getAadhaar().equals(aadhaar)) {
                        exists = true;
                        break;
                    }
                }
                if (exists) {
                    System.out.println("Aadhaar number already registered. Please use a different Aadhaar.");
                } else {
                    break;
                }
            } else {
                System.out.println("Invalid Aadhaar number. Must be 12 digits.");
            }
        }

        String pan;
        while (true) {
            System.out.print("PAN Card Number (10 characters, format: ABCDE1234F): ");
            pan = scanner.nextLine();

            if (pan.length() != 10) {
                System.out.println("PAN must be exactly 10 characters.");
                continue;
            }

            boolean valid = true;
            for (int i = 0; i < 5; i++) {
                char c = pan.charAt(i);
                if (!Character.isLetter(c)) {
                    valid = false;
                    break;
                }
            }

            if (valid) {
                for (int i = 5; i < 9; i++) {
                    char c = pan.charAt(i);
                    if (!Character.isDigit(c)) {
                        valid = false;
                        break;
                    }
                }
            }

            if (valid) {
                char lastChar = pan.charAt(9);
                if (!Character.isLetter(lastChar)) {
                    valid = false;
                }
            }

            if (valid) {
                pan = pan.toUpperCase();
                // Check if PAN already exists
                boolean exists = false;
                for (User user : users.values()) {
                    if (user.getPan().equals(pan)) {
                        exists = true;
                        break;
                    }
                }
                if (exists) {
                    System.out.println("PAN number already registered. Please use a different PAN.");
                } else {
                    break;
                }
            } else {
                System.out.println("Invalid PAN format. Must be in format: ABCDE1234F (5 letters, 4 numbers, 1 letter)");
            }
        }

        String password;
        String confirmPassword;
        while (true) {
            System.out.print("Password: ");
            password = scanner.nextLine();
            System.out.print("Confirm password: ");
            confirmPassword = scanner.nextLine();
            if (password.equals(confirmPassword)) {
                break;
            }
            System.out.println("Passwords do not match. Please try again.");
        }

        int userId = nextUserId++;
        String mpin = "";
        User user = new User(userId, 0, firstName, lastName, dob, 0, mobileNumber, email, aadhaar, pan, "", password,mpin);

        try {
            // Insert into users table
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO users (userId, firstName, lastName, dob, mobileNumber, email, aadhaar, pan, password) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
            ps.setInt(1, userId);
            ps.setString(2, firstName);
            ps.setString(3, lastName);
            ps.setString(4, dob.toString());
            ps.setString(5, mobileNumber);
            ps.setString(6, email);
            ps.setString(7, aadhaar);
            ps.setString(8, pan);
            ps.setString(9, password);
            ps.executeUpdate();
            ps.close();

            users.put(userId, user);

            String fullName = (firstName + " " + lastName).toLowerCase();
            if (!nameToUsers.containsKey(fullName)) {
                nameToUsers.put(fullName, new ArrayList<>());
            }
            nameToUsers.get(fullName).add(userId);

            nameBST = insertIntoBST(nameBST, fullName, userId);

            System.out.println("\nUser created successfully!");
            System.out.println("User ID: " + userId);

            // Ask if user wants to create an account now
            System.out.print("Do you want to create an account for this user now? (yes/no): ");
            String createAccount = scanner.nextLine().toLowerCase();

            if (createAccount.equals("yes")) {
                addAccountToUser(user);
            }
        } catch (SQLException e) {
            nextUserId--;
            System.out.println("Error creating user: " + e.getMessage());
        }
    }

    // ADD ACCOUNT TO EXISTING USER
    private static void addAccountToExistingUser()  {
        System.out.println("\n=== ADD ACCOUNT TO EXISTING USER ===");

        System.out.print("Enter User ID: ");
        int userId = scanner.nextInt();
        scanner.nextLine();

        if (!users.containsKey(userId)) {
            System.out.println("User not found.");
            return;
        }

        User user = users.get(userId);
        addAccountToUser(user);
    }

    // ADD ACCOUNT TO USER
    private static void addAccountToUser(User user)  {
        System.out.println("\n=== ADD ACCOUNT FOR USER: " + user.getFirstName() + " " + user.getLastName() + " ===");

        // Account type selection
        System.out.println("Select Account Type:");
        System.out.println("1) Savings Account");
        System.out.println("2) Current Account");
        System.out.println("3) NRI Account");
        System.out.print("Enter choice (1-3): ");

        int accountTypeChoice =0;
        try{
            accountTypeChoice = scanner.nextInt();
        } catch (Exception e) {
            System.out.println("Enter valid input(only numbers)");
        }
        scanner.nextLine();

        String accountType;
        switch (accountTypeChoice) {
            case 1:
                accountType = "Savings";
                break;
            case 2:
                accountType = "Current";
                break;
            case 3:
                accountType = "NRI";
                break;
            default:
                System.out.println("Invalid choice. Defaulting to Savings Account.");
                accountType = "Savings";
        }

        String mpin;
        while (true) {
            System.out.print("Set MPIN (Length of 6): ");
            mpin = scanner.nextLine();
            if (mpin.length() == 6) {
                break;
            }
            System.out.println("MPIN must be Length of 6.");
        }

        System.out.print("Enter Initial Balance: ₹");
        double bal = 0;
        boolean validInput = false;

        while (!validInput) {
            try {
                String input = scanner.nextLine();
                bal = Double.parseDouble(input);

                if (bal < 0) {
                    System.out.println("Error: Initial balance cannot be negative. Please try again.");
                } else {
                    validInput = true;
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a numeric value.");
            }
        }

        int accountNo = nextAccountNo++;

        User account = new User(user.getUserId(), accountNo, user.getFirstName(), user.getLastName(),
                user.getDob(), bal, user.getMobileNumber(), user.getEmail(),
                user.getAadhaar(), user.getPan(), accountType, user.getPassword(),mpin);

        try {
            // Insert into accounts table
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO accounts VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
            ps.setInt(1, accountNo);
            ps.setInt(2, user.getUserId());
            ps.setString(3, accountType);
            ps.setDouble(4, bal);
            ps.setInt(5, 0); // hasCreditCard
            ps.setInt(6, 0); // hasDebitCard
            ps.setInt(7, 0); // hasLoan
            ps.setString(8, mpin);
            ps.executeUpdate();
            ps.close();

            // Add to userAccounts map
            if (!userAccounts.containsKey(user.getUserId())) {
                userAccounts.put(user.getUserId(), new ArrayList<>());
            }
            userAccounts.get(user.getUserId()).add(account);

            accountNoToUserId.put(accountNo, user.getUserId());

            System.out.println("\nAccount created successfully!");
            System.out.println("Account No: " + accountNo);
            System.out.println("Account Type: " + accountType);
            System.out.println("MPIN: " + mpin + " (Please remember this for login)");

            // Inform about mutual funds for savings accounts
            if ("Savings".equals(accountType)) {
                System.out.println("\nNote: As a Savings account holder, you'll have access to mutual fund investments (Coming Soon).");
            }
        } catch (SQLException e) {
            nextAccountNo--;
            if (e.getMessage().contains("Balance cannot be negative")) {
                System.out.println("Account creation failed: Balance cannot be negative.");
            } else {
                System.out.println("Error creating account: " + e.getMessage());
            }
        }
    }

    // ADMIN PANEL
    private static void adminPanel() throws Exception{
        boolean back = false;
        while (!back) {
            System.out.println("\n=== ADMIN PANEL ===");
            System.out.println("1) Search user by Account No");
            System.out.println("2) Search user by Name");
            System.out.println("3) View all users");
            System.out.println("4) View all accounts");
            System.out.println("5) Add scheme to account");
            System.out.println("6) Issue debit/credit card");
            System.out.println("7) Approve loan");
            System.out.println("8) Delete account");
            System.out.println("9) View Mutual Fund Options (Savings Accounts)");
            System.out.println("10) Back to main menu");
            System.out.print("Enter choice: ");

            int choice = 0;
            try{
                choice = scanner.nextInt();
            } catch (Exception e) {
                System.out.println("Enter valid input(number only)");
            }
            scanner.nextLine();

            switch (choice) {
                case 1:
                    searchByAccountNo();
                    break;
                case 2:
                    searchByName();
                    break;
                case 3:
                    viewAllUsers();
                    break;
                case 4:
                    viewAllAccounts();
                    break;
                case 5:
                    addSchemeToAccount();
                    break;
                case 6:
                    issueCard();
                    break;
                case 7:
                    approveLoan();
                    break;
                case 8:
                    DeleteAccount();
                    break;
                case 9:
                    viewMutualFundOptions();
                    break;
                case 10:
                    back = true;
                    break;
                default:
                    System.out.println("Invalid choice.");
            }
        }
    }

    // View mutual fund options for admin
    private static void viewMutualFundOptions() {
        System.out.println("\n=== MUTUAL FUND OPTIONS FOR SAVINGS ACCOUNTS ===");
        System.out.println("Available Mutual Fund Types (Coming Soon):");
        System.out.println("1. Equity Funds - Invest primarily in stocks");
        System.out.println("2. Debt Funds - Invest primarily in bonds and fixed income securities");
        System.out.println("3. Hybrid Funds - Mix of equity and debt investments");
        System.out.println("4. ELSS Funds - Equity Linked Savings Scheme with tax benefits");
        System.out.println("5. Sectoral/Thematic Funds - Focus on specific sectors or themes");
        System.out.println("6. Index Funds - Track specific market indices");
        System.out.println("7. Fund of Funds - Invest in other mutual funds");
        System.out.println("\nNote: Mutual fund services will be integrated soon.");
    }

    // ADMIN METHODS
    private static void searchByAccountNo() {
        System.out.print("Enter Account No: ");
        int accountNo = scanner.nextInt();
        scanner.nextLine();

        if (!accountNoToUserId.containsKey(accountNo)) {
            System.out.println("Account not found.");
            return;
        }

        int userId = accountNoToUserId.get(accountNo);
        User user = users.get(userId);

        // Find the account
        User account = null;
        List<User> accounts = userAccounts.get(userId);
        for (User acc : accounts) {
            if (acc.getAccountNo() == accountNo) {
                account = acc;
                break;
            }
        }

        if (account == null) {
            System.out.println("Account not found.");
            return;
        }

        displayAccountDetails(user, account);
    }

    private static void searchByName() {
        System.out.print("Enter First Name: ");
        String firstName = scanner.nextLine();
        System.out.print("Enter Last Name: ");
        String lastName = scanner.nextLine();

        String fullName = (firstName + " " + lastName).toLowerCase();
        List<Integer> userIds = new ArrayList<>();
        searchInBST(nameBST, fullName, userIds);

        if (userIds.isEmpty()) {
            System.out.println("No users found with that name.");
            return;
        }

        for (int userId : userIds) {
            User user = users.get(userId);
            System.out.println("\nUSER: " + user.getFirstName() + " " + user.getLastName());
            System.out.println("User ID: " + user.getUserId());
            System.out.println("Mobile: " + user.getMobileNumber());
            System.out.println("Email: " + user.getEmail());

            List<User> accounts = userAccounts.get(userId);
            if (accounts != null && !accounts.isEmpty()) {
                System.out.println("Accounts:");
                for (User account : accounts) {
                    displayAccountDetails(user, account);
                    System.out.println("---------------------");
                }
            } else {
                System.out.println("No accounts found for this user.");
            }
        }
    }

    private static void viewAllUsers() {
        System.out.println("\nALL USERS");
        for (User user : users.values()) {
            System.out.println("User ID: " + user.getUserId() +
                    ", Name: " + user.getFirstName() + " " + user.getLastName() +
                    ", Mobile: " + user.getMobileNumber() +
                    ", Email: " + user.getEmail());
        }
    }

    private static void viewAllAccounts() {
        System.out.println("\nALL ACCOUNTS");
        for (Integer userId : userAccounts.keySet()) {
            User user = users.get(userId);
            List<User> accounts = userAccounts.get(userId);
            for (User account : accounts) {
                System.out.println("User ID: " + userId +
                        ", Name: " + user.getFirstName() + " " + user.getLastName() +
                        ", Account No: " + account.getAccountNo() +
                        ", Type: " + account.getAccountType() +
                        ", Balance: ₹" + account.getBalance());
            }
        }
    }

    private static void addSchemeToAccount() {
        System.out.print("Enter Account No: ");
        int accountNo = scanner.nextInt();
        scanner.nextLine();

        if (!accountNoToUserId.containsKey(accountNo)) {
            System.out.println("Account not found.");
            return;
        }

        int userId = accountNoToUserId.get(accountNo);
        List<User> accounts = userAccounts.get(userId);
        User account = null;
        for (User acc : accounts) {
            if (acc.getAccountNo() == accountNo) {
                account = acc;
                break;
            }
        }

        if (account == null) {
            System.out.println("Account not found.");
            return;
        }

        System.out.print("Enter scheme name: ");
        String scheme = scanner.nextLine();

        account.addScheme(scheme);

        try {
            PreparedStatement ps = con.prepareStatement("INSERT INTO schemes VALUES (?, ?)");
            ps.setInt(1, accountNo);
            ps.setString(2, scheme);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        System.out.println("Scheme added successfully.");
    }

    private static void issueCard() {
        System.out.print("Enter Account No: ");
        int accountNo = scanner.nextInt();
        scanner.nextLine();

        if (!accountNoToUserId.containsKey(accountNo)) {
            System.out.println("Account not found.");
            return;
        }

        int userId = accountNoToUserId.get(accountNo);
        List<User> accounts = userAccounts.get(userId);
        User account = null;
        for (User acc : accounts) {
            if (acc.getAccountNo() == accountNo) {
                account = acc;
                break;
            }
        }

        if (account == null) {
            System.out.println("Account not found.");
            return;
        }

        System.out.println("1) Debit Card");
        System.out.println("2) Credit Card");
        System.out.print("Enter choice: ");
        int cardChoice = scanner.nextInt();
        scanner.nextLine();

        boolean success = false;
        if (cardChoice == 1) {
            account.setHasDebitCard(true);
            System.out.println("Debit card issued successfully.");
            success = true;
        } else if (cardChoice == 2) {
            account.setHasCreditCard(true);
            System.out.println("Credit card issued successfully.");
            success = true;
        } else {
            System.out.println("Invalid choice.");
        }

        if (success) {
            try {
                PreparedStatement ps = con.prepareStatement(
                        "UPDATE accounts SET hasDebitCard = ?, hasCreditCard = ? WHERE accountNo = ?");
                ps.setInt(1, account.hasDebitCard() ? 1 : 0);
                ps.setInt(2, account.hasCreditCard() ? 1 : 0);
                ps.setInt(3, accountNo);
                ps.executeUpdate();
                ps.close();
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private static void approveLoan() {
        System.out.print("Enter Account No: ");
        int accountNo = scanner.nextInt();
        scanner.nextLine();

        if (!accountNoToUserId.containsKey(accountNo)) {
            System.out.println("Account not found.");
            return;
        }

        int userId = accountNoToUserId.get(accountNo);
        List<User> accounts = userAccounts.get(userId);
        User account = null;
        for (User acc : accounts) {
            if (acc.getAccountNo() == accountNo) {
                account = acc;
                break;
            }
        }

        if (account == null) {
            System.out.println("Account not found.");
            return;
        }

        if (account.hasLoan()) {
            System.out.println("This account already has a loan.");
            return;
        }

        System.out.print("Enter loan amount: ₹");
        double amount = scanner.nextDouble();
        scanner.nextLine();

        System.out.print("Enter loan type: ");
        String loanType = scanner.nextLine();

        account.setHasLoan(true);
        account.addScheme(loanType + " Loan: ₹" + amount);

        try {
            PreparedStatement ps = con.prepareStatement("UPDATE accounts SET hasLoan = ? WHERE accountNo = ?");
            ps.setInt(1, 1);
            ps.setInt(2, accountNo);
            ps.executeUpdate();
            ps.close();

            ps = con.prepareStatement("INSERT INTO schemes VALUES (?, ?)");
            ps.setInt(1, accountNo);
            ps.setString(2, loanType + " Loan: ₹" + amount);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        System.out.println("Loan approved successfully.");
    }

    private static void displayAccountDetails(User user, User account) {
        System.out.println("\nACCOUNT DETAILS");
        System.out.println("User ID: " + user.getUserId());
        System.out.println("Account No: " + account.getAccountNo());
        System.out.println("Name: " + user.getFirstName() + " " + user.getLastName());
        System.out.println("Age: " + Period.between(user.getDob(), LocalDate.now()).getYears());
        System.out.println("DOB: " + user.getDob() + (isLeapYear(user.getDob()) ? " (Leap Year)" : ""));
        System.out.println("Mobile: " + user.getMobileNumber());
        System.out.println("Email: " + user.getEmail());
        System.out.println("Account Type: " + account.getAccountType());
        System.out.println("Balance: ₹" + account.getBalance());
        System.out.println("Debit Card: " + (account.hasDebitCard() ? "Yes" : "No"));
        System.out.println("Credit Card: " + (account.hasCreditCard() ? "Yes" : "No"));
        System.out.println("Loan: " + (account.hasLoan() ? "Yes" : "No"));
        System.out.println("Schemes: " + String.join(", ", account.getSchemes()));

        // Show mutual funds info for savings accounts
        if ("Savings".equalsIgnoreCase(account.getAccountType())) {
            System.out.println("Mutual Funds: Coming Soon");
        }
    }

    static void DeleteAccount() throws Exception{
        System.out.print("Enter Account No to delete: ");
        int accountNo = scanner.nextInt();
        scanner.nextLine();

        if (!accountNoToUserId.containsKey(accountNo)) {
            System.out.println("Account not found.");
            return;
        }

        String spl = "{CALL delete_Account(?)}";
        CallableStatement cst = con.prepareCall(spl);
        cst.setInt(1, accountNo);
        int r = cst.executeUpdate();
        if(r>0){
            System.out.println("Delete successful");
            // Remove from local data structures
            int userId = accountNoToUserId.get(accountNo);
            List<User> accounts = userAccounts.get(userId);
            if (accounts != null) {
                accounts.removeIf(acc -> acc.getAccountNo() == accountNo);
                if (accounts.isEmpty()) {
                    userAccounts.remove(userId);
                }
            }
            accountNoToUserId.remove(accountNo);
        } else {
            System.out.println("Delete unsuccessful");
        }
    }

    private static void saveAccountsToFile() {
        try (PrintWriter writer = new PrintWriter(("bank_data.txt"))) {

            // Header
            writer.println("=".repeat(50));
            writer.println("        BANK DATA EXPORT");
            writer.println("        " + LocalDate.now());
            writer.println("=".repeat(50));
            writer.println();

            // Users section
            writer.println("USERS");
            writer.println("-".repeat(50));

            for (User user : users.values()) {
                writer.println("USER ID: " + user.getUserId());
                writer.println("Name: " + user.getFirstName() + " " + user.getLastName());
                writer.println("DOB: " + user.getDob() + (isLeapYear(user.getDob()) ? " (Leap Year)" : ""));
                writer.println("Mobile: " + user.getMobileNumber());
                writer.println("Email: " + user.getEmail());
                writer.println("Aadhaar: " + user.getAadhaar());
                writer.println("PAN: " + user.getPan());
                writer.println("-".repeat(30));
            }
            writer.println();

            // Accounts section
            writer.println("ACCOUNTS");
            writer.println("-".repeat(50));

            for (Integer userId : userAccounts.keySet()) {
                User user = users.get(userId);
                List<User> accounts = userAccounts.get(userId);

                for (User account : accounts) {
                    writer.println("User ID: " + userId);
                    writer.println("Name: " + user.getFirstName() + " " + user.getLastName());
                    writer.println("Account No: " + account.getAccountNo());
                    writer.println("Account Type: " + account.getAccountType());
                    writer.println("Balance: ₹" + account.getBalance());
                    writer.println("MPIN: " + account.getMpin());
                    writer.println("Debit Card: " + (account.hasDebitCard() ? "Yes" : "No"));
                    writer.println("Credit Card: " + (account.hasCreditCard() ? "Yes" : "No"));
                    writer.println("Loan: " + (account.hasLoan() ? "Yes" : "No"));

                    // Schemes
                    if (!account.getSchemes().isEmpty()) {
                        writer.println("Schemes: " + String.join(", ", account.getSchemes()));
                    }

                    // Transactions (last 5)
                    if (!account.getTransactionHistory().isEmpty()) {
                        writer.println("Recent Transactions:");
                        List<String> recentTransactions = account.getTransactionHistory();
                        int start = Math.max(0, recentTransactions.size() - 5);
                        for (int i = start; i < recentTransactions.size(); i++) {
                            writer.println("  - " + recentTransactions.get(i));
                        }
                    }

                    // Mutual funds info for savings accounts
                    if ("Savings".equalsIgnoreCase(account.getAccountType())) {
                        writer.println("Mutual Funds: Coming Soon");
                    }

                    writer.println("-".repeat(30));
                }
            }

            // Summary
            writer.println();
            writer.println("SUMMARY");
            writer.println("-".repeat(50));
            writer.println("Total Users: " + users.size());

            int totalAccounts = 0;
            int totalDebitCards = 0;
            int totalCreditCards = 0;
            int totalLoans = 0;
            double totalBalance = 0;
            int savingsAccounts = 0;
            int currentAccounts = 0;
            int nriAccounts = 0;

            for (List<User> accounts : userAccounts.values()) {
                totalAccounts += accounts.size();
                for (User account : accounts) {
                    totalBalance += account.getBalance();
                    if (account.hasDebitCard()) totalDebitCards++;
                    if (account.hasCreditCard()) totalCreditCards++;
                    if (account.hasLoan()) totalLoans++;

                    // Count account types
                    if ("Savings".equalsIgnoreCase(account.getAccountType())) savingsAccounts++;
                    else if ("Current".equalsIgnoreCase(account.getAccountType())) currentAccounts++;
                    else if ("NRI".equalsIgnoreCase(account.getAccountType())) nriAccounts++;
                }
            }

            writer.println("Total Accounts: " + totalAccounts);
            writer.println("  - Savings Accounts: " + savingsAccounts);
            writer.println("  - Current Accounts: " + currentAccounts);
            writer.println("  - NRI Accounts: " + nriAccounts);
            writer.println("Total Balance: ₹" + totalBalance);
            writer.println("Debit Cards Issued: " + totalDebitCards);
            writer.println("Credit Cards Issued: " + totalCreditCards);
            writer.println("Loans Approved: " + totalLoans);
            writer.println("=".repeat(50));

            System.out.println("Data saved successfully to bank_data.txt");

        } catch (FileNotFoundException e) {
            System.out.println("Error saving to file: " + e.getMessage());
        }
    }
}