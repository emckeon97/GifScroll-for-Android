# GifScroll for Android

The Android port of GifScroll — a comedy meme app. Swipe through an endless feed of funny memes, GIFs, and videos, react with a laugh, talk trash in the comments, and upload your own memes for the community.

Native Kotlin + Jetpack Compose (Material 3).

## What it does

- **Feed** — full-screen, TikTok-style swipe feed of memes, GIFs, and videos, powered by Reddit's meme communities (no API key needed). Videos play inline via ExoPlayer with tap-to-mute. The feed learns your taste: laugh-reacting teaches it which keywords you like, so similar ones surface first. Broken links and dead images are filtered out automatically before they reach you, and feed failures show a retry screen instead of a blank page.
- **Upload** — community meme feed. Pick a photo, add a caption, post it.
- **Profile** — your personal page: an Uploads grid of your memes and a Shared grid of feed memes you reposted to your page, with stats and full-screen viewing.
- **Comments** — every meme has its own thread. No account needed; signed-in users post under their display name, everyone else shows as "anon".
- **Reactions** — one-tap laugh react on anything.
- **Reports** — flag button on every meme and comment feeds a moderation queue.
- **Accounts** — email sign-up / sign-in, session persists across launches.

## Compatibility

Android 8.0 (API 26) and later.

## Setup

1. Open this folder in Android Studio. It syncs Gradle automatically.
2. API keys live in `Secrets.kt` (already in the repo).
3. The Supabase backend is shared with the iOS app — same project, same tables (`posts`, `comments`, `reports`, `reposts`) and `post-images` bucket. See the release notes for the SQL setup.
4. Run on a device or emulator.

## Tech

- Kotlin 2.2 + Jetpack Compose (Material 3, BOM 2026.08.00)
- Gradle 9.7.1, Android Gradle Plugin 9.3.1, compileSdk 37
- OkHttp for networking (feed + Supabase REST, no SDKs)
- Coil 3 for animated GIF/image loading
- Media3 ExoPlayer for video playback
- SharedPreferences for reactions, auth session, and offline caches

© 2026 Elijah McKeon. All rights reserved.
