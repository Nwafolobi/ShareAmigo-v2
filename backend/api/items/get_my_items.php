<?php
// get_my_items.php - All of the signed-in student's own listings (any status except deleted),
// newest first, each with its photos. Optional ?status=Available|Escrow|Exchanged

require_once '../../config/auth.php';
require_once '../../config/validation.php';
require_once '../../config/uploads.php';

$me = require_auth($pdo);
require_method('GET');

$sql = "SELECT i.id, i.listed_by, u.full_name AS donor_name, i.campus_id, c.name AS category_name,
               i.title, i.description, i.condition_status, i.credit_cost, i.status, i.photo_url, i.created_at
        FROM items i
        JOIN users u ON u.id = i.listed_by
        JOIN categories c ON c.id = i.category_id
        WHERE i.listed_by = ? AND i.status <> 'Deleted'";
$params = [$me['id']];

$status = isset($_GET['status']) ? trim($_GET['status']) : '';
if (in_array($status, ['Available', 'Escrow', 'Exchanged'], true)) {
    $sql .= " AND i.status = ?";
    $params[] = $status;
}
$sql .= " ORDER BY i.created_at DESC, i.id DESC";

$stmt = $pdo->prepare($sql);
$stmt->execute($params);
$items = $stmt->fetchAll();

foreach ($items as &$item) {
    $item['id'] = (int)$item['id'];
    $item['listed_by'] = (int)$item['listed_by'];
    $item['campus_id'] = (int)$item['campus_id'];
    $item['credit_cost'] = (int)$item['credit_cost'];
    $item['photos'] = item_photos($pdo, $item['id']);
}
unset($item);

echo json_encode(['success' => true, 'count' => count($items), 'items' => $items]);
