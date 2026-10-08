<?php
// change_password.php - Change password after confirming the current one

require_once '../../config/auth.php';
require_once '../../config/validation.php';

$me = require_auth($pdo);
require_method('POST');
$data = json_body();

$current = isset($data['current_password']) && is_string($data['current_password']) ? $data['current_password'] : '';
$new = isset($data['new_password']) && is_string($data['new_password']) ? $data['new_password'] : '';

if ($current === '' || $new === '') {
    json_fail(400, 'Enter your current and new password.');
}
if (strlen($new) < 8) {
    json_fail(400, 'New password must be at least 8 characters.');
}
if (strlen($new) > 72) {
    json_fail(400, 'New password must be 72 characters or fewer.');
}

$stmt = $pdo->prepare("SELECT password_hash FROM users WHERE id = ?");
$stmt->execute([$me['id']]);
$hash = $stmt->fetchColumn();

if ($hash === false || !password_verify($current, $hash)) {
    json_fail(403, 'Current password is incorrect.');
}
if (password_verify($new, $hash)) {
    json_fail(400, 'New password must be different from the current one.');
}

$upd = $pdo->prepare("UPDATE users SET password_hash = ? WHERE id = ?");
$upd->execute([password_hash($new, PASSWORD_BCRYPT, ['cost' => 12]), $me['id']]);

echo json_encode(['success' => true, 'message' => 'Password changed.']);
