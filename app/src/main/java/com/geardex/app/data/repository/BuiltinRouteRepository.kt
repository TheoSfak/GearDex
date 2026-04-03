package com.geardex.app.data.repository

import com.geardex.app.data.model.EkdromeDifficulty
import com.geardex.app.data.model.EkdromeRegion
import com.geardex.app.data.model.EkdromeRoute
import com.geardex.app.data.model.EkdromeTag
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BuiltinRouteRepository @Inject constructor() {

    val routes: List<EkdromeRoute> = listOf(
        // ── CRETE (12 routes) ──────────────────────────────────────
        EkdromeRoute(
            1, "Sfakia Mountain Pass", "Πέρασμα Σφακίων",
            EkdromeRegion.CRETE, listOf(EkdromeTag.MOTO, EkdromeTag.TWISTY),
            EkdromeDifficulty.MEDIUM, 85, 4.7f,
            "Stunning mountain road through the White Mountains descending to the Libyan Sea.",
            "Εκπληκτικός ορεινός δρόμος μέσα από τα Λευκά Όρη με κατάβαση στο Λιβυκό Πέλαγος.",
            35.1983, 24.1383,
            startLocation = "Chania", endLocation = "Hora Sfakion",
            waypoints = listOf("Askifou Plateau", "Imbros")
        ),
        EkdromeRoute(
            9, "Lasithi Plateau Loop", "Γύρος Οροπεδίου Λασιθίου",
            EkdromeRegion.CRETE, listOf(EkdromeTag.SCENIC, EkdromeTag.ASPHALT),
            EkdromeDifficulty.EASY, 45, 4.6f,
            "Circular route around the stunning Lasithi Plateau with windmills and mountain views.",
            "Κυκλική διαδρομή γύρω από το εκπληκτικό Οροπέδιο Λασιθίου με ανεμόμυλους και ορεινή θέα.",
            35.1833, 25.4667,
            startLocation = "Agios Nikolaos", endLocation = "Agios Nikolaos",
            waypoints = listOf("Neapoli", "Tzermiado", "Psychro Cave")
        ),
        EkdromeRoute(
            10, "Heraklion to Matala Coastal", "Ηράκλειο – Μάταλα Παράκτια",
            EkdromeRegion.CRETE, listOf(EkdromeTag.ASPHALT, EkdromeTag.SCENIC),
            EkdromeDifficulty.EASY, 70, 4.3f,
            "Relaxing coastal ride from Heraklion through Messara Valley to the famous Matala caves.",
            "Χαλαρωτική παράκτια διαδρομή από το Ηράκλειο μέσω κοιλάδας Μεσσαράς στις σπηλιές Μάταλα.",
            34.9950, 24.7500,
            startLocation = "Heraklion", endLocation = "Matala",
            waypoints = listOf("Gortyna", "Phaistos")
        ),
        EkdromeRoute(
            11, "Samaria Gorge Approach", "Προσέγγιση Φαράγγι Σαμαριάς",
            EkdromeRegion.CRETE, listOf(EkdromeTag.TWISTY, EkdromeTag.SCENIC),
            EkdromeDifficulty.MEDIUM, 60, 4.8f,
            "Winding descent from Omalos plateau to the entrance of Europe's longest gorge.",
            "Ελικοειδής κατάβαση από το οροπέδιο Ομαλού στην είσοδο του μεγαλύτερου φαραγγιού της Ευρώπης.",
            35.3200, 23.9600,
            startLocation = "Chania", endLocation = "Omalos Plateau",
            waypoints = listOf("Fournes", "Lakki")
        ),
        EkdromeRoute(
            12, "Rethymno to Plakias", "Ρέθυμνο – Πλακιάς",
            EkdromeRegion.CRETE, listOf(EkdromeTag.MOTO, EkdromeTag.TWISTY),
            EkdromeDifficulty.MEDIUM, 40, 4.5f,
            "Tight mountain switchbacks through Kourtaliotiko Gorge to the southern coast.",
            "Σφιχτές ορεινές στροφές μέσα από το Φαράγγι Κουρταλιώτικο στη νότια ακτή.",
            35.1800, 24.3900,
            startLocation = "Rethymno", endLocation = "Plakias",
            waypoints = listOf("Koxare", "Kourtaliotiko Gorge")
        ),
        EkdromeRoute(
            13, "Elafonisi Beach Road", "Δρόμος Ελαφονησίου",
            EkdromeRegion.CRETE, listOf(EkdromeTag.SCENIC, EkdromeTag.ASPHALT),
            EkdromeDifficulty.EASY, 75, 4.4f,
            "Scenic western Crete route through Kissamos to the pink-sand beaches of Elafonisi.",
            "Γραφική διαδρομή δυτικής Κρήτης μέσω Κισσάμου στις ροζ παραλίες Ελαφονησίου.",
            35.2722, 23.5417,
            startLocation = "Chania", endLocation = "Elafonisi Beach",
            waypoints = listOf("Kissamos", "Topolia Gorge")
        ),
        EkdromeRoute(
            14, "Imbros Gorge Run", "Διαδρομή Φαράγγι Ίμβρου",
            EkdromeRegion.CRETE, listOf(EkdromeTag.MOTO, EkdromeTag.TWISTY, EkdromeTag.SCENIC),
            EkdromeDifficulty.MEDIUM, 30, 4.6f,
            "Short but dramatic ride along the road paralleling Imbros Gorge to Hora Sfakion.",
            "Σύντομη αλλά εντυπωσιακή διαδρομή κατά μήκος του Φαραγγιού Ίμβρου προς Χώρα Σφακίων.",
            35.2600, 24.1833,
            startLocation = "Imbros", endLocation = "Hora Sfakion",
            waypoints = listOf("Komitades")
        ),
        EkdromeRoute(
            15, "Spinalonga & Elounda", "Σπιναλόγκα & Ελούντα",
            EkdromeRegion.CRETE, listOf(EkdromeTag.ASPHALT, EkdromeTag.SCENIC),
            EkdromeDifficulty.EASY, 35, 4.5f,
            "Coastal drive from Agios Nikolaos to Elounda with views of Spinalonga island.",
            "Παράκτια διαδρομή από Άγιο Νικόλαο στην Ελούντα με θέα Σπιναλόγκα.",
            35.2972, 25.7361,
            startLocation = "Agios Nikolaos", endLocation = "Elounda",
            waypoints = listOf("Plaka")
        ),
        EkdromeRoute(
            16, "Balos Lagoon Trail", "Μονοπάτι Μπάλος",
            EkdromeRegion.CRETE, listOf(EkdromeTag.OFFROAD, EkdromeTag.SCENIC),
            EkdromeDifficulty.HARD, 25, 4.7f,
            "Rough gravel road to the spectacular Balos Lagoon. 4x4 recommended.",
            "Τραχύς χωματόδρομος προς τη θεαματική λιμνοθάλασσα Μπάλος. Συνιστάται 4x4.",
            35.5828, 23.5883,
            startLocation = "Kissamos", endLocation = "Balos Lagoon",
            waypoints = listOf("Kaliviani")
        ),
        EkdromeRoute(
            17, "Ano Viannos Mountain Circuit", "Ορεινός Γύρος Ανω Βιάννου",
            EkdromeRegion.CRETE, listOf(EkdromeTag.TWISTY, EkdromeTag.OFFROAD),
            EkdromeDifficulty.HARD, 55, 4.3f,
            "Remote mountain circuit through the Dikti range with abandoned villages and epic solitude.",
            "Απομακρυσμένος ορεινός γύρος στη Δίκτη με εγκαταλειμμένα χωριά και απόλυτη ηρεμία.",
            35.0500, 25.4000,
            startLocation = "Ano Viannos", endLocation = "Ano Viannos",
            waypoints = listOf("Amiras", "Kato Symi")
        ),
        EkdromeRoute(
            18, "Preveli Monastery Road", "Δρόμος Μονής Πρέβελης",
            EkdromeRegion.CRETE, listOf(EkdromeTag.SCENIC, EkdromeTag.MOTO),
            EkdromeDifficulty.MEDIUM, 50, 4.5f,
            "South-coast route through olive groves to the historic Preveli Monastery and palm beach.",
            "Νοτιοκρητική διαδρομή μέσα από ελαιώνες στην ιστορική Μονή Πρέβελης και την παραλία φοινικόδασος.",
            35.1531, 24.4578,
            startLocation = "Rethymno", endLocation = "Preveli Monastery",
            waypoints = listOf("Lefkogia", "Asomatos")
        ),
        EkdromeRoute(
            19, "Theriso Gorge & Village", "Φαράγγι & Χωριό Θερίσου",
            EkdromeRegion.CRETE, listOf(EkdromeTag.TWISTY, EkdromeTag.ASPHALT),
            EkdromeDifficulty.EASY, 20, 4.2f,
            "Quick ride from Chania through a narrow gorge to the historic village of Theriso.",
            "Γρήγορη βόλτα από Χανιά μέσα από στενό φαράγγι στο ιστορικό χωριό του Θερίσου.",
            35.4400, 23.9900,
            startLocation = "Chania", endLocation = "Theriso"
        ),

        // ── PELOPONNESE (4 routes) ────────────────────────────────
        EkdromeRoute(
            2, "Mani Peninsula Loop", "Γύρος Μάνης",
            EkdromeRegion.PELOPONNESE, listOf(EkdromeTag.MOTO, EkdromeTag.ASPHALT),
            EkdromeDifficulty.EASY, 120, 4.5f,
            "Coastal and mountain loop through the wild Mani peninsula with stone tower villages.",
            "Παράκτια και ορεινή διαδρομή μέσα από την άγρια Μάνη με πύργους από πέτρα.",
            36.6333, 22.4167,
            startLocation = "Areopoli", endLocation = "Areopoli",
            waypoints = listOf("Gerolimenas", "Vathia", "Cape Tenaro")
        ),
        EkdromeRoute(
            8, "Taygetos Pass", "Πέρασμα Ταϋγέτου",
            EkdromeRegion.PELOPONNESE, listOf(EkdromeTag.MOTO, EkdromeTag.TWISTY, EkdromeTag.OFFROAD),
            EkdromeDifficulty.HARD, 75, 4.8f,
            "Epic pass over the Taygetos mountain range with breathtaking valley views.",
            "Εκπληκτικό πέρασμα πάνω από την οροσειρά Ταϋγέτου με θέα στους κόλπους.",
            37.0500, 22.3500,
            startLocation = "Sparta", endLocation = "Kalamata",
            waypoints = listOf("Langada Pass")
        ),
        EkdromeRoute(
            20, "Nemea Wine Road", "Δρόμος Κρασιού Νεμέας",
            EkdromeRegion.PELOPONNESE, listOf(EkdromeTag.ASPHALT, EkdromeTag.SCENIC),
            EkdromeDifficulty.EASY, 90, 4.2f,
            "Gentle cruise through the Nemea wine region past vineyards and ancient ruins.",
            "Ήρεμη βόλτα μέσα από τη δρόμο κρασιού Νεμέας με αμπελώνες και αρχαία ερείπια.",
            37.8200, 22.6600,
            startLocation = "Corinth", endLocation = "Ancient Nemea",
            waypoints = listOf("Koutsi", "Petri")
        ),
        EkdromeRoute(
            21, "Monemvasia Coastal Drive", "Παράκτια Διαδρομή Μονεμβασιάς",
            EkdromeRegion.PELOPONNESE, listOf(EkdromeTag.SCENIC, EkdromeTag.ASPHALT),
            EkdromeDifficulty.EASY, 100, 4.6f,
            "Stunning coastal road to the medieval fortress town of Monemvasia.",
            "Εκπληκτική παράκτια διαδρομή στη μεσαιωνική καστροπολιτεία Μονεμβασιάς.",
            36.6883, 23.0533,
            startLocation = "Sparti", endLocation = "Monemvasia",
            waypoints = listOf("Skala", "Molai")
        ),

        // ── EPIRUS (3 routes) ─────────────────────────────────────
        EkdromeRoute(
            3, "Vikos Gorge Route", "Φαράγγι Βίκου",
            EkdromeRegion.EPIRUS, listOf(EkdromeTag.MOTO, EkdromeTag.TWISTY),
            EkdromeDifficulty.HARD, 65, 4.9f,
            "Technical mountain roads around the world's deepest gorge with epic views.",
            "Τεχνικός ορεινός δρόμος γύρω από το βαθύτερο φαράγγι του κόσμου.",
            39.9100, 20.7603,
            startLocation = "Ioannina", endLocation = "Monodendri",
            waypoints = listOf("Vitsa", "Aristi")
        ),
        EkdromeRoute(
            5, "Pantokrator Summit", "Κορυφή Παντοκράτορα",
            EkdromeRegion.EPIRUS, listOf(EkdromeTag.OFFROAD, EkdromeTag.TWISTY),
            EkdromeDifficulty.HARD, 40, 4.4f,
            "Challenging dirt track to a high peak with 360-degree mountain views.",
            "Απαιτητικό χωματόδρομο σε ψηλή κορυφή με πανοραμική θέα 360 μοιρών.",
            39.7800, 19.8600,
            startLocation = "Corfu Town", endLocation = "Mount Pantokrator",
            waypoints = listOf("Strinilas")
        ),
        EkdromeRoute(
            22, "Zagorochoria Stone Bridge Tour", "Γύρος Πετρογέφυρων Ζαγοροχωρίων",
            EkdromeRegion.EPIRUS, listOf(EkdromeTag.SCENIC, EkdromeTag.ASPHALT),
            EkdromeDifficulty.MEDIUM, 70, 4.8f,
            "Tour through 46 stone villages of Zagori with Ottoman bridges and alpine scenery.",
            "Περιήγηση στα 46 πετρόχτιστα χωριά του Ζαγορίου με οθωμανικά γεφύρια και αλπικό τοπίο.",
            39.8900, 20.7200,
            startLocation = "Ioannina", endLocation = "Papingo",
            waypoints = listOf("Kipi Stone Bridges", "Tsepelovo", "Megalo Papingo")
        ),

        // ── MACEDONIA (3 routes) ──────────────────────────────────
        EkdromeRoute(
            4, "Nestos River Valley", "Κοιλάδα Νέστου",
            EkdromeRegion.MACEDONIA, listOf(EkdromeTag.ASPHALT, EkdromeTag.MOTO),
            EkdromeDifficulty.EASY, 95, 4.3f,
            "Scenic river valley road through dense forests and traditional villages.",
            "Γραφικός δρόμος κοιλάδας μέσα από πυκνά δάση και παραδοσιακά χωριά.",
            41.1500, 24.7167,
            startLocation = "Drama", endLocation = "Xanthi",
            waypoints = listOf("Paranesti", "Stavroupoli")
        ),
        EkdromeRoute(
            7, "Olympus Foothills Tour", "Πρόποδες Ολύμπου",
            EkdromeRegion.MACEDONIA, listOf(EkdromeTag.MOTO, EkdromeTag.TWISTY),
            EkdromeDifficulty.MEDIUM, 110, 4.6f,
            "The roads around Greece's highest mountain through forests and plateaus.",
            "Οι δρόμοι γύρω από το ψηλότερο βουνό της Ελλάδας μέσα από δάση και οροπέδια.",
            40.0858, 22.3583,
            startLocation = "Litochoro", endLocation = "Elatochori",
            waypoints = listOf("Prionia", "Dion")
        ),
        EkdromeRoute(
            23, "Prespa Lakes Circuit", "Γύρος Λιμνών Πρεσπών",
            EkdromeRegion.MACEDONIA, listOf(EkdromeTag.SCENIC, EkdromeTag.ASPHALT),
            EkdromeDifficulty.MEDIUM, 80, 4.5f,
            "Tranquil ride around the Prespa Lakes national park on the Albanian-Greek border.",
            "Ήρεμη βόλτα γύρω από τις λίμνες Πρεσπών στα ελληνοαλβανικά σύνορα.",
            40.8500, 21.0833,
            startLocation = "Florina", endLocation = "Agios Germanos",
            waypoints = listOf("Psarades", "Mikrolimni")
        ),

        // ── ATTICA (2 routes) ─────────────────────────────────────
        EkdromeRoute(
            6, "Parnitha Mountain Loop", "Γύρος Πάρνηθας",
            EkdromeRegion.ATTICA, listOf(EkdromeTag.MOTO, EkdromeTag.ASPHALT),
            EkdromeDifficulty.EASY, 55, 4.0f,
            "Quick escape from Athens through forested mountain roads.",
            "Γρήγορη απόδραση από την Αθήνα μέσα από δασωμένους ορεινούς δρόμους.",
            38.1667, 23.7333,
            startLocation = "Athens", endLocation = "Athens",
            waypoints = listOf("Acharnes", "Parnitha Casino", "Vilia")
        ),
        EkdromeRoute(
            24, "Cape Sounion Sunset Run", "Βόλτα Ακρωτήρι Σούνιο",
            EkdromeRegion.ATTICA, listOf(EkdromeTag.SCENIC, EkdromeTag.ASPHALT, EkdromeTag.MOTO),
            EkdromeDifficulty.EASY, 65, 4.4f,
            "Athens Riviera road to the Temple of Poseidon — best at sunset.",
            "Παράκτιος δρόμος Αθηναϊκής Ριβιέρας στον Ναό Ποσειδώνα — ιδανικά στο ηλιοβασίλεμα.",
            37.6503, 24.0247,
            startLocation = "Athens", endLocation = "Cape Sounion",
            waypoints = listOf("Vouliagmeni", "Varkiza", "Lagonisi")
        ),

        // ── THESSALY (2 routes) ───────────────────────────────────
        EkdromeRoute(
            25, "Meteora Monasteries Circuit", "Γύρος Μονών Μετεώρων",
            EkdromeRegion.THESSALY, listOf(EkdromeTag.SCENIC, EkdromeTag.ASPHALT, EkdromeTag.TWISTY),
            EkdromeDifficulty.MEDIUM, 50, 4.9f,
            "Wind through towering rock pillars and clifftop monasteries of Meteora.",
            "Ελίξτε ανάμεσα σε πανύψηλους βραχώδεις πύργους και μοναστήρια στα Μετέωρα.",
            39.7217, 21.6308,
            startLocation = "Kalabaka", endLocation = "Kalabaka",
            waypoints = listOf("Great Meteoron Monastery", "Varlaam Monastery", "Roussanou Monastery")
        ),
        EkdromeRoute(
            26, "Pelion Peninsula Road", "Δρόμος Χερσονήσου Πηλίου",
            EkdromeRegion.THESSALY, listOf(EkdromeTag.TWISTY, EkdromeTag.SCENIC, EkdromeTag.MOTO),
            EkdromeDifficulty.MEDIUM, 100, 4.7f,
            "Lush green roads through chestnut forests and stone villages on the mythical Pelion.",
            "Καταπράσινοι δρόμοι μέσα από καστανοδάση και πέτρινα χωριά στο μυθικό Πήλιο.",
            39.3833, 23.0500,
            startLocation = "Volos", endLocation = "Tsagarada",
            waypoints = listOf("Portaria", "Makrinitsa", "Milies")
        ),

        // ── DODECANESE (2 routes) ─────────────────────────────────
        EkdromeRoute(
            27, "Rhodes Medieval Coast", "Ρόδος Μεσαιωνική Ακτή",
            EkdromeRegion.DODECANESE, listOf(EkdromeTag.SCENIC, EkdromeTag.ASPHALT),
            EkdromeDifficulty.EASY, 80, 4.3f,
            "Full island loop from Rhodes Old Town along turquoise beaches and hilltop castles.",
            "Πλήρης γύρος νησιού από Παλιά Πόλη Ρόδου σε γαλαζοπράσινες παραλίες και κάστρα.",
            36.4344, 28.2178,
            startLocation = "Rhodes Old Town", endLocation = "Rhodes Old Town",
            waypoints = listOf("Lindos", "Monolithos Castle", "Prasonisi")
        ),
        EkdromeRoute(
            28, "Kos Island Explorer", "Εξερεύνηση Κω",
            EkdromeRegion.DODECANESE, listOf(EkdromeTag.MOTO, EkdromeTag.SCENIC),
            EkdromeDifficulty.EASY, 45, 4.1f,
            "Compact island ride past Asklepion ruins, salt lakes, and volcanic beaches.",
            "Σύντομη νησιωτική βόλτα σε Ασκληπιείο, αλυκές και ηφαιστειακές παραλίες.",
            36.8933, 27.0917,
            startLocation = "Kos Town", endLocation = "Kefalos",
            waypoints = listOf("Asklepion", "Tigkaki", "Paradise Beach")
        )
    )
}
