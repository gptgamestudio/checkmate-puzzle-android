# Checkmate Chess Puzzle (2D Landscape Mobile)

A high-performance, dark-themed 2D Landscape Chess Puzzle & Checkmate Game built for 16:9 mobile devices with complete rule validation, level progression, custom arcade 3D visual board, Winner & Loser character reactions, and AdMob monetization rules.

---

## 🎮 Visual Style & Theme (Screen-Accurate)

- **16:9 Landscape Layout:** Optimized for horizontal mobile play with a dedicated bottom 320x50 Banner Ad area and clean side navigation controls.
- **Board Grid & Outer Frame:** Slate Charcoal (`#555B62`) and Pearlescent Silver (`#D6DBDF`) squares surrounded by a 3D rounded beveled outer border.
- **3D Bubble Pieces:** Pearl white and glossy jet-black pieces with realistic ground drop-shadows and specular curved highlights.
- **Winner & Loser Character Avatars ("Manalo at Matalo"):**
  - **Opponent King Avatar (Top-Left):** Reacts with a wide-eyed, gasping shocked expression when defeated by checkmate.
  - **Player King Avatar (Bottom-Right):** Features royal blue robe and crown, celebrating with a laughing face and thumbs-up gesture on checkmate, or a shocked expression if a wrong move is attempted.
- **3D Ribbon Banners:**
  - **"YOU WIN" Banner:** Arched 3D golden ribbon with royal blue backdrop and sparkling victory stars.
  - **"TRY AGAIN" Banner:** Red arched banner signaling an incorrect move sequence.
- **Timer & Quick Emotes:**
  - Timer pill (`⏱ 01:51`) at the top of the board.
  - Quick emote buttons (`WOW! 😲`, `GG! ✨`, `Good Luck! 🍀`, `OOPSIE 😭`) that trigger dynamic speech bubbles.

---

## ♟️ Chess Rule Engine & Tactical Categories

- **Rule Engine:** Complete board evaluation (pins, checks, double checks, legal move filtering, automated multi-step opponent responses).
- **Tactical Categories (Mate in 2, 3, 5, and 10):**
  - **Category 1 (Easy):** Mate in 2 Moves (*Scholar's Mate Trap*, *Smothered Combination*)
  - **Category 2 (Medium):** Mate in 3 Moves (*Boden's Mate Pattern*)
  - **Category 3 (Hard):** Mate in 5 Moves (*Rook & Knight Assault*)
  - **Category 4 (Master):** Mate in 10 Moves (*King & Rook Precision Mate*)

---

## 💰 Monetization & AdMob Integration

- **AdMob App ID:** `ca-app-pub-9786472984542330~7452842897`
- **Bottom Banner Ad (320x50):** `ca-app-pub-9786472984542330/2771361852`
- **Interstitial Ad:** `ca-app-pub-9786472984542330/2579790164` (Automatically triggered upon checkmate victory before progressing to the next level)

---

## 🛠️ Godot 4.x Engine Scripts Included

Full, production-ready GDScript files are included in `app/src/main/assets/godot/` and accessible in the in-app export viewer:
- `ChessPuzzleBackend.gd`: Pure RefCounted chess puzzle rule engine, move generation, FEN loader, and level progression manager.
- `ChessBoardUI.gd`: Control node drawing custom 3D bubble pieces, beveled slate/silver board, king character avatars, timer pill, and 3D victory banner.
- `PuzzleData.gd`: Tactical puzzle categories (Mate in 2, 3, 5, 10).
- `AdMobManager.gd`: AdMob monetization controller for banners and interstitials.

---

## 📦 APK Hosting & GitHub Actions

GitHub Actions automatically builds and packages the Android APK on every push:
1. Go to the **Actions** tab in this GitHub repository.
2. Click on the latest workflow run: **Build and Host Android APK**.
3. Under **Artifacts**, download `chess-puzzle-debug-apk` to install the APK directly on your device.
