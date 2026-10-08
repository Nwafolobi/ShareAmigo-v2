<?php
// delete_item.php - Remove one of your own listings from the feed. The row is kept as
// 'Deleted' because past transactions point to it; its photos are removed.

require_once '../../config/auth.php';
require_once '../../config/validation.php';
require_once '../../config/uploads.php';

$me = require_auth($pdo);
require_method('POST');
$data = json_body();

$item_id = int_field($data, 'item_id', 'item_id');
$item = load_own_item($pdo, $item_id, (int)$me['id']);
if ($item['status'] !== 'Available') {
    json_fail(409, 'This listing is part of an exchange and cannot be deleted.');
}

$photos = item_photos($pdo, $item_id);

$pdo->beginTransaction();
$stmt = $pdo->prepare("UPDATE items SET status = 'Deleted', photo_url = NULL WHERE id = ? AND listed_by = ? AND status = 'Available'");
$stmt->execute([$item_id, $me['id']]);
if ($stmt->rowCount() !== 1) {
    $pdo->rollBack();
    json_fail(409, 'This listing changed while you were deleting it. Please refresh.');
}
$pdo->prepare("DELETE FROM item_photos WHERE item_id = ?")->execute([$item_id]);
$pdo->commit();

foreach ($photos as $p) {
    delete_uploaded_image($p['photo_url']);
}

echo json_encode(['success' => true, 'message' => 'Listing deleted.']);
