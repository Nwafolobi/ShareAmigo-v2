<?php
// upload_avatar.php - Set or replace the profile picture (multipart form, field "photo").
// Send remove=1 with no photo to remove the current picture.

require_once '../../config/auth.php';
require_once '../../config/validation.php';
require_once '../../config/uploads.php';

$me = require_auth($pdo);
require_method('POST');

$stmt = $pdo->prepare("SELECT avatar_url FROM users WHERE id = ?");
$stmt->execute([$me['id']]);
$old = $stmt->fetchColumn();

$new = null;
if (empty($_POST['remove'])) {
    $new = save_uploaded_image('photo', 'avatars');
}

$upd = $pdo->prepare("UPDATE users SET avatar_url = ? WHERE id = ?");
$upd->execute([$new, $me['id']]);

if ($old !== false) {
    delete_uploaded_image($old);
}

echo json_encode([
    'success' => true,
    'message' => $new === null ? 'Profile picture removed.' : 'Profile picture updated.',
    'avatar_url' => $new,
]);
