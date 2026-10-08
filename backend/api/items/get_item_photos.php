<?php
// get_item_photos.php - All photos for a listing, used by the item detail gallery. ?item_id=5

require_once '../../config/auth.php';
require_once '../../config/validation.php';
require_once '../../config/uploads.php';

$me = require_auth($pdo);
require_method('GET');

$item_id = isset($_GET['item_id']) ? (int)$_GET['item_id'] : 0;
if ($item_id <= 0) {
    json_fail(400, 'Missing item_id.');
}

$stmt = $pdo->prepare("SELECT id FROM items WHERE id = ? AND status <> 'Deleted'");
$stmt->execute([$item_id]);
if (!$stmt->fetch()) {
    json_fail(404, 'Listing not found.');
}

echo json_encode(['success' => true, 'photos' => item_photos($pdo, $item_id)]);
