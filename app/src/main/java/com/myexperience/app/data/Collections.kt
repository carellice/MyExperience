package com.myexperience.app.data

/** Una collezione di bollini: ogni elemento ha un aspetto unico e si raccoglie sul posto con una foto. */
data class BadgeCollection(
    val id: String,
    val title: String,
    val subtitle: String,
    /** Testo breve per il pop-up di raccolta ("Patrimonio UNESCO", "Museo"). */
    val kind: String,
    val items: List<UnescoSite>,
)

private fun museum(id: String, name: String, city: String, year: Int, icon: String, lat: Double, lon: Double) =
    UnescoSite("museo_$id", name, city, year, false, icon, listOf(lat to lon), label = "MUSEO")

/** I grandi musei del mondo, con l'anno di apertura al pubblico. */
val Museums: List<UnescoSite> = listOf(
    museum("louvre", "Museo del Louvre", "Parigi, Francia", 1793, "museum", 48.8606, 2.3376),
    museum("orsay", "Museo d'Orsay", "Parigi, Francia", 1986, "art", 48.8600, 2.3266),
    museum("british", "British Museum", "Londra, Regno Unito", 1759, "museum", 51.5194, -0.1270),
    museum("national_gallery", "National Gallery", "Londra, Regno Unito", 1824, "art", 51.5089, -0.1283),
    museum("tate_modern", "Tate Modern", "Londra, Regno Unito", 2000, "factory", 51.5076, -0.0994),
    museum("vaticani", "Musei Vaticani", "Città del Vaticano", 1506, "church", 41.9065, 12.4536),
    museum("uffizi", "Galleria degli Uffizi", "Firenze, Italia", 1769, "art", 43.7678, 11.2553),
    museum("accademia_firenze", "Galleria dell'Accademia", "Firenze, Italia", 1784, "statue", 43.7768, 11.2586),
    museum("borghese", "Galleria Borghese", "Roma, Italia", 1903, "statue", 41.9142, 12.4921),
    museum("egizio_torino", "Museo Egizio", "Torino, Italia", 1824, "tomb", 45.0684, 7.6843),
    museum("brera", "Pinacoteca di Brera", "Milano, Italia", 1809, "art", 45.4719, 9.1879),
    museum("mann", "Museo Archeologico Nazionale", "Napoli, Italia", 1777, "temple", 40.8535, 14.2508),
    museum("accademia_venezia", "Gallerie dell'Accademia", "Venezia, Italia", 1817, "art", 45.4311, 12.3281),
    museum("prado", "Museo del Prado", "Madrid, Spagna", 1819, "art", 40.4138, -3.6921),
    museum("reina_sofia", "Museo Reina Sofía", "Madrid, Spagna", 1992, "art", 40.4086, -3.6944),
    museum("guggenheim_bilbao", "Museo Guggenheim", "Bilbao, Spagna", 1997, "museum", 43.2687, -2.9340),
    museum("rijksmuseum", "Rijksmuseum", "Amsterdam, Paesi Bassi", 1800, "museum", 52.3600, 4.8852),
    museum("van_gogh", "Museo Van Gogh", "Amsterdam, Paesi Bassi", 1973, "art", 52.3584, 4.8811),
    museum("pergamon", "Pergamonmuseum", "Berlino, Germania", 1930, "temple", 52.5212, 13.3969),
    museum("khm", "Kunsthistorisches Museum", "Vienna, Austria", 1891, "palace", 48.2038, 16.3617),
    museum("ermitage", "Museo dell'Ermitage", "San Pietroburgo, Russia", 1764, "palace", 59.9398, 30.3146),
    museum("acropoli", "Museo dell'Acropoli", "Atene, Grecia", 2009, "temple", 37.9685, 23.7285),
    museum("met", "Metropolitan Museum of Art", "New York, Stati Uniti", 1870, "museum", 40.7794, -73.9632),
    museum("moma", "MoMA", "New York, Stati Uniti", 1929, "art", 40.7614, -73.9776),
    museum("art_institute", "Art Institute of Chicago", "Chicago, Stati Uniti", 1879, "art", 41.8796, -87.6237),
    museum("antropologia", "Museo Nazionale di Antropologia", "Città del Messico, Messico", 1964, "tomb", 19.4260, -99.1863),
    museum("egizio_cairo", "Museo Egizio del Cairo", "Il Cairo, Egitto", 1902, "tomb", 30.0478, 31.2336),
    museum("tokyo", "Museo Nazionale di Tokyo", "Tokyo, Giappone", 1872, "temple", 35.7188, 139.7765),
).mapIndexed { i, s -> s.copy(order = 50 + i) } // order diverso dai siti UNESCO: aspetto sempre unico

val BadgeCollections: List<BadgeCollection> = listOf(
    BadgeCollection(
        "unesco", "Patrimoni UNESCO",
        "Italia · avvicinati a meno di 200 m da un patrimonio e incolla una tua foto per raccoglierne il bollino",
        "Patrimonio UNESCO", UnescoSites,
    ),
    BadgeCollection(
        "musei", "Grandi musei del mondo",
        "Avvicinati a meno di 200 m da un museo e incolla una tua foto per raccoglierne il bollino",
        "Museo", Museums,
    ),
)

/** Tutti i bollini di tutte le collezioni. */
val AllBadgeSites: List<UnescoSite> = BadgeCollections.flatMap { it.items }

fun collectionOf(site: UnescoSite): BadgeCollection = BadgeCollections.first { site in it.items }
