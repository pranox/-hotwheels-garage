# HW Garage

Android app for your Hot Wheels collection. Take a photo, and the app identifies the car and adds it to your garage.

## How identification works

1. **ML Kit (on-device, free, offline)** reads the text and barcode in the photo.
2. A **toy number** like `JJM00` is looked up on the [Hot Wheels Fandom wiki](https://hotwheels.fandom.com). That gives the exact release, its photo, and whether it's a Treasure Hunt or Super Treasure Hunt. Exact matches go straight to the confirm screen.
3. Otherwise the biggest text on the card or base, the barcode (UPCitemdb free tier) and, optionally, **Gemini** (free API key, set in Settings) are used to suggest matches.
4. You confirm the casting and pick which version you have, then it's saved in the local Room database.

## Tabs

- **Garage**: search and filters (Wishlist, Treasure Hunts, Carded/Loose), duplicate counts, and editing price, notes and condition.
- **Scan**: camera, gallery, or a typed name. Shows "Owned ×N" so you can use it in the store.
- **Hype**: this year's Super Treasure Hunts and Treasure Hunts, next year's hunts, the newest mainline releases, and the top posts of the week on r/HotWheels. Tap a car to add it to your wishlist.
- **Stats**: totals, Treasure Hunt counts, money spent, and breakdowns by series, year and color.
- **Settings**: Gemini key, and JSON backup export/import.

## Build & run

1. Install [Android Studio](https://developer.android.com/studio). It includes the JDK and Android SDK.
2. **File → Open** and choose this folder. Wait for Gradle sync.
3. Plug in your phone with USB debugging on (Settings → About → tap *Build number* 7 times → Developer options → USB debugging), then press **Run ▶**.

From the command line: `./gradlew assembleDebug` creates `app/build/outputs/apk/debug/app-debug.apk`.

## Tips for good scans

- **Carded cars:** photograph the *back* of the card, where the toy number, collector number and barcode are printed.
- **Loose cars:** photograph the *base*, where the casting name is cast into the metal. Use good light and hold the phone close.
- Add a Gemini key to recognise loose cars from the top as well.

## Project layout

```
app/src/main/java/com/hotwheelsgarage/
  data/     Room DB (Car, CarDao), WikiApi + WikiText parser, Gemini, UPC, Reddit
  scan/     CardReader (ML Kit OCR + barcode), Identifier (matching pipeline)
  ui/       Compose screens: garage, scan, hype, stats, settings
```

Data and images come from the Hot Wheels Fandom wiki (CC BY-SA). Not affiliated with Mattel.
