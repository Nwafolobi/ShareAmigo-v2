<?php
// get_handover.php - The receiver reopens their QR code and PIN for a pending exchange

require_once '../../config/auth.php';

$me = require_auth($pdo);

$transaction_id = isset($_GET['transaction_id']) ? (int)$_GET['transaction_id'] : 0;
if ($transaction_id <= 0) {
    json_fail(400, 'Missing transaction_id.');
}

$stmt = $pdo->prepare(
    "SELECT t.id, i.title AS item_title, t.credit_amount, t.qr_verify_hash, t.pin_fallback
     FROM transactions t JOIN items i ON i.id = t.item_id
     WHERE t.id = ? AND t.receiver_id = ? AND t.status = 'Pending'"
);
$stmt->execute([$transaction_id, $me['id']]);
$tx = $stmt->fetch();

if (!$tx) {
    json_fail(404, 'No pending exchange found.');
}

$tx['credit_amount'] = (int)$tx['credit_amount'];
echo json_encode(['success' => true, 'transaction' => $tx]);
