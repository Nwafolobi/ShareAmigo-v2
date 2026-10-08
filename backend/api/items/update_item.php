<?php
// update_item.php - Edit one of your own listings. Only listings still on the feed
// (status Available) can be changed, so nobody edits an item someone already claimed.

require_once '../../config/auth.php';
require_once '../../config/validation.php';

$me = require_auth($pdo);
require_method('POST');
$data = json_body();

$item_id = int_field($data, 'item_id', 'item_id');
$item = load_own_item($pdo, $item_id, (int)$me['id']);
if ($item['status'] !== 'Available') {
    json_fail(409, 'This listing has been claimed and can no longer be edited.');
}

$title = text_field($data, 'title', 'Title', 3, 150);
$description = text_field($data, 'description', 'Description', 0, 2000);
$category_name = text_field($data, 'category_name', 'Category', 1, 50);
$condition = isset($data['condition_status']) ? $data['condition_status'] : '';
if (!in_array($condition, ITEM_CONDITIONS, true)) {
    json_fail(400, 'Choose a valid condition.');
}
$credit_cost = int_field($data, 'credit_cost', 'credit_cost');

$cat = $pdo->prepare("SELECT id, max_credit_limit FROM categories WHERE name = ?");
$cat->execute([$category_name]);
$category = $cat->fetch();
if (!$category) {
    json_fail(400, 'Invalid category selected.');
}
if ($credit_cost < 1 || $credit_cost > (int)$category['max_credit_limit']) {
    json_fail(400, "Credit cost must be between 1 and {$category['max_credit_limit']} for {$category_name}.");
}

$stmt = $pdo->prepare(
    "UPDATE items SET title = ?, description = ?, category_id = ?, condition_status = ?, credit_cost = ?
     WHERE id = ? AND listed_by = ? AND status = 'Available'"
);
$stmt->execute([$title, $description, $category['id'], $condition, $credit_cost, $item_id, $me['id']]);

echo json_encode(['success' => true, 'message' => 'Listing updated.', 'item_id' => $item_id]);
