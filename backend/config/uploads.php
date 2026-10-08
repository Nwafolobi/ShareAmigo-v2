<?php
// uploads.php - Saves student photos (listing pictures and profile pictures) safely.
//
// Files go to backend/uploads/<folder>/ with a random name. The database stores the
// path relative to the backend folder, for example "uploads/items/3f9a....jpg".
// The app turns that into a full address using its BASE_URL.

const UPLOAD_MAX_BYTES = 5 * 1024 * 1024;   // 5 MB per photo (the app compresses to well under this)
const UPLOAD_MAX_SIDE  = 6000;              // reject absurdly large images
const MAX_ITEM_PHOTOS  = 4;

function uploads_root(): string {
    return dirname(__DIR__) . '/uploads';
}

// Checks the uploaded file in $_FILES[$field] and saves it. Returns the relative path.
// Stops the request with a clear message if anything is wrong.
function save_uploaded_image(string $field, string $folder): string {
    if (!isset($_FILES[$field]) || !is_array($_FILES[$field])) {
        json_fail(400, 'No photo was sent.');
    }
    $file = $_FILES[$field];

    if ($file['error'] === UPLOAD_ERR_INI_SIZE || $file['error'] === UPLOAD_ERR_FORM_SIZE) {
        json_fail(413, 'Photo is too large. The limit is 5 MB.');
    }
    if ($file['error'] !== UPLOAD_ERR_OK || !is_uploaded_file($file['tmp_name'])) {
        json_fail(400, 'The photo did not upload. Please try again.');
    }
    if ($file['size'] <= 0 || $file['size'] > UPLOAD_MAX_BYTES) {
        json_fail(413, 'Photo is too large. The limit is 5 MB.');
    }

    // Trust the file contents, never the name or the type the phone claims.
    $allowed = ['image/jpeg' => 'jpg', 'image/png' => 'png', 'image/webp' => 'webp'];
    $finfo = new finfo(FILEINFO_MIME_TYPE);
    $mime = $finfo->file($file['tmp_name']);
    if (!isset($allowed[$mime])) {
        json_fail(415, 'Only JPG, PNG or WebP photos are allowed.');
    }

    $size = @getimagesize($file['tmp_name']);
    if ($size === false || $size[0] < 1 || $size[1] < 1
        || $size[0] > UPLOAD_MAX_SIDE || $size[1] > UPLOAD_MAX_SIDE) {
        json_fail(415, 'That file is not a usable photo.');
    }

    $dir = uploads_root() . '/' . $folder;
    if (!is_dir($dir) && !mkdir($dir, 0755, true)) {
        json_fail(500, 'Could not save the photo.');
    }

    $name = bin2hex(random_bytes(16)) . '.' . $allowed[$mime];
    if (!move_uploaded_file($file['tmp_name'], $dir . '/' . $name)) {
        json_fail(500, 'Could not save the photo.');
    }
    @chmod($dir . '/' . $name, 0644);

    return 'uploads/' . $folder . '/' . $name;
}

// Deletes a file saved by save_uploaded_image. Ignores anything outside the uploads folder
// (for example the old Unsplash seed addresses).
function delete_uploaded_image(?string $path): void {
    if ($path === null || strpos($path, 'uploads/') !== 0 || strpos($path, '..') !== false) {
        return;
    }
    $full = dirname(__DIR__) . '/' . $path;
    if (is_file($full)) {
        @unlink($full);
    }
}

// The feed shows items.photo_url, so keep it pointing at the item's first photo.
function sync_item_cover(PDO $pdo, int $item_id): void {
    $stmt = $pdo->prepare("SELECT photo_path FROM item_photos WHERE item_id = ? ORDER BY sort_order, id LIMIT 1");
    $stmt->execute([$item_id]);
    $first = $stmt->fetchColumn();
    $upd = $pdo->prepare("UPDATE items SET photo_url = ? WHERE id = ?");
    $upd->execute([$first === false ? null : $first, $item_id]);
}

function item_photos(PDO $pdo, int $item_id): array {
    $stmt = $pdo->prepare("SELECT id, photo_path AS photo_url FROM item_photos WHERE item_id = ? ORDER BY sort_order, id");
    $stmt->execute([$item_id]);
    $rows = $stmt->fetchAll();
    foreach ($rows as &$r) {
        $r['id'] = (int)$r['id'];
    }
    return $rows;
}
