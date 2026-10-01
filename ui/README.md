# The design system

The Compose components, colour, type, shape and spacing that the app draws
with. `ui/theme/` is the only source of colour, type and shape: reach for
`AppTheme.colors` / `.typography` / `.dimensions` rather than a literal, and never
read a colour out of `res/values` directly.

This module was a View-based component library when the project was imported
from Pocket. The XML layouts, the custom `View` classes (`ThemedView`,
`ItemRowView`, `BadgeView`, the `com.pocket.ui.view.button.*` family) and the
`NestedColorStateList` workaround are all gone. What remains is Compose, and
this file is the whole of it — there is no second, XML way to do any of it.

## Components

| | |
|---|---|
| [`AppBar`](src/main/java/com/neverreader/ui/compose/AppBar.kt) | The screen header: title, optional back arrow, and an actions row. |
| [`ItemRow`](src/main/java/com/neverreader/ui/compose/ItemRow.kt) | A row in the saved list: title, domain, reading time, image, and the favourite/share/archive actions. |
| [`FilterChips`](src/main/java/com/neverreader/ui/compose/FilterChips.kt) | The tab and sort row above the list. `FilterChips` is the container, `FilterChip` one chip. |
| [`SearchField`](src/main/java/com/neverreader/ui/compose/SearchField.kt) | The list search input, with its own clear button. |
| [`SettingsList`](src/main/java/com/neverreader/ui/compose/SettingsList.kt) | `SettingsHeader`, `SettingsAction` and `SettingsToggle`, for the settings screen. |
| [`AppSnackbarHost`](src/main/java/com/neverreader/ui/compose/AppSnackbarHost.kt) | Where snackbars render. `AbsNeverReaderActivity` owns one `SnackbarHostState` for the whole app. |
| [`AppIconButton`](src/main/java/com/neverreader/ui/view/button/AppIconButton.kt) | The icon button used throughout, themed and with a minimum touch target. |
| [`AppIcons`](src/main/java/com/neverreader/ui/view/button/AppIcons.kt) | The small set of drawn icons: `UpIcon`, `ArchiveIcon`, and the rest. |

Bottom sheets use Compose's own `ModalBottomSheet`. The design system's
`TransparentBottomSheetDialogTheme` is gone with the View implementation.

Every component above has tests under `src/test`, running on the JVM under
Robolectric — no emulator. They assert layout and behaviour: the trailing edge of
`ItemRow`'s actions, the 56dp of `AppBar`, that `SettingsToggle` follows its
preference rather than a snapshot of it, that a thumbnail which will not load
takes no space. What they cannot see is anything visual: Robolectric's rendering
is not faithful enough to assert a colour, so the theming above is only covered
by eye.

## Colour

`values/colors.xml` holds the palette, and the file's own header comment is the
authority on how to add to it: define a colour there, and prefer the `nr_`
prefix so it is clear where it came from. Dark variants are plain
`values-night` overrides now, not the `nr_nst_*` selector workarounds.

Compose reads the palette through `AppTheme.colors`; a screen never names a
`R.color` itself.

## Type

The UI face is **Inter** and the display and reading face is **Source Serif 4**,
both OFL-1.1, in [`src/main/assets/fonts`](src/main/assets/fonts). They replaced
Pocket's licensed Graphik LCG and Doyle.

`AppFontFamily` in `theme/Type.kt` loads them for Compose. The reader is a
WebView, so it gets them through `@font-face` rules in
`app/src/main/assets/html/c/text.css` instead — if you change a face, change
both.

## Resources

Drawables are `ic_nr_*`. Colours and dimensions are `nr_*`; the list row's image
tile is `saves_image_*`, which the app module also overrides in `values-large`,
so the ui copy is the default and the app copy is the large-screen one.
