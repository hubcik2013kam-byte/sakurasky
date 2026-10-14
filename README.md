# Sakura Sky (Fabric, Minecraft 1.21.11)

Klient-only mod. Klawisz **K** (zmienialny w Opcje > Sterowanie > Sakura Sky) otwiera menu:
- Niebo: wlacz + kolor (hex / RGB)
- Mgla: wlacz + kolor + poczatek/koniec
- Czasteczki: sakura, snieg, swietliki, iskry, kurz; kolor, ilosc, rozmiar, predkosc

Config: `config/sakurasky.json`. Tekstury czasteczek (biale, tintowane kolorem): `assets/sakurasky/textures/particle/*.png`
- mozesz je podmienic wlasnymi.

## Budowanie
1. Wygeneruj szablon na https://fabricmc.net/develop/template/ (1.21.11, Mojang mappings) i skopiuj z niego
   `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`.
2. Wrzuc tu `src`, `build.gradle`, `gradle.properties`, `settings.gradle`.
3. `./gradlew build` (Java 21) -> `build/libs/sakurasky-1.0.0.jar` (nie *-sources.jar).
4. Jar + Fabric API do folderu `mods`.

## Najprosciej: jar zbudowany przez GitHub (bez instalowania czegokolwiek)
1. Zaloz konto na github.com i nowe repozytorium (np. `sakurasky`).
2. Wrzuc tam cala zawartosc tego folderu (przycisk "Add file" > "Upload files"; wlacznie z folderem `.github`).
3. Zakladka **Actions** > ostatni przebieg "build" > na dole **Artifacts** > pobierz `sakurasky-jar`.
4. W zipie jest Twoj `sakurasky-1.0.0.jar` - wrzuc go do `mods` razem z Fabric API.
Jesli build sie wywali, otworz log i wklej mi blad.
