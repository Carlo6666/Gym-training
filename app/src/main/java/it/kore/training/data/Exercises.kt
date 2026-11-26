package it.kore.training.data

data class Exercise(
    val id: String,
    val name: String,
    val muscle: Muscle,
    val category: Category,
    val equipment: Equipment,
    val cues: List<String>,
    val secondary: List<Muscle> = emptyList(),
    /** true if the exercise uses a 1RM (massimale) to compute loads. */
    val tracksOneRm: Boolean = false
)

object Exercises {
    val all: List<Exercise> = listOf(
        // ---------- PETTO ----------
        Exercise("panca_piana", "Panca piana bilanciere", Muscle.PETTO, Category.BASE, Equipment.BILANCIERE,
            listOf("Scapole addotte e depresse, mantienile così per tutta la serie", "Piedi saldi a terra, leggero arco lombare",
                "Scendi controllato fino a sfiorare lo sterno basso, senza rimbalzo", "Gomiti a circa 45-60° dal busto"),
            listOf(Muscle.TRICIPITI, Muscle.SPALLE), tracksOneRm = true),
        Exercise("panca_inclinata_bil", "Panca inclinata bilanciere", Muscle.PETTO, Category.COMPLEMENTARE, Equipment.BILANCIERE,
            listOf("Inclinazione 30°", "Scapole bloccate come in panca piana", "Bilanciere verso la parte alta del petto"),
            listOf(Muscle.SPALLE, Muscle.TRICIPITI)),
        Exercise("panca_inclinata_man", "Panca inclinata manubri", Muscle.PETTO, Category.COMPLEMENTARE, Equipment.MANUBRI,
            listOf("Inclinazione 30°", "Scendi in profondità sentendo l'allungamento del petto", "Spingi convergendo leggermente"),
            listOf(Muscle.SPALLE, Muscle.TRICIPITI)),
        Exercise("panca_piana_man", "Panca piana manubri", Muscle.PETTO, Category.COMPLEMENTARE, Equipment.MANUBRI,
            listOf("Polsi sopra ai gomiti", "Ampia escursione, controlla la discesa"), listOf(Muscle.TRICIPITI)),
        Exercise("dip", "Dip alle parallele", Muscle.PETTO, Category.COMPLEMENTARE, Equipment.CORPO_LIBERO,
            listOf("Busto leggermente inclinato in avanti", "Scendi finché le spalle restano stabili", "Aggiungi zavorra quando superi le 12 ripetizioni"),
            listOf(Muscle.TRICIPITI, Muscle.SPALLE)),
        Exercise("chest_press", "Chest press", Muscle.PETTO, Category.COMPLEMENTARE, Equipment.MACCHINA,
            listOf("Regola il sedile: maniglie all'altezza del petto", "Non staccare le scapole dallo schienale")),
        Exercise("croci_cavi", "Croci ai cavi", Muscle.PETTO, Category.ISOLAMENTO, Equipment.CAVI,
            listOf("Gomiti leggermente flessi e fissi", "Pensa ad abbracciare un albero", "Stringi 1 secondo in chiusura")),
        Exercise("pec_deck", "Pectoral machine", Muscle.PETTO, Category.ISOLAMENTO, Equipment.MACCHINA,
            listOf("Petto in fuori, scapole addotte", "Allungamento controllato in apertura")),
        Exercise("croci_man", "Croci su panca inclinata", Muscle.PETTO, Category.ISOLAMENTO, Equipment.MANUBRI,
            listOf("Apri fino a sentire l'allungamento, non oltre", "Chiudi senza far toccare i manubri")),

        // ---------- DORSO ----------
        Exercise("lat_machine", "Lat machine avanti", Muscle.DORSO, Category.BASE, Equipment.MACCHINA,
            listOf("Petto alto, tira verso lo sterno", "Porta i gomiti verso i fianchi", "Risalita controllata fino ad allungare i dorsali"),
            listOf(Muscle.BICIPITI), tracksOneRm = true),
        Exercise("trazioni", "Trazioni alla sbarra", Muscle.DORSO, Category.BASE, Equipment.CORPO_LIBERO,
            listOf("Parti da braccia distese", "Petto verso la sbarra", "Aggiungi zavorra quando superi le 10 ripetizioni"),
            listOf(Muscle.BICIPITI)),
        Exercise("stacco", "Stacco da terra", Muscle.DORSO, Category.BASE, Equipment.BILANCIERE,
            listOf("Bilanciere a metà piede, tibie vicine", "Schiena neutra, petto alto", "Spingi il pavimento, il bilanciere sale aderente alle gambe",
                "Blocca in alto con i glutei, senza iperestendere"),
            listOf(Muscle.FEMORALI, Muscle.QUADRICIPITI), tracksOneRm = true),
        Exercise("rematore_bil", "Rematore bilanciere", Muscle.DORSO, Category.COMPLEMENTARE, Equipment.BILANCIERE,
            listOf("Busto a circa 45°, schiena neutra", "Tira verso l'ombelico", "Nessuno slancio con le gambe"), listOf(Muscle.BICIPITI)),
        Exercise("rematore_man", "Rematore manubrio", Muscle.DORSO, Category.COMPLEMENTARE, Equipment.MANUBRI,
            listOf("Appoggio su panca", "Gomito verso l'anca", "Allunga bene in basso")),
        Exercise("pulley", "Pulley basso", Muscle.DORSO, Category.COMPLEMENTARE, Equipment.CAVI,
            listOf("Busto fermo", "Tira verso l'addome portando indietro le scapole")),
        Exercise("lat_stretta", "Lat machine presa stretta", Muscle.DORSO, Category.COMPLEMENTARE, Equipment.MACCHINA,
            listOf("Presa neutra", "Tira verso il petto basso")),
        Exercise("pullover_cavo", "Pullover al cavo", Muscle.DORSO, Category.ISOLAMENTO, Equipment.CAVI,
            listOf("Braccia quasi tese", "Porta la barra verso le cosce usando i dorsali")),

        // ---------- SPALLE ----------
        Exercise("military", "Military press", Muscle.SPALLE, Category.BASE, Equipment.BILANCIERE,
            listOf("In piedi, glutei e addome contratti", "Bilanciere sale in linea retta vicino al viso", "Testa avanti a fine spinta"),
            listOf(Muscle.TRICIPITI), tracksOneRm = true),
        Exercise("lento_man", "Lento manubri seduto", Muscle.SPALLE, Category.COMPLEMENTARE, Equipment.MANUBRI,
            listOf("Schienale quasi verticale", "Scendi fino all'altezza delle orecchie"), listOf(Muscle.TRICIPITI)),
        Exercise("alzate_laterali", "Alzate laterali manubri", Muscle.SPALLE, Category.ISOLAMENTO, Equipment.MANUBRI,
            listOf("Gomiti leggermente flessi", "Sali fino all'altezza delle spalle", "Niente slancio")),
        Exercise("alzate_cavo", "Alzate laterali al cavo", Muscle.SPALLE, Category.ISOLAMENTO, Equipment.CAVI,
            listOf("Tensione costante, discesa lenta")),
        Exercise("alzate_post", "Alzate posteriori", Muscle.SPALLE, Category.ISOLAMENTO, Equipment.MANUBRI,
            listOf("Busto flesso o su panca inclinata", "Apri verso l'esterno guidando con i gomiti")),
        Exercise("face_pull", "Face pull", Muscle.SPALLE, Category.ISOLAMENTO, Equipment.CAVI,
            listOf("Corda all'altezza del viso", "Apri le mani ai lati delle orecchie")),

        // ---------- BICIPITI ----------
        Exercise("curl_bil", "Curl bilanciere", Muscle.BICIPITI, Category.COMPLEMENTARE, Equipment.BILANCIERE,
            listOf("Gomiti fermi ai fianchi", "Niente slancio del busto")),
        Exercise("curl_man", "Curl manubri alternato", Muscle.BICIPITI, Category.COMPLEMENTARE, Equipment.MANUBRI,
            listOf("Supina il polso in salita")),
        Exercise("curl_inclinata", "Curl su panca inclinata", Muscle.BICIPITI, Category.ISOLAMENTO, Equipment.MANUBRI,
            listOf("Braccia dietro la linea del busto per allungare il bicipite")),
        Exercise("curl_scott", "Curl alla panca Scott", Muscle.BICIPITI, Category.ISOLAMENTO, Equipment.BILANCIERE,
            listOf("Non distendere completamente di scatto in basso")),
        Exercise("hammer", "Hammer curl", Muscle.BICIPITI, Category.ISOLAMENTO, Equipment.MANUBRI,
            listOf("Presa neutra, gomiti fermi")),
        Exercise("curl_concentrato", "Curl concentrato", Muscle.BICIPITI, Category.ISOLAMENTO, Equipment.MANUBRI,
            listOf("Gomito appoggiato all'interno coscia", "Stringi in alto")),

        // ---------- TRICIPITI ----------
        Exercise("panca_stretta", "Panca presa stretta", Muscle.TRICIPITI, Category.COMPLEMENTARE, Equipment.BILANCIERE,
            listOf("Mani alla larghezza delle spalle", "Gomiti vicini al busto"), listOf(Muscle.PETTO)),
        Exercise("french_press", "French press", Muscle.TRICIPITI, Category.COMPLEMENTARE, Equipment.BILANCIERE,
            listOf("Gomiti fermi verso il soffitto", "Scendi dietro la fronte")),
        Exercise("pushdown", "Push down ai cavi", Muscle.TRICIPITI, Category.ISOLAMENTO, Equipment.CAVI,
            listOf("Gomiti incollati ai fianchi", "Estendi completamente")),
        Exercise("estensioni_sopra", "Estensioni sopra la testa al cavo", Muscle.TRICIPITI, Category.ISOLAMENTO, Equipment.CAVI,
            listOf("Allungamento massimo del capo lungo")),

        // ---------- GAMBE ----------
        Exercise("squat", "Squat bilanciere", Muscle.QUADRICIPITI, Category.BASE, Equipment.BILANCIERE,
            listOf("Bilanciere sui trapezi, piedi larghezza spalle", "Ginocchia in linea con le punte", "Scendi almeno al parallelo con schiena neutra",
                "Spingi con tutto il piede"),
            listOf(Muscle.FEMORALI), tracksOneRm = true),
        Exercise("pressa", "Pressa 45°", Muscle.QUADRICIPITI, Category.COMPLEMENTARE, Equipment.MACCHINA,
            listOf("Bacino incollato allo schienale", "Scendi profondo senza staccare il sedere")),
        Exercise("hack_squat", "Hack squat", Muscle.QUADRICIPITI, Category.COMPLEMENTARE, Equipment.MACCHINA,
            listOf("Piedi medi, discesa controllata")),
        Exercise("affondi", "Affondi con manubri", Muscle.QUADRICIPITI, Category.COMPLEMENTARE, Equipment.MANUBRI,
            listOf("Passo lungo, busto eretto", "Ginocchio posteriore sfiora terra"), listOf(Muscle.FEMORALI)),
        Exercise("leg_extension", "Leg extension", Muscle.QUADRICIPITI, Category.ISOLAMENTO, Equipment.MACCHINA,
            listOf("Stringi 1 secondo in alto")),
        Exercise("sissy", "Sissy squat", Muscle.QUADRICIPITI, Category.ISOLAMENTO, Equipment.CORPO_LIBERO,
            listOf("Ginocchia avanti, bacino esteso", "Usa un appoggio per l'equilibrio")),
        Exercise("stacco_rumeno", "Stacco rumeno", Muscle.FEMORALI, Category.COMPLEMENTARE, Equipment.BILANCIERE,
            listOf("Ginocchia appena flesse", "Bacino indietro, bilanciere aderente alle cosce", "Fermati quando senti l'allungamento dei femorali")),
        Exercise("hip_thrust", "Hip thrust", Muscle.FEMORALI, Category.COMPLEMENTARE, Equipment.BILANCIERE,
            listOf("Mento verso il petto", "Spingi con i talloni, contrai i glutei in alto")),
        Exercise("leg_curl", "Leg curl", Muscle.FEMORALI, Category.ISOLAMENTO, Equipment.MACCHINA,
            listOf("Bacino fermo", "Discesa lenta")),
        Exercise("calf_piedi", "Calf in piedi", Muscle.POLPACCI, Category.ISOLAMENTO, Equipment.MACCHINA,
            listOf("Pausa di 1 secondo in allungamento", "Sali sulle punte al massimo")),
        Exercise("calf_seduto", "Calf seduto", Muscle.POLPACCI, Category.ISOLAMENTO, Equipment.MACCHINA,
            listOf("Ampia escursione, niente rimbalzi")),

        // ---------- ADDOME ----------
        Exercise("crunch_cavo", "Crunch al cavo", Muscle.ADDOME, Category.ISOLAMENTO, Equipment.CAVI,
            listOf("Arrotola la colonna, non tirare con le braccia")),
        Exercise("leg_raise", "Leg raise alla sbarra", Muscle.ADDOME, Category.ISOLAMENTO, Equipment.CORPO_LIBERO,
            listOf("Retroversione del bacino in alto", "Niente oscillazioni"))
    )

    private val byId = all.associateBy { it.id }

    fun get(id: String): Exercise = byId[id] ?: all.first()
    fun find(id: String): Exercise? = byId[id]

    fun alternatives(ex: Exercise): List<Exercise> = all.filter { it.muscle == ex.muscle && it.id != ex.id }

    val oneRmLifts: List<Exercise> get() = all.filter { it.tracksOneRm }
}
