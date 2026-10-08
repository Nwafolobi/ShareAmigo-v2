<?php
// get_profile.php - The signed-in student's profile and activity numbers

require_once '../../config/auth.php';
require_once '../../config/validation.php';

$me = require_auth($pdo);
require_method('GET');

$stmt = $pdo->prepare(
    "SELECT u.id, u.campus_id, c.name AS campus_name, u.full_name, u.email, u.student_number,
            u.bio, u.avatar_url, u.credit_balance, u.role, u.created_at
     FROM users u JOIN campuses c ON c.id = u.campus_id
     WHERE u.id = ?"
);
$stmt->execute([$me['id']]);
$user = $stmt->fetch();
if (!$user) {
    json_fail(404, 'Account not found.');
}

$user['id'] = (int)$user['id'];
$user['campus_id'] = (int)$user['campus_id'];
$user['credit_balance'] = (int)$user['credit_balance'];

$counts = $pdo->prepare(
    "SELECT
        SUM(CASE WHEN status = 'Available' THEN 1 ELSE 0 END) AS active,
        SUM(CASE WHEN status = 'Escrow'    THEN 1 ELSE 0 END) AS in_escrow,
        SUM(CASE WHEN status = 'Exchanged' THEN 1 ELSE 0 END) AS exchanged
     FROM items WHERE listed_by = ? AND status <> 'Deleted'"
);
$counts->execute([$me['id']]);
$c = $counts->fetch();

$given = $pdo->prepare("SELECT COUNT(*) FROM transactions WHERE donor_id = ? AND status = 'Completed'");
$given->execute([$me['id']]);
$received = $pdo->prepare("SELECT COUNT(*) FROM transactions WHERE receiver_id = ? AND status = 'Completed'");
$received->execute([$me['id']]);

echo json_encode([
    'success' => true,
    'user' => $user,
    'stats' => [
        'active_listings'   => (int)($c['active'] ?? 0),
        'in_escrow'         => (int)($c['in_escrow'] ?? 0),
        'exchanged'         => (int)($c['exchanged'] ?? 0),
        'items_given'       => (int)$given->fetchColumn(),
        'items_received'    => (int)$received->fetchColumn(),
    ],
]);
