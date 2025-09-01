# Design Tokens

Palette
- primary: #5E8BFF
- onPrimary: #FFFFFF
- secondary: #00D1B2
- background: #0E0F12
- surface: #15171C
- onSurface: #E6E8EE
- glassLight: #26FFFFFF
- glassDark: #1A000000
- glassStroke: #33FFFFFF

Typography
- DisplayLarge: 57/64/0
- HeadlineMedium: 28/36/0
- TitleMedium: 16/24/0
- BodyMedium: 14/20/0
- LabelLarge: 14/20/0

Spacing
- space2: 2dp
- space4: 4dp
- space8: 8dp
- space12: 12dp
- space16: 16dp
- space20: 20dp
- space24: 24dp
- space32: 32dp

Icons
- sizeSm: 18dp
- sizeMd: 24dp
- sizeLg: 32dp
- sizeXl: 48dp

Usage (Compose)
- Container: background(MaterialTheme.colorScheme.surface)
- Glass: GlassSurface(cornerRadius = 20.dp) { ... }
- Headlines: Text(style = MaterialTheme.typography.headlineSmall)
- Spacing: Modifier.padding(MotionTokens.Space16)

