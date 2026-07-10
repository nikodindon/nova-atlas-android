package com.niko.novaatlas.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Palette Nova-Atlas (alignee sur le site Flask).
 * Source: site/static/css/ du repo nova-atlas (extraction grep sur 2026-07-09).
 */

// Backgrounds (du plus sombre au plus clair)
val NovaBg0 = Color(0xFF0A0A0F)  // fond principal
val NovaBg1 = Color(0xFF111118)  // surface elevee 1
val NovaBg2 = Color(0xFF1A1A24)  // surface elevee 2
val NovaBg3 = Color(0xFF1A1D2E)  // card article
val NovaBg4 = Color(0xFF2A2A3A)  // bordure / divider

// Texte
val NovaTextPrimary = Color(0xFFEDF0F7)
val NovaTextSecondary = Color(0xFFD0D7E8)
val NovaTextMuted = Color(0xFF8A90A8)
val NovaTextDim = Color(0xFF555568)

// Accents
val NovaAccentYellow = Color(0xFFC9A84C)  // categories, badges
val NovaAccentOrange = Color(0xFFE8612A)  // breaking / chaud
val NovaAccentBlue = Color(0xFF3B55E8)    // liens, actif
val NovaAccentGreen = Color(0xFF22C55E)   // EN DIRECT
val NovaAccentBlueLight = Color(0xFF6EA8FE)
val NovaAccentRed = Color(0xFFC44D1E)

// Couleurs par categorie (scan rapide du feed en 2 sec).
// Aligne sur les 16 categories du serveur (modules/web/atlas_web.py).
// Mapping : cle serveur -> couleur d'accent pour pastille/tag/FilterChip.
val NovaCatGeopolitique      = Color(0xFF3B55E8)  // bleu
val NovaCatEconomie          = Color(0xFFE8612A)  // orange
val NovaCatCrypto            = Color(0xFFC9A84C)  // jaune
val NovaCatTechIA            = Color(0xFF8B5CF6)  // violet
val NovaCatFrance            = Color(0xFF6EA8FE)  // bleu clair
val NovaCatMonde             = Color(0xFF06B6D4)  // cyan
val NovaCatScienceSante      = Color(0xFF22C55E)  // vert
val NovaCatEnvironnement     = Color(0xFF4ADE80)  // vert clair
val NovaCatSociete           = Color(0xFFEC4899)  // rose
val NovaCatCulture           = Color(0xFFA78BFA)  // violet clair
val NovaCatSport             = Color(0xFFEF4444)  // rouge
val NovaCatSante             = Color(0xFF14B8A6)  // turquoise
val NovaCatGaming            = Color(0xFF6366F1)  // indigo
val NovaCatSciencesHumaines  = Color(0xFFF59E0B)  // ambre
val NovaCatAuto              = Color(0xFF64748B)  // gris-bleu
val NovaCatRegions           = Color(0xFF92400E)  // brun
