# Where to?

**Where to?** is an Android app for spontaneous drives. It creates a random, turn-by-turn route on real roads near you—no destination required.

## Features

- Random drives using nearby OpenStreetMap road data.
- Dark, road-aligned driving view with a highlighted route and navigation arrow.
- Spoken turn guidance, mute control, and recenter control.
- On-device road cache for recently used areas.
- Route preferences to avoid dead ends, residential streets, and toll roads.
- Return-to-start navigation using the shortest available route in the local road graph.
- Bottom media card for Spotify or YouTube Music: artwork, track details, previous, play/pause, next, and swipe controls.
- Nearby toilet finder.

## Running the app

1. Open the project in Android Studio.
2. Add your restricted Google Maps Android API key to `local.properties`:

   ```properties
   MAPS_API_KEY=your_key_here
   ```

3. Run it on an Android device or emulator and allow location permission.

For media controls, enable **Where to? media controls** in Android's **Notification access** settings after installing the app.

## Notes

Road data comes from OpenStreetMap through public Overpass services. It is suitable for testing and personal use; public services can occasionally be slow or unavailable.
