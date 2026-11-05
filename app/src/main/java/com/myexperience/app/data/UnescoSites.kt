package com.myexperience.app.data

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Patrimonio mondiale UNESCO. [points] sono i luoghi in cui si può raccogliere il bollino:
 * per i siti estesi o seriali ci sono più punti (monumenti principali, componenti del sito).
 * [icon] è una chiave risolta in un'icona dalla UI.
 */
data class UnescoSite(
    val id: String,
    val name: String,
    val region: String,
    val year: Int,
    val natural: Boolean,
    val icon: String,
    val points: List<Pair<Double, Double>>,
) {
    val lat get() = points[0].first
    val lon get() = points[0].second
}

/** Raggio entro cui si può raccogliere il bollino. */
const val UNESCO_RADIUS_M = 200.0

/** Distanza in metri tra due coordinate (formula dell'emisenoverso). */
fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6_371_000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).let { it * it } +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).let { it * it }
    return 2 * r * asin(sqrt(a.coerceIn(0.0, 1.0)))
}

/** Distanza dal punto più vicino del sito. */
fun UnescoSite.distanceFrom(lat: Double, lon: Double): Double =
    points.minOf { (a, b) -> distanceMeters(lat, lon, a, b) }

private fun site(id: String, name: String, region: String, year: Int, icon: String, vararg pts: Pair<Double, Double>, natural: Boolean = false) =
    UnescoSite(id, name, region, year, natural, icon, pts.toList())

/** Per ora sono mappati solo i patrimoni in Italia. */
val UnescoSites: List<UnescoSite> = listOf(
    site("valcamonica", "Arte rupestre della Valcamonica", "Lombardia", 1979, "art",
        46.0306 to 10.3480, 46.0297 to 10.3436, 46.0190 to 10.3390),
    site("roma", "Centro storico di Roma", "Lazio", 1980, "arena",
        41.8902 to 12.4922, 41.8986 to 12.4769, 41.8925 to 12.4853, 41.9009 to 12.4833, 41.8992 to 12.4731,
        41.8859 to 12.5057, 41.8976 to 12.4984, 41.9031 to 12.4663, 41.8587 to 12.4768, 41.8894 to 12.4823),
    site("cenacolo", "Santa Maria delle Grazie e il Cenacolo di Leonardo", "Lombardia", 1980, "art",
        45.4659 to 9.1709),
    site("firenze", "Centro storico di Firenze", "Toscana", 1982, "church",
        43.7731 to 11.2560, 43.7696 to 11.2558, 43.7680 to 11.2531, 43.7678 to 11.2553, 43.7651 to 11.2500,
        43.7686 to 11.2622, 43.7746 to 11.2493, 43.7762 to 11.2590),
    site("venezia", "Venezia e la sua Laguna", "Veneto", 1987, "boat",
        45.4341 to 12.3388, 45.4380 to 12.3359, 45.4314 to 12.3280, 45.4410 to 12.3210, 45.4580 to 12.3530,
        45.4853 to 12.4170, 45.4977 to 12.4183, 45.2190 to 12.2790, 45.4300 to 12.3350),
    site("pisa", "Piazza del Duomo di Pisa", "Toscana", 1987, "tower",
        43.7230 to 10.3966),
    site("san_gimignano", "Centro storico di San Gimignano", "Toscana", 1990, "tower",
        43.4677 to 11.0432),
    site("matera", "I Sassi e il Parco delle Chiese Rupestri di Matera", "Basilicata", 1993, "cave",
        40.6664 to 16.6110, 40.6635 to 16.6127, 40.6680 to 16.6100, 40.6620 to 16.6230),
    site("vicenza", "Città di Vicenza e le ville del Palladio in Veneto", "Veneto", 1994, "villa",
        45.5467 to 11.5466, 45.5486 to 11.5502, 45.5310 to 11.5604, 45.8170 to 11.9810, 45.7110 to 11.9970,
        45.4372 to 12.2058, 45.0290 to 11.6440, 45.6050 to 11.9950),
    site("siena", "Centro storico di Siena", "Toscana", 1995, "tower",
        43.3186 to 11.3316, 43.3176 to 11.3288),
    site("napoli", "Centro storico di Napoli", "Campania", 1995, "church",
        40.8525 to 14.2600, 40.8467 to 14.2527, 40.8500 to 14.2571, 40.8384 to 14.2527, 40.8535 to 14.2508),
    site("crespi", "Crespi d'Adda", "Lombardia", 1995, "factory",
        45.5953 to 9.5378),
    site("ferrara", "Ferrara, città del Rinascimento, e il Delta del Po", "Emilia-Romagna", 1995, "castle",
        44.8378 to 11.6198, 44.8357 to 11.6198, 44.8418 to 11.6215, 44.8327 to 12.1769, 44.6947 to 12.1829),
    site("castel_del_monte", "Castel del Monte", "Puglia", 1996, "castle",
        41.0847 to 16.2708),
    site("alberobello", "I Trulli di Alberobello", "Puglia", 1996, "trullo",
        40.7820 to 17.2370, 40.7810 to 17.2365, 40.7860 to 17.2380),
    site("ravenna", "Monumenti paleocristiani di Ravenna", "Emilia-Romagna", 1996, "mosaic",
        44.4206 to 12.1965, 44.4166 to 12.2045, 44.4163 to 12.1966, 44.4189 to 12.2032, 44.4246 to 12.2096,
        44.3801 to 12.2333, 44.4159 to 12.1970),
    site("pienza", "Centro storico di Pienza", "Toscana", 1996, "church",
        43.0767 to 11.6789),
    site("caserta", "Reggia di Caserta, Acquedotto Vanvitelliano e San Leucio", "Campania", 1997, "palace",
        41.0732 to 14.3266, 41.0850 to 14.3262, 41.0957 to 14.3110, 41.0610 to 14.4390),
    site("sabaude", "Residenze Sabaude", "Piemonte", 1997, "palace",
        45.0726 to 7.6857, 45.0710 to 7.6858, 45.0680 to 7.6855, 45.1355 to 7.6267, 44.7690 to 7.6750,
        44.9950 to 7.6050, 45.0540 to 7.6860, 45.0700 to 7.5130, 44.9990 to 7.6870, 45.3640 to 7.7680,
        45.0590 to 7.7000, 44.6870 to 7.8930, 44.8040 to 8.0960),
    site("orto_padova", "Orto botanico di Padova", "Veneto", 1997, "garden",
        45.3994 to 11.8807),
    site("cinque_terre", "Portovenere, Cinque Terre e Isole", "Liguria", 1997, "coast",
        44.0505 to 9.8350, 44.0993 to 9.7380, 44.1067 to 9.7290, 44.1196 to 9.7097, 44.1350 to 9.6845,
        44.1460 to 9.6545, 44.0420 to 9.8430),
    site("modena", "Modena: Cattedrale, Torre Civica e Piazza Grande", "Emilia-Romagna", 1997, "church",
        44.6465 to 10.9255),
    site("pompei", "Aree archeologiche di Pompei, Ercolano e Torre Annunziata", "Campania", 1997, "temple",
        40.7489 to 14.4846, 40.7510 to 14.4950, 40.7535 to 14.4775, 40.8059 to 14.3480, 40.7570 to 14.4530),
    site("amalfi", "Costiera Amalfitana", "Campania", 1997, "coast",
        40.6340 to 14.6027, 40.6281 to 14.4850, 40.6491 to 14.6116, 40.6480 to 14.6410, 40.6500 to 14.6270,
        40.6360 to 14.6080, 40.6120 to 14.5270, 40.6470 to 14.7010, 40.6720 to 14.7290, 40.6180 to 14.5730),
    site("agrigento", "Area archeologica di Agrigento", "Sicilia", 1997, "temple",
        37.2902 to 13.5925, 37.2896 to 13.6000, 37.2898 to 13.5862, 37.2905 to 13.5800, 37.2960 to 13.5900),
    site("casale", "Villa Romana del Casale", "Sicilia", 1997, "mosaic",
        37.3653 to 14.3348),
    site("barumini", "Su Nuraxi di Barumini", "Sardegna", 1997, "fort",
        39.7056 to 8.9906),
    site("aquileia", "Area archeologica e Basilica patriarcale di Aquileia", "Friuli-Venezia Giulia", 1998, "mosaic",
        45.7695 to 13.3700, 45.7740 to 13.3700, 45.7715 to 13.3670),
    site("urbino", "Centro storico di Urbino", "Marche", 1998, "palace",
        43.7240 to 12.6370, 43.7255 to 12.6355),
    site("cilento", "Cilento e Vallo di Diano con Paestum, Velia e la Certosa di Padula", "Campania", 1998, "temple",
        40.4197 to 15.0055, 40.4185 to 15.0050, 40.4235 to 15.0060, 40.1605 to 15.1550, 40.3388 to 15.6547),
    site("villa_adriana", "Villa Adriana", "Lazio", 1999, "temple",
        41.9420 to 12.7740, 41.9395 to 12.7725, 41.9440 to 12.7750),
    site("eolie", "Isole Eolie", "Sicilia", 2000, "volcano",
        38.4672 to 14.9540, 38.8030 to 15.2350, 38.4170 to 14.9580, 38.5640 to 14.8680, 38.6380 to 15.0720,
        38.5640 to 14.5750, 38.5390 to 14.3610, natural = true),
    site("assisi", "Assisi, la Basilica di San Francesco e altri siti francescani", "Umbria", 2000, "church",
        43.0750 to 12.6055, 43.0687 to 12.6187, 43.0710 to 12.6153, 43.0580 to 12.5830, 43.0650 to 12.6390,
        43.0632 to 12.6180),
    site("verona", "Città di Verona", "Veneto", 2000, "arena",
        45.4390 to 10.9944, 45.4419 to 10.9984, 45.4430 to 10.9975, 45.4398 to 10.9877, 45.4475 to 11.0010,
        45.4470 to 10.9970, 45.4423 to 10.9792),
    site("villa_este", "Villa d'Este", "Lazio", 2001, "fountain",
        41.9627 to 12.7962),
    site("val_di_noto", "Città tardo barocche del Val di Noto", "Sicilia", 2002, "church",
        36.8920 to 15.0690, 36.8580 to 14.7600, 36.9270 to 14.7400, 36.9260 to 14.7260, 36.7920 to 14.7040,
        37.2370 to 14.5150, 37.2750 to 14.7930, 37.0610 to 14.9020, 37.5026 to 15.0873, 37.5050 to 15.0850),
    site("sacri_monti", "Sacri Monti del Piemonte e della Lombardia", "Piemonte e Lombardia", 2003, "church",
        45.8180 to 8.2550, 45.0960 to 8.2720, 45.8020 to 8.4050, 45.8610 to 8.7900, 45.6280 to 7.9800,
        45.9690 to 9.1680, 45.9580 to 8.6050, 46.1110 to 8.2810, 45.3830 to 7.6370),
    site("etruschi", "Necropoli etrusche di Cerveteri e Tarquinia", "Lazio", 2004, "tomb",
        42.0055 to 12.1050, 42.2490 to 11.7700),
    site("val_orcia", "Val d'Orcia", "Toscana", 2004, "hills",
        43.0580 to 11.6050, 43.0280 to 11.6190, 43.0050 to 11.6150, 43.0590 to 11.4890, 42.8970 to 11.7680,
        43.0447 to 11.6366),
    site("siracusa", "Siracusa e le Necropoli rupestri di Pantalica", "Sicilia", 2005, "temple",
        37.0597 to 15.2935, 37.0590 to 15.2910, 37.0757 to 15.2750, 37.0770 to 15.2780, 37.1430 to 15.0280),
    site("genova", "Genova: le Strade Nuove e i Palazzi dei Rolli", "Liguria", 2006, "palace",
        44.4113 to 8.9334, 44.4150 to 8.9270, 44.4075 to 8.9340),
    site("mantova", "Mantova e Sabbioneta", "Lombardia", 2008, "palace",
        45.1600 to 10.7990, 45.1470 to 10.7880, 45.1585 to 10.7955, 44.9990 to 10.4890),
    site("retica", "Ferrovia Retica nel paesaggio dell'Albula e del Bernina", "Lombardia", 2008, "train",
        46.2167 to 10.1657),
    site("dolomiti", "Dolomiti", "Veneto, Trentino-Alto Adige, Friuli", 2009, "mountain",
        46.6122 to 12.2950, 46.6186 to 12.3020, 46.4580 to 11.8670, 46.6000 to 11.7250, 46.5080 to 11.7580,
        46.5150 to 12.0500, 46.6946 to 12.0857, 46.4580 to 11.6200, 46.2170 to 10.8640, 46.5180 to 12.2120,
        46.6430 to 11.7240, natural = true),
    site("san_giorgio", "Monte San Giorgio", "Lombardia", 2010, "fossil",
        45.8890 to 8.8890, natural = true),
    site("longobardi", "I Longobardi in Italia. I luoghi del potere", "Italia", 2011, "church",
        46.0950 to 13.4340, 45.5400 to 10.2260, 45.7240 to 8.8620, 45.7260 to 8.8640, 42.7420 to 12.7350,
        42.8270 to 12.7710, 41.1310 to 14.7800, 41.7080 to 15.9550),
    site("palafitte", "Siti palafitticoli preistorici delle Alpi", "Italia", 2011, "lake",
        45.8770 to 10.7680, 46.0040 to 10.8370, 45.8130 to 8.7290),
    site("ville_medicee", "Ville e giardini medicei in Toscana", "Toscana", 2013, "villa",
        43.8190 to 11.2280, 43.8160 to 11.2340, 43.7625 to 11.2480, 43.8150 to 11.0590, 43.7590 to 10.8810,
        43.9660 to 11.2750, 43.8590 to 11.3040, 43.7860 to 11.0010, 43.8050 to 11.2870, 43.9940 to 10.2300,
        43.7510 to 11.2440),
    site("etna", "Monte Etna", "Sicilia", 2013, "volcano",
        37.6990 to 14.9990, 37.6980 to 15.0000, 37.7330 to 14.9980, natural = true),
    site("langhe", "Paesaggi vitivinicoli di Langhe-Roero e Monferrato", "Piemonte", 2014, "wine",
        44.6100 to 7.9430, 44.7240 to 8.0810, 44.6550 to 7.9930, 44.7210 to 8.2920, 44.7740 to 8.3570,
        44.6380 to 7.9330, 44.6090 to 8.0000),
    site("palermo", "Palermo arabo-normanna e le cattedrali di Cefalù e Monreale", "Sicilia", 2015, "mosaic",
        38.1110 to 13.3530, 38.1142 to 13.3560, 38.1100 to 13.3500, 38.1156 to 13.3630, 38.1170 to 13.3360,
        38.1080 to 13.3740, 38.0393 to 14.0230, 38.0816 to 13.2924),
    site("difese_venete", "Opere di difesa veneziane: Bergamo, Peschiera, Palmanova", "Italia", 2017, "fort",
        45.7030 to 9.6620, 45.4390 to 10.6910, 45.9050 to 13.3100),
    site("faggete", "Antiche faggete primordiali d'Europa", "Italia", 2017, "forest",
        43.8450 to 11.8100, 41.8150 to 13.7050, 42.4060 to 12.2010, 42.1630 to 12.1490, 41.8188 to 15.9960,
        natural = true),
    site("ivrea", "Ivrea, città industriale del XX secolo", "Piemonte", 2018, "factory",
        45.4610 to 7.8730, 45.4660 to 7.8800),
    site("prosecco", "Colline del Prosecco di Conegliano e Valdobbiadene", "Veneto", 2019, "wine",
        45.9010 to 11.9960, 45.9000 to 12.1300, 45.9530 to 12.1190, 45.8990 to 12.1720, 45.9330 to 12.0670,
        45.9480 to 12.1620),
    site("padova_affreschi", "Padova Urbs picta: i cicli di affreschi del Trecento", "Veneto", 2021, "art",
        45.4116 to 11.8794, 45.4071 to 11.8752, 45.4075 to 11.8725, 45.4016 to 11.8807),
    site("montecatini", "Montecatini Terme (Grandi città termali d'Europa)", "Toscana", 2021, "spa",
        43.8858 to 10.7740, 43.8830 to 10.7740),
    site("portici", "I Portici di Bologna", "Emilia-Romagna", 2021, "arch",
        44.4793 to 11.2983, 44.4890 to 11.3150, 44.4938 to 11.3426, 44.4960 to 11.3500, 44.4920 to 11.3480,
        44.4920 to 11.3530, 44.4880 to 11.3530, 44.4980 to 11.3080, 44.4930 to 11.2970),
    site("appia", "Via Appia. Regina Viarum", "Lazio, Campania, Basilicata, Puglia", 2024, "road",
        41.8740 to 12.5010, 41.8520 to 12.5220, 41.8300 to 12.5490, 41.7200 to 12.6730, 41.2890 to 13.2490,
        41.2650 to 13.5940, 41.0850 to 14.2510, 41.1320 to 14.7820, 40.6405 to 17.9440),
    site("domus_janas", "Domus de Janas, arte e architettura funeraria della Sardegna", "Sardegna", 2025, "tomb",
        40.6340 to 8.3350, 39.1220 to 8.6800, 40.4500 to 8.7720, 40.8060 to 8.4180, 39.5850 to 9.3330),
)
