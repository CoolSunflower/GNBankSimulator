// Import Bank Classes
import Bank.*;
import static Bank.Constants.NUM_BRANCHES;
import static Bank.Constants.NUM_UPDATERS_PER_BRANCH;

// Import UI Relevant Classes
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.Map;

// GUI Class for the Guwahati National Bank (GNB) simulation and interactive operations.
public class BankGUI extends JFrame {
    // Core objects: bank database and the updaters, and their threads
    private BankDatabase bank;
    private Updater[][] updaters;
    private Thread[][] updaterThreads;

    // GUI components for Simulation Tab
    private JTextField tfInitialAccounts; // Initial Accounts Text Field
    private JTextField tfTransactionsPerUpdater; // Num. transactions Text Field
    private JCheckBox chkSaveLogs; // Save Logs Check Box
    private JButton btnRunSimulation; // Start Simulation Button
    private JTextArea taSimulationLog; // Text Area for output

    // GUI components for User Operation Tab
    private JComboBox<Integer> cbBranch; // Branch Selector
    private JComboBox<Integer> cbUpdater; // Updater Selector
    private JTextField tfAccountNumber; // Account Number Text Field
    private JComboBox<String> cbOperation; // Operation Selector
    private JTextField tfAmount; // Used for deposit, withdrawal, add customer, transfer money
    private JTextField tfDestAccount; // For transfer money
    private JTextField tfDestBranch;  // For transfer customer account
    private JButton btnSubmitOperation; // Submit Operation Button
    private JTextArea taOperationResult; // Text Area for output

    // Main constructor for the GUI application
    public BankGUI() {
        super("Guwahati National Bank (GNB) Simulator"); // Title of GUI
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null); // Center window

        // Initialize core objects
        // Create the BankDatabase (which automatically creates branches and updaters)
        bank = new BankDatabase();
        updaters = new Updater[NUM_BRANCHES][NUM_UPDATERS_PER_BRANCH];
        updaterThreads = new Thread[NUM_BRANCHES][NUM_UPDATERS_PER_BRANCH];

        // Populate updaters and get the start their updaterThreads
        for (int i = 0; i < NUM_BRANCHES; i++) {
            for (int j = 0; j < NUM_UPDATERS_PER_BRANCH; j++) {
                updaters[i][j] = bank.getUpdater(i, j);
                updaterThreads[i][j] = new Thread(updaters[i][j], "Updater-" + i + "-" + j);
                updaterThreads[i][j].start();
            }
        }

        // Set up the tabbed pane for the GUI
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Simulation", createSimulationPanel());
        tabbedPane.addTab("User Operations", createOperationPanel());
        add(tabbedPane);
    }

    
    // Creates the panel for running the simulation
    private JPanel createSimulationPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Input options at the top
        JPanel inputPanel = new JPanel(new GridLayout(4, 3, 10, 10));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Intial Accounts Text Field
        inputPanel.add(new JLabel("Initial Accounts per Branch:"));
        tfInitialAccounts = new JTextField("1000");
        inputPanel.add(tfInitialAccounts);

        // Number of transactions Text Field
        inputPanel.add(new JLabel("Transactions per Updater:"));
        tfTransactionsPerUpdater = new JTextField("1000");
        inputPanel.add(tfTransactionsPerUpdater);

        // Save Logs Checkbox
        inputPanel.add(new JLabel("Save Logs:"));
        chkSaveLogs = new JCheckBox();
        chkSaveLogs.setSelected(false); // Default is "off"
        inputPanel.add(chkSaveLogs);

        // Run Simulation Button
        btnRunSimulation = new JButton("Run Simulation");
        inputPanel.add(btnRunSimulation);
        panel.add(inputPanel, BorderLayout.NORTH);

        // Log area in the center
        taSimulationLog = new JTextArea();
        taSimulationLog.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(taSimulationLog);
        panel.add(scrollPane, BorderLayout.CENTER);

        // When simulation button is clicked, run simulation is called in background.
        btnRunSimulation.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                runSimulation();
            }
        });

        return panel;
    }

    // Runs the simulation on a background thread (using SwingWorker)
    private void runSimulation() {
        // Read parameters from text fields
        int numInitialAccounts;
        int numTransactions;
        boolean saveLogs; 
        try {
            numInitialAccounts = Integer.parseInt(tfInitialAccounts.getText().trim());
            numTransactions = Integer.parseInt(tfTransactionsPerUpdater.getText().trim());
            saveLogs = chkSaveLogs.isSelected();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid input parameters.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Log start of simulation
        taSimulationLog.append("Starting simulation with Initial Accounts = " + numInitialAccounts + ", Number of Transactions/Updater = " + numTransactions + "\n");

        // Create a Simulator object with the desired parameters
        Simulator simulator = new Simulator(bank, updaters, updaterThreads, numInitialAccounts, numTransactions, saveLogs);

        // Run simulation in a SwingWorker to avoid freezing the GUI
        SwingWorker<Map.Entry<String, String>, Void> worker = new SwingWorker<Map.Entry<String, String>, Void>() {
            @Override
            protected Map.Entry<String, String> doInBackground() throws Exception {
                // This method runs on a background thread.
                simulator.initializeAccounts(); // initialize accounts as per user input
                Map.Entry<String, String> ret = simulator.simulate(); // start simulation
                return ret; // return execution time for printing
            }

            @Override
            protected void done() {
                try {
                    Map.Entry<String, String> ret = get();
                    taSimulationLog.append("Simulation completed in " + ret.getKey() + " ms.\n");
                    taSimulationLog.append("Simulation Metrics: \n" + ret.getValue() + "\n");
                    if(saveLogs){
                        taSimulationLog.append("Output saved in output.txt\n");
                    }
                } catch (InterruptedException | ExecutionException ex) {
                    taSimulationLog.append("Simulation encountered an error: " + ex.getMessage() + "\n");
                }
            }
        };

        worker.execute();
    }


    // Creates the panel for interactive user operations
    private JPanel createOperationPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Operation input panel at the top.
        JPanel opPanel = new JPanel(new GridLayout(8, 2, 5, 5));
        opPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        opPanel.add(new JLabel("Select Branch:"));
        cbBranch = new JComboBox<>();
        for (int i = 0; i < NUM_BRANCHES; i++) {
            cbBranch.addItem(i);
        }
        opPanel.add(cbBranch);

        opPanel.add(new JLabel("Select Updater:"));
        cbUpdater = new JComboBox<>();
        // Updater indices 0 .. (NUM_UPDATERS_PER_BRANCH - 1)
        for (int j = 0; j < NUM_UPDATERS_PER_BRANCH; j++) {
            cbUpdater.addItem(j);
        }
        opPanel.add(cbUpdater);

        opPanel.add(new JLabel("Account Number:"));
        tfAccountNumber = new JTextField();
        opPanel.add(tfAccountNumber);

        opPanel.add(new JLabel("Operation:"));
        cbOperation = new JComboBox<>(new String[]{
            "BALANCE_CHECK",
            "DEPOSIT",
            "WITHDRAWAL",
            "TRANSFER_MONEY",
            "ADD_CUSTOMER",
            "DELETE_CUSTOMER",
            "TRANSFER_CUSTOMER_ACCOUNT"
        });
        opPanel.add(cbOperation);

        opPanel.add(new JLabel("Amount (if applicable):"));
        tfAmount = new JTextField();
        opPanel.add(tfAmount);

        opPanel.add(new JLabel("Destination Account (for transfer):"));
        tfDestAccount = new JTextField();
        opPanel.add(tfDestAccount);

        opPanel.add(new JLabel("Destination Branch (for account transfer):"));
        tfDestBranch = new JTextField();
        opPanel.add(tfDestBranch);

        btnSubmitOperation = new JButton("Submit Operation");
        opPanel.add(btnSubmitOperation);

        panel.add(opPanel, BorderLayout.NORTH);

        // Operation result area at the bottom
        taOperationResult = new JTextArea(10, 40);
        taOperationResult.setEditable(false);
        panel.add(new JScrollPane(taOperationResult), BorderLayout.CENTER);

        // Listener for operation submission
        btnSubmitOperation.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performUserOperation();
            }
        });

        return panel;
    }

    // Performs a user operation by reading the input fields and calling the appropriate BankDatabase method.
    private void performUserOperation() {
        int branchId;
        int updaterIndex;
        String op;
        String accNum;
        String amountStr;
        String destAcc;
        String destBranchStr;

        try{
            branchId = (Integer) cbBranch.getSelectedItem();
            updaterIndex = (Integer) cbUpdater.getSelectedItem();
            op = (String) cbOperation.getSelectedItem();
            accNum = tfAccountNumber.getText().trim();
            amountStr = tfAmount.getText().trim();
            destAcc = tfDestAccount.getText().trim();
            destBranchStr = tfDestBranch.getText().trim();    
        }catch(Exception e){
            JOptionPane.showMessageDialog(this, "Invalid input parameters.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        double amount = 0;
        if (!amountStr.isEmpty()) {
            try {
                amount = Double.parseDouble(amountStr);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid amount value", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        // Create a CompletableFuture to receive the response
        CompletableFuture<String> responseFuture = new CompletableFuture<>();
        Query query = null;        

        // Create Query using appropriate constructor
        try {
            switch(op) {
                case "BALANCE_CHECK":
                    query = new Query(Query.QueryType.BALANCE_CHECK, accNum, responseFuture);
                    break;
                case "DEPOSIT":
                    query = new Query(Query.QueryType.DEPOSIT, accNum, amount, responseFuture);
                    break;
                case "WITHDRAWAL":
                    query = new Query(Query.QueryType.WITHDRAWAL, accNum, amount, responseFuture);
                    break;
                case "TRANSFER_MONEY":
                    query = new Query(Query.QueryType.TRANSFER_MONEY, accNum, destAcc, amount, responseFuture);
                    break;
                case "ADD_CUSTOMER":
                    query = new Query(Query.QueryType.ADD_CUSTOMER, "", amount, responseFuture);
                    break;
                case "DELETE_CUSTOMER":
                    query = new Query(Query.QueryType.DELETE_CUSTOMER, accNum, responseFuture);
                    break;
                case "TRANSFER_CUSTOMER_ACCOUNT":
                    int destBranch = Integer.parseInt(destBranchStr);
                    query = new Query(Query.QueryType.TRANSFER_CUSTOMER_ACCOUNT, accNum, destBranch, responseFuture);
                    break;
                default:
                    taOperationResult.append("Unknown operation selected.\n");
                    return;
            }
        } catch(Exception ex) {
            taOperationResult.append("Error creating query: " + ex.getMessage() + "\n");
            return;
        }

        // Submit the query to the chosen updater.
        updaters[branchId][updaterIndex].submitQuery(query);

        // Wait for the response (with a timeout) and display the result
        responseFuture.whenComplete((result, error) -> {
            SwingUtilities.invokeLater(() -> {
                if (error != null) {
                    taOperationResult.append("Error: " + error.getMessage() + "\n");
                } else {
                    taOperationResult.append(result + "\n");
                }
            });
        });
    }

    public static void main(String[] args) {
        // Redirect System.out and System.err to a file
        try {
            PrintStream fileOut = new PrintStream(new FileOutputStream("output.txt"));
            System.setOut(fileOut);
            System.setErr(fileOut);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // Change UI design
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            UIManager.put("Label.font", new Font("SansSerif", Font.PLAIN, 14));
            UIManager.put("Button.font", new Font("SansSerif", Font.BOLD, 14));
            UIManager.put("ComboBox.font", new Font("SansSerif", Font.PLAIN, 14));
            UIManager.put("TextField.font", new Font("SansSerif", Font.PLAIN, 14));
            UIManager.put("TextArea.font", new Font("Monospaced", Font.PLAIN, 13));
            UIManager.put("TitledBorder.font", new Font("SansSerif", Font.BOLD, 14));
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // Start GUI
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                BankGUI gui = new BankGUI();
                gui.setVisible(true);
            }
        });
    }
}
