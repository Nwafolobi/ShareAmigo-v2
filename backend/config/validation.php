<?php
// validation.php - Small helpers shared by the profile and listing endpoints.

const ITEM_CONDITIONS = ['Brand New', 'Like New', 'Good', 'Fair'];

function require_method(string $method): void {
    if ($_SERVER['REQUEST_METHOD'] !== $method) {
        json_fail(405, 'Method not allowed.');
    }
}

function json_body(): array {
    $data = json_decode(file_get_contents('php://input'), true);
    if (!is_array($data)) {
        json_fail(400, 'Invalid request body.');
    }
    return $data;
}

// Returns a trimmed string, or stops with an error if it is missing or the wrong length.
function text_field(array $data, string $key, string $label, int $min, int $max): string {
    $value = isset($data[$key]) && is_string($data[$key]) ? trim($data[$key]) : '';
    $len = function_exists('mb_strlen') ? mb_strlen($value) : strlen($value);
    if ($len < $min) {
        json_fail(400, $min <= 1 ? "$label is required." : "$label must be at least $min characters.");
    }
    if ($len > $max) {
        json_fail(400, "$label must be $max characters or fewer.");
    }
    return $value;
}

function int_field(array $data, string $key, string $label): int {
    if (!isset($data[$key]) || !is_numeric($data[$key])) {
        json_fail(400, "Missing $label.");
    }
    return (int)$data[$key];
}

// Loads one of the signed-in student's own items, or stops with 404.
function load_own_item(PDO $pdo, int $item_id, int $user_id): array {
    $stmt = $pdo->prepare("SELECT id, listed_by, status FROM items WHERE id = ? AND listed_by = ? AND status <> 'Deleted'");
    $stmt->execute([$item_id, $user_id]);
    $item = $stmt->fetch();
    if (!$item) {
        json_fail(404, 'Listing not found.');
    }
    return $item;
}
