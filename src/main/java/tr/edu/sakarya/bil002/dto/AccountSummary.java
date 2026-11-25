package tr.edu.sakarya.bil002.dto;

/**
 * GET /api/accounts/{accountNumber}/summary yanıtı (TDD ile eklenen özellik).
 */
public record AccountSummary(String accountNumber, double totalDeposited, double totalWithdrawn,
                             int transactionCount) {
}
