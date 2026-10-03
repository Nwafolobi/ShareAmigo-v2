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
