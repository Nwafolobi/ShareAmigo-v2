<?php
// delete_item_photo.php - Remove a photo from one of your own listings. JSON: { "photo_id": 12 }

require_once '../../config/auth.php';
require_once '../../config/validation.php';
require_once '../../config/uploads.php';

$me = require_auth($pdo);
require_method('POST');
$data = json_body();

$photo_id = int_field($data, 'photo_id', 'photo_id');

$stmt = $pdo->prepare(
    "SELECT p.id, p.item_id, p.photo_path, i.status
     FROM item_photos p JOIN items i ON i.id = p.item_id
     WHERE p.id = ? AND i.listed_by = ? AND i.status <> 'Deleted'"
);
$stmt->execute([$photo_id, $me['id']]);
$photo = $stmt->fetch();
if (!$photo) {
    json_fail(404, 'Photo not found.');
}
if ($photo['status'] !== 'Available') {
    json_fail(409, 'This listing has been claimed and can no longer be edited.');
}

$pdo->prepare("DELETE FROM item_photos WHERE id = ?")->execute([$photo_id]);
delete_uploaded_image($photo['photo_path']);
sync_item_cover($pdo, (int)$photo['item_id']);

echo json_encode(['success' => true, 'photos' => item_photos($pdo, (int)$photo['item_id'])]);
