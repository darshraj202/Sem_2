import java.time.LocalDate;
import java.util.*;
// User Class
class User {
    String firstName;
    String lastName;
    LocalDate dob;
    String mobileNumber;
    String email;
    String aadhaar;
    String pan;
    String accountType;
    String password;
    double balance;
    int accountNo;
    int userId;
    String mpin;
    List<String> transactionHistory;
    boolean hasCreditCard;
    boolean hasDebitCard;
    boolean hasLoan;
    List<String> schemes;

    User(int userId, int accountNo, String firstName, String lastName, LocalDate dob, double balance,
         String mobileNumber, String email, String aadhaar, String pan, String accountType, String password, String mpin) {
        this.userId = userId;
        this.accountNo = accountNo;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dob = dob;
        this.mobileNumber = mobileNumber;
        this.email = email;
        this.aadhaar = aadhaar;
        this.pan = pan;
        this.accountType = accountType;
        this.password = password;
        this.mpin = mpin;
        this.balance = balance;
        this.transactionHistory = new ArrayList<>();
        this.hasCreditCard = false;
        this.hasDebitCard = false;
        this.hasLoan = false;
        this.schemes = new ArrayList<>();
    }

    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public LocalDate getDob() { return dob; }
    public String getMobileNumber() { return mobileNumber; }
    public String getEmail() { return email; }
    public String getAadhaar() { return aadhaar; }
    public String getPan() { return pan; }
    public String getAccountType() { return accountType; }
    public String getPassword() { return password; }
    public double getBalance() { return balance; }
    public int getAccountNo() { return accountNo; }
    public int getUserId() { return userId; }
    public String getMpin() { return mpin; }
    public List<String> getTransactionHistory() { return transactionHistory; }
    public boolean hasCreditCard() { return hasCreditCard; }
    public boolean hasDebitCard() { return hasDebitCard; }
    public boolean hasLoan() { return hasLoan; }
    public List<String> getSchemes() { return schemes; }


    public void setMpin(String mpin) {this.mpin = mpin;}
    public void setBalance(double balance) { this.balance = balance; }
    public void setHasCreditCard(boolean hasCreditCard) { this.hasCreditCard = hasCreditCard; }
    public void setHasDebitCard(boolean hasDebitCard) { this.hasDebitCard = hasDebitCard; }
    public void setHasLoan(boolean hasLoan) { this.hasLoan = hasLoan; }

    // Transaction methods
    public void addTransaction(String transaction) {
        transactionHistory.add(transaction);
        if (transactionHistory.size() > 10) {
            transactionHistory.removeFirst();
        }
    }

    public void addScheme(String scheme) {
        schemes.add(scheme);
    }

    // Method to get user details for file output
    public String getUserDetailsForFile() {
        StringBuilder details = new StringBuilder();
        details.append("User ID: ").append(userId).append("\n");
        details.append("Name: ").append(firstName).append(" ").append(lastName).append("\n");
        details.append("DOB: ").append(dob).append("\n");
        details.append("Mobile: ").append(mobileNumber).append("\n");
        details.append("Email: ").append(email).append("\n");
        details.append("Aadhaar: ").append(aadhaar).append("\n");
        details.append("PAN: ").append(pan).append("\n");
        details.append("Account No: ").append(accountNo).append("\n");
        details.append("Account Type: ").append(accountType).append("\n");
        details.append("Balance: ₹").append(balance).append("\n");
        details.append("Debit Card: ").append(hasDebitCard ? "Yes" : "No").append("\n");
        details.append("Credit Card: ").append(hasCreditCard ? "Yes" : "No").append("\n");
        details.append("Loan: ").append(hasLoan ? "Yes" : "No").append("\n");
        details.append("Schemes: ").append(String.join(", ", schemes)).append("\n");
        details.append("Transactions:\n");
        for (String transaction : transactionHistory) {
            details.append("- ").append(transaction).append("\n");
        }
        details.append("---------------------\n");

        return details.toString();
    }
}
