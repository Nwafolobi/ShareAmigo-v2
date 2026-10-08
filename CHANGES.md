# Backend v2: what changed

## Security
- Login and register now return a `token`. Send it as `Authorization: Bearer <token>` on every other call.
- The server now uses the signed-in user. It ignores any `user_id`, `receiver_id` or `donor_id` sent by the app.
- Tokens last 7 days. Only a hash is stored. New `auth/logout.php` ends a session.
- QR hash and PIN are now random (before: `md5` and a fixed PIN `849201`).
- Wrong QR/PIN is limited to 5 tries per exchange.
- Database login moved to `config/env.php` (git-ignored). Copy `config/env.example.php` to create it.
- Removed `Access-Control-Allow-Origin: *`. Database error text is no longer sent to the app.
- Added checks: password length, valid email, condition list, text lengths, photo URL, same-campus claims.

## Database
- New table `auth_tokens`, new column `transactions.verify_attempts`.
- New install: run `schema.sql`. Existing database: run `migration_v2.sql` once.
- Seed users now have a working password: `Test@1234` (dev only).

## The Android app must change next
Until the app sends the token, every call except login and register returns 401.

# Android app v2 (step 2)
- Login and register save the token on the phone. Every call sends it. A 401 sends the student back to sign in.
- Added sign out (three dots menu on the home screen).
- Removed all "offline demo" shortcuts. Before, a failed login still opened the app as user 2, and a failed claim still showed success.
- Claim now opens the real QR and PIN from the server. The donor verifies by scanning the QR or typing the PIN (open it from a Pending row in the wallet).
- Server error messages now show in the app.
- Base URL now lives in `app/build.gradle` (debug = emulator, release = your live HTTPS address).
- Debug builds allow http. Release builds do not. `allowBackup` is off.
- Added the files Android Studio needs: settings.gradle, root build.gradle, gradle.properties, wrapper properties, proguard file, app icon.
- New backend file: `backend/api/transactions/get_handover.php`.

# Profile page and my listings (v3)

## What students can do now
- **My Profile** (person icon on the home screen): profile picture, name, email, student number, campus, member since, and a short bio.
- Edit name, student number and bio. Email can't be changed because it proves the student is enrolled.
- Change password (asks for the current one first).
- Upload, replace or remove a profile picture.
- Numbers at a glance: credits (tap to open the wallet), listings on the feed, items given, items received.
- **My listings**: every listing the student has made, filtered by All / On the feed / Claimed / Exchanged. Edit or delete from the ⋮ menu. Claimed and exchanged listings are locked.
- **Listings with photos**: add up to 4 photos from the phone's gallery when creating or editing a listing. At least one is required. The first photo is the cover on the feed. Photos are shrunk to about 1280 px before upload to save data.
- Item detail shows all photos (swipe through them). On your own listing the button becomes **Edit your listing**.
- The price slider now follows each category's limit (Textbooks 30, Stationery 20, Clothing 20, Food/Meals 15), so the server no longer rejects prices that are too high.

## Backend
New endpoints, all need `Authorization: Bearer <token>`:

| Endpoint | Method | What it does |
|---|---|---|
| `users/get_profile.php` | GET | Profile and stats |
| `users/update_profile.php` | POST JSON | `full_name`, `student_number`, `bio` |
| `users/change_password.php` | POST JSON | `current_password`, `new_password` |
| `users/upload_avatar.php` | POST multipart | `photo`, or `remove=1` |
| `items/get_my_items.php` | GET | My listings with photos, optional `?status=` |
| `items/update_item.php` | POST JSON | Edit an available listing |
| `items/delete_item.php` | POST JSON | `item_id`; marks it Deleted and removes its photos |
| `items/upload_item_photo.php` | POST multipart | `item_id`, `photo` (max 4 per listing, 5 MB, JPG/PNG/WebP) |
| `items/delete_item_photo.php` | POST JSON | `photo_id` |
| `items/get_item_photos.php` | GET | `?item_id=` |

- Photos are checked by their real file contents, saved with random names in `backend/uploads/`, and stored in the database as a path such as `uploads/items/abc.jpg`. The app turns that into a full address from its `BASE_URL`, so the same database works for the emulator and a real phone.
- `backend/uploads/.htaccess` stops code from running in the uploads folder. Uploaded photos are git-ignored.
- New shared helpers: `config/uploads.php`, `config/validation.php`.

## Setup
1. Run `backend/migration_v3.sql` once (adds `users.bio`, `users.avatar_url` and the `item_photos` table).
2. Make sure the web server can write to `backend/uploads/` (XAMPP on Windows: nothing to do).
3. Sync Gradle in Android Studio (new libraries: activity, swiperefreshlayout, viewpager2, exifinterface).
