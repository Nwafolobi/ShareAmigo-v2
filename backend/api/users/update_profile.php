<?php
// update_profile.php - Change name, student number and bio. Email stays fixed because it
// proves the student belongs to the university.

require_once '../../config/auth.php';
require_once '../../config/validation.php';

$me = require_auth($pdo);
require_method('POST');
$data = json_body();

$full_name = text_field($data, 'full_name', 'Full name', 2, 100);
$student_number = text_field($data, 'student_number', 'Student number', 3, 50);
$bio = isset($data['bio']) && is_string($data['bio']) ? trim($data['bio']) : '';
if ((function_exists('mb_strlen') ? mb_strlen($bio) : strlen($bio)) > 300) {
    json_fail(400, 'Bio must be 300 characters or fewer.');
}

$stmt = $pdo->prepare("UPDATE users SET full_name = ?, student_number = ?, bio = ? WHERE id = ?");
$stmt->execute([$full_name, $student_number, $bio === '' ? null : $bio, $me['id']]);

echo json_encode([
    'success' => true,
    'message' => 'Profile updated.',
    'user' => [
        'id' => (int)$me['id'],
        'full_name' => $full_name,
        'student_number' => $student_number,
        'bio' => $bio === '' ? null : $bio,
    ],
]);
