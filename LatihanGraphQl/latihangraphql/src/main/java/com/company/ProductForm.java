package com.company;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ProductForm extends JFrame {
    private JTextField tfName = new JTextField();
    private JTextField tfPrice = new JTextField();
    private JTextField tfCategory = new JTextField();
    private JTable table;
    private DefaultTableModel tableModel;
    private String selectedProductId = null;

    public ProductForm() {
        setTitle("GraphQL Product Form");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Form field panel
        JPanel fieldPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        fieldPanel.add(new JLabel("Name"));
        fieldPanel.add(tfName);
        fieldPanel.add(new JLabel("Price"));
        fieldPanel.add(tfPrice);
        fieldPanel.add(new JLabel("Category"));
        fieldPanel.add(tfCategory);

        // Button panel
        JButton btnAdd = new JButton("Add");
        JButton btnEdit = new JButton("Edit");
        JButton btnDelete = new JButton("Delete");
        JButton btnFetch = new JButton("Show All");

        JPanel buttonPanel = new JPanel(new GridLayout(1, 4, 5, 5));
        buttonPanel.add(btnAdd);
        buttonPanel.add(btnEdit);
        buttonPanel.add(btnDelete);
        buttonPanel.add(btnFetch);

        JPanel inputPanel = new JPanel(new BorderLayout(10, 10));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        inputPanel.add(fieldPanel, BorderLayout.NORTH);
        inputPanel.add(buttonPanel, BorderLayout.CENTER);

        add(inputPanel, BorderLayout.NORTH);

        // Table
        tableModel = new DefaultTableModel(new String[]{"ID", "Name", "Price", "Category"}, 0);
        table = new JTable(tableModel);
        JScrollPane tableScroll = new JScrollPane(table);
        add(tableScroll, BorderLayout.CENTER);

        // Actions
        btnAdd.addActionListener(e -> tambahProduk());
        btnEdit.addActionListener(e -> updateProduk());
        btnDelete.addActionListener(e -> hapusProduk());
        btnFetch.addActionListener(e -> ambilSemuaProduk());

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() != -1) {
                int row = table.getSelectedRow();
                selectedProductId = table.getValueAt(row, 0).toString();
                tfName.setText(table.getValueAt(row, 1).toString());
                tfPrice.setText(table.getValueAt(row, 2).toString());
                tfCategory.setText(table.getValueAt(row, 3).toString());
            }
        });

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void tambahProduk() {
        try {
            String query = String.format(
                "mutation { addProduct (name: \"%s\", price: %s, category: \"%s\") { id name }}",
                tfName.getText(), tfPrice.getText(), tfCategory.getText()
            );
            String jsonRequest = new Gson().toJson(new GraphQLQuery(query));
            sendGraphQLRequest(jsonRequest);
            ambilSemuaProduk();
            clearForm();
        } catch (Exception e) {
            showError(e);
        }
    }

    private void updateProduk() {
        if (selectedProductId == null) return;
        try {
            String query = String.format(
                "mutation { updateProduct(id: %s, name: \"%s\", price: %s, category: \"%s\") { id name price category} }",
                selectedProductId, tfName.getText(), tfPrice.getText(), tfCategory.getText()
            );
            String jsonRequest = new Gson().toJson(new GraphQLQuery(query));
            sendGraphQLRequest(jsonRequest);
            ambilSemuaProduk();
            clearForm();
        } catch (Exception e) {
            showError(e);
        }
    }

    private void hapusProduk() {
        if (selectedProductId == null) return;
        try {
            String query = String.format("mutation { deleteProduct(id: %s) }", selectedProductId);
            String jsonRequest = new Gson().toJson(new GraphQLQuery(query));
            sendGraphQLRequest(jsonRequest);
            ambilSemuaProduk();
            clearForm();
        } catch (Exception e) {
            showError(e);
        }
    }

    private void ambilSemuaProduk() {
        try {
            String query = "query { allProducts { id name price category } }";
            String jsonRequest = new Gson().toJson(new GraphQLQuery(query));
            String response = sendGraphQLRequest(jsonRequest);
            updateTable(response);
        } catch (Exception e) {
            showError(e);
        }
    }

    private void updateTable(String json) {
        tableModel.setRowCount(0);
        JsonObject data = JsonParser.parseString(json).getAsJsonObject();
        JsonArray products = data.getAsJsonObject("data").getAsJsonArray("allProducts");
        for (JsonElement elem : products) {
            JsonObject obj = elem.getAsJsonObject();
            Object[] row = {
                obj.get("id").getAsString(),
                obj.get("name").getAsString(),
                obj.get("price").getAsString(),
                obj.get("category").getAsString()
            };
            tableModel.addRow(row);
        }
    }

    private void clearForm() {
        tfName.setText("");
        tfPrice.setText("");
        tfCategory.setText("");
        selectedProductId = null;
        table.clearSelection();
    }

    private String sendGraphQLRequest(String json) throws Exception {
        URL url = new URL("http://localhost:4567/graphql");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes());
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line).append("\n");
            return sb.toString();
        }
    }

    private void showError(Exception e) {
        JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ProductForm::new);
    }

    class GraphQLQuery {
        String query;
        GraphQLQuery(String query) {
            this.query = query;
        }
    }
}
