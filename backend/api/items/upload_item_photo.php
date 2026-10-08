<?php
// upload_item_photo.php - Add a photo to one of your own listings.
// Multipart form: item_id, photo (JPG/PNG/WebP, max 5 MB). Up to 4 photos per listing.
// The first photo becomes the picture shown on the campus feed.

require_once '../../config/auth.php';
require_once '../../config/validation.php';
require_once '../../config/uploads.php';

$me = require_auth($pdo);
require_method('POST');

$item_id = int_field($_POST, 'item_id', 'item_id');
$item = load_own_item($pdo, $item_id, (int)$me['id']);
if ($item['status'] !== 'Available') {
    json_fail(409, 'This listing has been claimed and can no longer be edited.');
}

$count = $pdo->prepare("SELECT COUNT(*) FROM item_photos WHERE item_id = ?");
$count->execute([$item_id]);
$existing = (int)$count->fetchColumn();
if ($existing >= MAX_ITEM_PHOTOS) {
    json_fail(409, 'A listing can have at most ' . MAX_ITEM_PHOTOS . ' photos.');
}

$path = save_uploaded_image('photo', 'items');

$ins = $pdo->prepare("INSERT INTO item_photos (item_id, photo_path, sort_order) VALUES (?, ?, ?)");
$ins->execute([$item_id, $path, $existing]);
$photo_id = (int)$pdo->lastInsertId();

sync_item_cover($pdo, $item_id);

http_response_code(201);
echo json_encode([
    'success' => true,
    'photo' => ['id' => $photo_id, 'photo_url' => $path],
    'photos' => item_photos($pdo, $item_id),
]);
