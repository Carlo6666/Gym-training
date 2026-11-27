// KORE Training · dati statici (esercizi, programma, guida). Porting 1:1 dall'app Android.
'use strict';

const SEX = { M: 'Uomo', F: 'Donna' };
const LEVELS = { PRINCIPIANTE: 'Principiante', INTERMEDIO: 'Intermedio', AVANZATO: 'Avanzato' };
const SPLITS = {
  SPLIT_A: { label: 'Split 4 giorni · A', description: 'Ogni giorno ha un distretto principale (petto, dorso, spalle, gambe) e due secondari. Multifrequenza: ogni muscolo lavora 2 volte.' },
  SPLIT_B: { label: 'Split 4 giorni · B', description: 'Accoppiamenti antagonisti: petto+dorso, spalle+gambe. Ogni distretto principale torna due volte nella settimana.' },
  PPL: { label: 'Push · Legs · Pull ×2', description: '6 sedute a rotazione (spinta, gambe, tirata) distribuite su 8-9 giorni. Ideale per chi si allena 4-5 volte.' }
};
const RESPONDERS = {
  NORMALE: { label: 'Normale', surplusMin: 200, surplusMax: 300 },
  HARD_GAINER: { label: 'Hard gainer (fatico a crescere)', surplusMin: 400, surplusMax: 600 },
  INGRASSO_FACILE: { label: 'Tendo ad ingrassare', surplusMin: 150, surplusMax: 200 }
};
const MUSCLES = {
  PETTO: 'Petto', DORSO: 'Dorso', SPALLE: 'Spalle', BICIPITI: 'Bicipiti', TRICIPITI: 'Tricipiti',
  QUADRICIPITI: 'Quadricipiti', FEMORALI: 'Femorali e glutei', POLPACCI: 'Polpacci', ADDOME: 'Addome'
};
const CATEGORIES = {
  BASE: { label: 'Fondamentale', short: 'BASE' },
  COMPLEMENTARE: { label: 'Complementare', short: 'COMP' },
  ISOLAMENTO: { label: 'Isolamento', short: 'ISO' }
};
const EQUIPMENT = {
  BILANCIERE: { label: 'Bilanciere', step: 2.5 },
  MANUBRI: { label: 'Manubri', step: 2.0 },
  MACCHINA: { label: 'Macchina', step: 2.5 },
  CAVI: { label: 'Cavi', step: 2.5 },
  CORPO_LIBERO: { label: 'Corpo libero', step: 1.25 }
};
const TECHNIQUES = {
  NESSUNA: { label: '', howTo: '' },
  REST_PAUSE: { label: 'Rest-pause', howTo: 'Arriva a cedimento, riposa 15-20 secondi e strappa altre ripetizioni con lo stesso carico. Ripeti per 2 mini-serie.' },
  STRIPPING: { label: 'Stripping', howTo: 'Arriva a cedimento, scala subito il carico del 20-25% e continua fino a nuovo cedimento. Ripeti una seconda volta.' },
  BACK_OFF: { label: 'Back-off', howTo: "Dopo le serie pesanti, togli circa il 15-20% del carico ed esegui una serie extra a buffer 1-0 con più ripetizioni." },
  PARZIALI: { label: 'Ripetizioni parziali', howTo: 'Al cedimento completo aggiungi 3-5 ripetizioni parziali nella porzione di movimento che riesci ancora a completare.' },
  ISOMETRIA: { label: 'Tenuta isometrica', howTo: "All'ultima ripetizione blocca il carico nel punto di massima contrazione per 10-15 secondi." }
};

function E(id, name, muscle, category, equipment, cues, secondary = [], tracksOneRm = false) {
  return { id, name, muscle, category, equipment, cues, secondary, tracksOneRm };
}
function s(key, category, ...options) { return { key, category, options }; }
function D(name, muscles, slots) { return { name, muscles, slots }; }
function G(title, body) { return { title, body }; }

const EXERCISES = [

        
        E("panca_piana", "Panca piana bilanciere", "PETTO", "BASE", "BILANCIERE",
            ["Scapole addotte e depresse, mantienile così per tutta la serie", "Piedi saldi a terra, leggero arco lombare",
                "Scendi controllato fino a sfiorare lo sterno basso, senza rimbalzo", "Gomiti a circa 45-60° dal busto"],
            ["TRICIPITI", "SPALLE"], true),
        E("panca_inclinata_bil", "Panca inclinata bilanciere", "PETTO", "COMPLEMENTARE", "BILANCIERE",
            ["Inclinazione 30°", "Scapole bloccate come in panca piana", "Bilanciere verso la parte alta del petto"],
            ["SPALLE", "TRICIPITI"]),
        E("panca_inclinata_man", "Panca inclinata manubri", "PETTO", "COMPLEMENTARE", "MANUBRI",
            ["Inclinazione 30°", "Scendi in profondità sentendo l'allungamento del petto", "Spingi convergendo leggermente"],
            ["SPALLE", "TRICIPITI"]),
        E("panca_piana_man", "Panca piana manubri", "PETTO", "COMPLEMENTARE", "MANUBRI",
            ["Polsi sopra ai gomiti", "Ampia escursione, controlla la discesa"], ["TRICIPITI"]),
        E("dip", "Dip alle parallele", "PETTO", "COMPLEMENTARE", "CORPO_LIBERO",
            ["Busto leggermente inclinato in avanti", "Scendi finché le spalle restano stabili", "Aggiungi zavorra quando superi le 12 ripetizioni"],
            ["TRICIPITI", "SPALLE"]),
        E("chest_press", "Chest press", "PETTO", "COMPLEMENTARE", "MACCHINA",
            ["Regola il sedile: maniglie all'altezza del petto", "Non staccare le scapole dallo schienale"]),
        E("croci_cavi", "Croci ai cavi", "PETTO", "ISOLAMENTO", "CAVI",
            ["Gomiti leggermente flessi e fissi", "Pensa ad abbracciare un albero", "Stringi 1 secondo in chiusura"]),
        E("pec_deck", "Pectoral machine", "PETTO", "ISOLAMENTO", "MACCHINA",
            ["Petto in fuori, scapole addotte", "Allungamento controllato in apertura"]),
        E("croci_man", "Croci su panca inclinata", "PETTO", "ISOLAMENTO", "MANUBRI",
            ["Apri fino a sentire l'allungamento, non oltre", "Chiudi senza far toccare i manubri"]),

        
        E("lat_machine", "Lat machine avanti", "DORSO", "BASE", "MACCHINA",
            ["Petto alto, tira verso lo sterno", "Porta i gomiti verso i fianchi", "Risalita controllata fino ad allungare i dorsali"],
            ["BICIPITI"], true),
        E("trazioni", "Trazioni alla sbarra", "DORSO", "BASE", "CORPO_LIBERO",
            ["Parti da braccia distese", "Petto verso la sbarra", "Aggiungi zavorra quando superi le 10 ripetizioni"],
            ["BICIPITI"]),
        E("stacco", "Stacco da terra", "DORSO", "BASE", "BILANCIERE",
            ["Bilanciere a metà piede, tibie vicine", "Schiena neutra, petto alto", "Spingi il pavimento, il bilanciere sale aderente alle gambe",
                "Blocca in alto con i glutei, senza iperestendere"],
            ["FEMORALI", "QUADRICIPITI"], true),
        E("rematore_bil", "Rematore bilanciere", "DORSO", "COMPLEMENTARE", "BILANCIERE",
            ["Busto a circa 45°, schiena neutra", "Tira verso l'ombelico", "Nessuno slancio con le gambe"], ["BICIPITI"]),
        E("rematore_man", "Rematore manubrio", "DORSO", "COMPLEMENTARE", "MANUBRI",
            ["Appoggio su panca", "Gomito verso l'anca", "Allunga bene in basso"]),
        E("pulley", "Pulley basso", "DORSO", "COMPLEMENTARE", "CAVI",
            ["Busto fermo", "Tira verso l'addome portando indietro le scapole"]),
        E("lat_stretta", "Lat machine presa stretta", "DORSO", "COMPLEMENTARE", "MACCHINA",
            ["Presa neutra", "Tira verso il petto basso"]),
        E("pullover_cavo", "Pullover al cavo", "DORSO", "ISOLAMENTO", "CAVI",
            ["Braccia quasi tese", "Porta la barra verso le cosce usando i dorsali"]),

        
        E("military", "Military press", "SPALLE", "BASE", "BILANCIERE",
            ["In piedi, glutei e addome contratti", "Bilanciere sale in linea retta vicino al viso", "Testa avanti a fine spinta"],
            ["TRICIPITI"], true),
        E("lento_man", "Lento manubri seduto", "SPALLE", "COMPLEMENTARE", "MANUBRI",
            ["Schienale quasi verticale", "Scendi fino all'altezza delle orecchie"], ["TRICIPITI"]),
        E("alzate_laterali", "Alzate laterali manubri", "SPALLE", "ISOLAMENTO", "MANUBRI",
            ["Gomiti leggermente flessi", "Sali fino all'altezza delle spalle", "Niente slancio"]),
        E("alzate_cavo", "Alzate laterali al cavo", "SPALLE", "ISOLAMENTO", "CAVI",
            ["Tensione costante, discesa lenta"]),
        E("alzate_post", "Alzate posteriori", "SPALLE", "ISOLAMENTO", "MANUBRI",
            ["Busto flesso o su panca inclinata", "Apri verso l'esterno guidando con i gomiti"]),
        E("face_pull", "Face pull", "SPALLE", "ISOLAMENTO", "CAVI",
            ["Corda all'altezza del viso", "Apri le mani ai lati delle orecchie"]),

        
        E("curl_bil", "Curl bilanciere", "BICIPITI", "COMPLEMENTARE", "BILANCIERE",
            ["Gomiti fermi ai fianchi", "Niente slancio del busto"]),
        E("curl_man", "Curl manubri alternato", "BICIPITI", "COMPLEMENTARE", "MANUBRI",
            ["Supina il polso in salita"]),
        E("curl_inclinata", "Curl su panca inclinata", "BICIPITI", "ISOLAMENTO", "MANUBRI",
            ["Braccia dietro la linea del busto per allungare il bicipite"]),
        E("curl_scott", "Curl alla panca Scott", "BICIPITI", "ISOLAMENTO", "BILANCIERE",
            ["Non distendere completamente di scatto in basso"]),
        E("hammer", "Hammer curl", "BICIPITI", "ISOLAMENTO", "MANUBRI",
            ["Presa neutra, gomiti fermi"]),
        E("curl_concentrato", "Curl concentrato", "BICIPITI", "ISOLAMENTO", "MANUBRI",
            ["Gomito appoggiato all'interno coscia", "Stringi in alto"]),

        
        E("panca_stretta", "Panca presa stretta", "TRICIPITI", "COMPLEMENTARE", "BILANCIERE",
            ["Mani alla larghezza delle spalle", "Gomiti vicini al busto"], ["PETTO"]),
        E("french_press", "French press", "TRICIPITI", "COMPLEMENTARE", "BILANCIERE",
            ["Gomiti fermi verso il soffitto", "Scendi dietro la fronte"]),
        E("pushdown", "Push down ai cavi", "TRICIPITI", "ISOLAMENTO", "CAVI",
            ["Gomiti incollati ai fianchi", "Estendi completamente"]),
        E("estensioni_sopra", "Estensioni sopra la testa al cavo", "TRICIPITI", "ISOLAMENTO", "CAVI",
            ["Allungamento massimo del capo lungo"]),

        
        E("squat", "Squat bilanciere", "QUADRICIPITI", "BASE", "BILANCIERE",
            ["Bilanciere sui trapezi, piedi larghezza spalle", "Ginocchia in linea con le punte", "Scendi almeno al parallelo con schiena neutra",
                "Spingi con tutto il piede"],
            ["FEMORALI"], true),
        E("pressa", "Pressa 45°", "QUADRICIPITI", "COMPLEMENTARE", "MACCHINA",
            ["Bacino incollato allo schienale", "Scendi profondo senza staccare il sedere"]),
        E("hack_squat", "Hack squat", "QUADRICIPITI", "COMPLEMENTARE", "MACCHINA",
            ["Piedi medi, discesa controllata"]),
        E("affondi", "Affondi con manubri", "QUADRICIPITI", "COMPLEMENTARE", "MANUBRI",
            ["Passo lungo, busto eretto", "Ginocchio posteriore sfiora terra"], ["FEMORALI"]),
        E("leg_extension", "Leg extension", "QUADRICIPITI", "ISOLAMENTO", "MACCHINA",
            ["Stringi 1 secondo in alto"]),
        E("sissy", "Sissy squat", "QUADRICIPITI", "ISOLAMENTO", "CORPO_LIBERO",
            ["Ginocchia avanti, bacino esteso", "Usa un appoggio per l'equilibrio"]),
        E("stacco_rumeno", "Stacco rumeno", "FEMORALI", "COMPLEMENTARE", "BILANCIERE",
            ["Ginocchia appena flesse", "Bacino indietro, bilanciere aderente alle cosce", "Fermati quando senti l'allungamento dei femorali"]),
        E("hip_thrust", "Hip thrust", "FEMORALI", "COMPLEMENTARE", "BILANCIERE",
            ["Mento verso il petto", "Spingi con i talloni, contrai i glutei in alto"]),
        E("leg_curl", "Leg curl", "FEMORALI", "ISOLAMENTO", "MACCHINA",
            ["Bacino fermo", "Discesa lenta"]),
        E("calf_piedi", "Calf in piedi", "POLPACCI", "ISOLAMENTO", "MACCHINA",
            ["Pausa di 1 secondo in allungamento", "Sali sulle punte al massimo"]),
        E("calf_seduto", "Calf seduto", "POLPACCI", "ISOLAMENTO", "MACCHINA",
            ["Ampia escursione, niente rimbalzi"]),

        
        E("crunch_cavo", "Crunch al cavo", "ADDOME", "ISOLAMENTO", "CAVI",
            ["Arrotola la colonna, non tirare con le braccia"]),
        E("leg_raise", "Leg raise alla sbarra", "ADDOME", "ISOLAMENTO", "CORPO_LIBERO",
            ["Retroversione del bacino in alto", "Niente oscillazioni"])
    
];

const TEMPLATES = {
  SPLIT_A: [

        D("Petto", "Petto · Spalle · Bicipiti", [
            s("A1_base", "BASE", "panca_piana"),
            s("A1_comp", "COMPLEMENTARE", "panca_inclinata_man", "panca_inclinata_bil", "dip"),
            s("A1_iso", "ISOLAMENTO", "croci_cavi", "pec_deck", "croci_man"),
            s("A1_spalle", "ISOLAMENTO", "alzate_laterali", "alzate_cavo", "alzate_laterali"),
            s("A1_bic", "COMPLEMENTARE", "curl_bil", "curl_man", "curl_bil"),
            s("A1_bic2", "ISOLAMENTO", "curl_inclinata", "curl_scott", "curl_concentrato")
        ]),
        D("Dorso", "Dorso · Petto · Tricipiti · Femorali", [
            s("A2_base", "BASE", "lat_machine"),
            s("A2_comp", "COMPLEMENTARE", "rematore_bil", "rematore_man", "pulley"),
            s("A2_comp2", "COMPLEMENTARE", "pulley", "lat_stretta", "rematore_man"),
            s("A2_petto", "ISOLAMENTO", "pec_deck", "croci_cavi", "croci_man"),
            s("A2_tri", "COMPLEMENTARE", "french_press", "panca_stretta", "french_press"),
            s("A2_tri2", "ISOLAMENTO", "pushdown", "estensioni_sopra", "pushdown"),
            s("A2_fem", "ISOLAMENTO", "leg_curl")
        ]),
        D("Spalle", "Spalle · Petto · Bicipiti", [
            s("A3_base", "BASE", "military"),
            s("A3_comp", "COMPLEMENTARE", "lento_man"),
            s("A3_lat", "ISOLAMENTO", "alzate_laterali", "alzate_cavo", "alzate_laterali"),
            s("A3_post", "ISOLAMENTO", "alzate_post", "face_pull", "alzate_post"),
            s("A3_petto", "COMPLEMENTARE", "dip", "chest_press", "panca_piana_man"),
            s("A3_bic", "ISOLAMENTO", "hammer", "curl_concentrato", "hammer")
        ]),
        D("Gambe", "Gambe · Dorso · Tricipiti", [
            s("A4_base", "BASE", "squat"),
            s("A4_post", "COMPLEMENTARE", "stacco_rumeno", "hip_thrust", "stacco_rumeno"),
            s("A4_quad", "COMPLEMENTARE", "pressa", "hack_squat", "affondi"),
            s("A4_ext", "ISOLAMENTO", "leg_extension", "leg_extension", "sissy"),
            s("A4_calf", "ISOLAMENTO", "calf_piedi", "calf_seduto", "calf_piedi"),
            s("A4_dorso", "COMPLEMENTARE", "lat_stretta", "trazioni", "pulley"),
            s("A4_tri", "ISOLAMENTO", "estensioni_sopra", "pushdown", "estensioni_sopra")
        ])
    
  ],
  SPLIT_B: [

        D("Petto & Dorso", "Petto · Dorso · Tricipiti", [
            s("B1_base", "BASE", "panca_piana"),
            s("B1_dorso", "COMPLEMENTARE", "rematore_bil", "rematore_man", "pulley"),
            s("B1_petto", "COMPLEMENTARE", "panca_inclinata_man", "dip", "panca_inclinata_bil"),
            s("B1_dorso2", "COMPLEMENTARE", "pulley", "lat_stretta", "rematore_man"),
            s("B1_iso", "ISOLAMENTO", "croci_cavi", "pec_deck", "croci_man"),
            s("B1_tri", "ISOLAMENTO", "pushdown", "estensioni_sopra", "pushdown")
        ]),
        D("Spalle & Gambe", "Spalle · Bicipiti · Gambe", [
            s("B2_base", "BASE", "military"),
            s("B2_lat", "ISOLAMENTO", "alzate_laterali", "alzate_cavo", "alzate_laterali"),
            s("B2_quad", "COMPLEMENTARE", "pressa", "hack_squat", "affondi"),
            s("B2_fem", "ISOLAMENTO", "leg_curl"),
            s("B2_bic", "COMPLEMENTARE", "curl_bil", "curl_man", "curl_bil"),
            s("B2_calf", "ISOLAMENTO", "calf_piedi", "calf_seduto", "calf_piedi")
        ]),
        D("Dorso & Petto", "Dorso · Petto · Tricipiti", [
            s("B3_base", "BASE", "lat_machine"),
            s("B3_petto", "COMPLEMENTARE", "panca_inclinata_bil", "panca_inclinata_man", "chest_press"),
            s("B3_dorso", "COMPLEMENTARE", "rematore_man", "pulley", "rematore_bil"),
            s("B3_petto2", "COMPLEMENTARE", "dip", "panca_piana_man", "dip"),
            s("B3_tri", "COMPLEMENTARE", "french_press", "panca_stretta", "french_press"),
            s("B3_abs", "ISOLAMENTO", "crunch_cavo", "leg_raise", "crunch_cavo")
        ]),
        D("Gambe & Spalle", "Gambe · Spalle · Bicipiti", [
            s("B4_base", "BASE", "squat"),
            s("B4_post", "COMPLEMENTARE", "stacco_rumeno", "hip_thrust", "stacco_rumeno"),
            s("B4_ext", "ISOLAMENTO", "leg_extension", "sissy", "leg_extension"),
            s("B4_spalle", "COMPLEMENTARE", "lento_man"),
            s("B4_post_d", "ISOLAMENTO", "alzate_post", "face_pull", "alzate_post"),
            s("B4_bic", "ISOLAMENTO", "curl_inclinata", "curl_scott", "hammer"),
            s("B4_calf", "ISOLAMENTO", "calf_seduto", "calf_piedi", "calf_seduto")
        ])
    
  ],
  PPL: [

        D("Push 1", "Petto · Spalle · Tricipiti", [
            s("P1_base", "BASE", "panca_piana"),
            s("P1_petto", "COMPLEMENTARE", "panca_inclinata_man", "panca_inclinata_bil", "dip"),
            s("P1_spalle", "COMPLEMENTARE", "lento_man"),
            s("P1_lat", "ISOLAMENTO", "alzate_laterali", "alzate_cavo", "alzate_laterali"),
            s("P1_iso", "ISOLAMENTO", "croci_cavi", "pec_deck", "croci_man"),
            s("P1_tri", "ISOLAMENTO", "pushdown", "estensioni_sopra", "pushdown")
        ]),
        D("Legs 1", "Quadricipiti · Femorali · Polpacci", [
            s("L1_base", "BASE", "squat"),
            s("L1_quad", "COMPLEMENTARE", "pressa", "hack_squat", "pressa"),
            s("L1_fem", "COMPLEMENTARE", "stacco_rumeno", "hip_thrust", "stacco_rumeno"),
            s("L1_ext", "ISOLAMENTO", "leg_extension", "sissy", "leg_extension"),
            s("L1_calf", "ISOLAMENTO", "calf_piedi", "calf_seduto", "calf_piedi")
        ]),
        D("Pull 1", "Dorso · Bicipiti", [
            s("U1_base", "BASE", "lat_machine"),
            s("U1_row", "COMPLEMENTARE", "rematore_bil", "rematore_man", "rematore_bil"),
            s("U1_row2", "COMPLEMENTARE", "pulley", "lat_stretta", "pulley"),
            s("U1_post", "ISOLAMENTO", "face_pull", "alzate_post", "face_pull"),
            s("U1_bic", "COMPLEMENTARE", "curl_bil", "curl_man", "curl_bil"),
            s("U1_bic2", "ISOLAMENTO", "hammer", "curl_concentrato", "hammer")
        ]),
        D("Push 2", "Spalle · Petto · Tricipiti", [
            s("P2_base", "BASE", "military"),
            s("P2_petto", "COMPLEMENTARE", "panca_inclinata_bil", "chest_press", "panca_piana_man"),
            s("P2_dip", "COMPLEMENTARE", "dip"),
            s("P2_lat", "ISOLAMENTO", "alzate_cavo", "alzate_laterali", "alzate_cavo"),
            s("P2_iso", "ISOLAMENTO", "pec_deck", "croci_man", "croci_cavi"),
            s("P2_tri", "COMPLEMENTARE", "french_press", "panca_stretta", "french_press")
        ]),
        D("Legs 2", "Femorali · Quadricipiti · Addome", [
            s("L2_base", "BASE", "stacco"),
            s("L2_quad", "COMPLEMENTARE", "hack_squat", "affondi", "hack_squat"),
            s("L2_fem", "ISOLAMENTO", "leg_curl"),
            s("L2_aff", "COMPLEMENTARE", "affondi", "pressa", "affondi"),
            s("L2_calf", "ISOLAMENTO", "calf_seduto", "calf_piedi", "calf_seduto"),
            s("L2_abs", "ISOLAMENTO", "crunch_cavo", "leg_raise", "crunch_cavo")
        ]),
        D("Pull 2", "Dorso · Bicipiti · Spalle post.", [
            s("U2_base", "BASE", "trazioni"),
            s("U2_row", "COMPLEMENTARE", "rematore_man", "pulley", "rematore_man"),
            s("U2_lat", "COMPLEMENTARE", "lat_stretta", "lat_machine", "lat_stretta"),
            s("U2_post", "ISOLAMENTO", "alzate_post", "face_pull", "alzate_post"),
            s("U2_bic", "ISOLAMENTO", "curl_inclinata", "curl_scott", "curl_inclinata"),
            s("U2_bic2", "ISOLAMENTO", "curl_concentrato", "hammer", "curl_concentrato")
        ])
    
  ]
};

const PHASES = [
  { name: 'Forza & Ipertrofia', short: 'FASE 1', goal: 'Costruire le basi di forza sui fondamentali e alzare i massimali.', details: [
    'Fondamentali a percentuale del massimale: prima sale il volume a carico costante, poi sale il carico e scendono le ripetizioni.',
    'Complementari in doppia progressione (prima le ripetizioni, poi il carico).',
    'Massimali da testare all\'inizio e alla fine della fase.',
    'Ogni mesociclo dura 5 settimane di carico + 1 di scarico.'] },
  { name: 'Trasformazione Ipertrofica', short: 'FASE 2', goal: 'Più volume su complementari e isolamento, serie portate al cedimento.', details: [
    'Fondamentali in range 6-8 con buffer 2 → 1.',
    'Più serie per i complementari, ultima serie a cedimento concentrico.',
    'Isolamento a cedimento con rest-pause nelle settimane più dure.',
    'Rotazione degli esercizi complementari rispetto alla fase 1, i fondamentali restano.'] },
  { name: 'Qualità Muscolare', short: 'FASE 3', goal: 'Pompaggio, densità e tecniche di intensità per rifinire il fisico.', details: [
    'Recuperi che si accorciano settimana dopo settimana (progressione di densità).',
    'Back-off set dopo i fondamentali, stripping e tenute isometriche sull\'isolamento.',
    'Cedimento frequente su complementari e isolamento.',
    'Fase adatta anche a un periodo di definizione.'] }
];

const GUIDE = [

    G("La piramide delle priorità", [
        "Alla base c'è la costanza: un programma discreto seguito per mesi batte un programma perfetto abbandonato dopo tre settimane. Allenarsi con piacere aiuta a restare costanti.",
        "Sopra viene l'organizzazione: si parte dall'obiettivo a lungo termine e si costruisce a ritroso il percorso, fase per fase.",
        "Poi la prestazione: ogni seduta cerca di aggiungere qualcosa (un chilo, una ripetizione, una serie).",
        "Infine l'equilibrio tra stimolo e recupero: stimoli troppo forti o ravvicinati accumulano fatica, stimoli troppo blandi non producono adattamento."
    ]),
    G("I tre stimoli dell'ipertrofia", [
        "Tensione meccanica: serve un carico adeguato e ripetizioni vicine al cedimento.",
        "Stress metabolico: si ottiene con serie lunghe (oltre 15-20 secondi sotto tensione), più serie e recuperi brevi.",
        "Danno muscolare: è una conseguenza dell'allenamento intenso, non va cercato di proposito."
    ]),
    G("Categorie di esercizi", [
        "Fondamentali (BASE): squat, stacco, panca, lat machine, military press, dip. Carichi alti, poche ripetizioni, all'inizio della seduta.",
        "Complementari (COMP): multiarticolari o monoarticolari a carico medio, progressione più lenta.",
        "Isolamento (ISO): carichi leggeri e ripetizioni alte, a fine seduta, spesso a cedimento.",
        "Mantieni almeno due esercizi \"bersaglio\" per muscolo a lungo per misurarne i progressi; gli altri possono ruotare tra le fasi."
    ]),
    G("Intensità: le fasce di ripetizioni", [
        "Carico alto: meno di 6 ripetizioni. Recupero 2'30\"-5'.",
        "Carico medio: 6-12 ripetizioni. Recupero 1'30\"-2'30\".",
        "Carico basso: oltre 12 ripetizioni. Recupero 30\"-1'30\".",
        "Per la massa circa il 65-75% delle serie settimanali sta nella fascia media, il resto si divide tra carichi alti e bassi."
    ]),
    G("Volume settimanale", [
        "Mantenimento: circa 8-10 serie a settimana per gruppo muscolare.",
        "Minimo efficace: 10-15 serie.",
        "Zona ottimale: 15-25 serie (spingi verso l'alto per i muscoli carenti, resta verso il basso per quelli già sviluppati).",
        "Oltre 25-30 serie il recupero non regge e il volume diventa controproducente.",
        "Meglio distribuire il volume su 2 sedute (multifrequenza): le ultime serie restano di qualità."
    ]),
    G("RIR, RPE e cedimento", [
        "RIR (o buffer) = ripetizioni che avresti ancora potuto fare. RPE = 10 − RIR.",
        "Fondamentali pesanti: sempre con buffer, mai a cedimento, per tecnica e sicurezza.",
        "Complementari: 1-2 di buffer nelle prime serie, l'ultima arriva naturalmente al cedimento.",
        "Isolamento: cedimento su tutte le serie.",
        "Cedimento tecnico → concentrico → eccentrico: l'obiettivo è avvicinarsi al cedimento concentrico mantenendo una tecnica pulita."
    ]),
    G("Il tempo di esecuzione", [
        "Si scrive con quattro numeri: eccentrica · pausa in basso · concentrica · pausa in alto.",
        "Standard 2-0-1-0: discesa controllata in due secondi, salita esplosiva, nessuna pausa."
    ]),
    G("Modelli di progressione", [
        "Carico: stesse serie e ripetizioni, il peso sale ogni settimana (es. 4×6 con +2,5 kg).",
        "Carico con riduzione delle ripetizioni: 4×6 → 4×5 → 4×4… mentre il peso sale.",
        "Volume: a carico fisso aumentano le ripetizioni (3×8 → 3×10) o le serie (3×6 → 6×6).",
        "Doppia progressione: prima porti tutte le serie al limite alto del range, poi aumenti il carico e riparti dal basso.",
        "Densità: stesso lavoro con recuperi più brevi (es. da 120\" a 80\").",
        "Intensità percepita: stesso carico, buffer che scende da 2 a 0 e poi tecniche come parziali, stripping o isometria.",
        "Frequenza: lo stesso volume viene spezzato su più sedute."
    ]),
    G("Periodizzazione", [
        "Seduta → microciclo (circa una settimana) → mesociclo (5-6 settimane) → macrociclo (un anno).",
        "Ogni mesociclo si chiude con una settimana di scarico per smaltire la fatica accumulata.",
        "Fase 1 Forza & Ipertrofia: prima sale il volume a carico costante, poi sale il carico. Massimali testati a inizio e fine fase.",
        "Fase 2 Trasformazione Ipertrofica: più complementari e isolamento, più serie a cedimento, tecniche da bodybuilding.",
        "Fase 3 Qualità Muscolare: pompaggio, cedimento frequente, stripping, rest-pause e back-off."
    ]),
    G("Alimentazione in massa", [
        "Parti dal fabbisogno giornaliero (TDEE) e aggiungi +200/300 kcal (+400/600 se fatichi a crescere, +150/200 se ingrassi facilmente).",
        "Proteine 1,4-2 g per kg, grassi 0,8-1,2 g per kg, il resto carboidrati.",
        "Calorie cicliche: circa +10% nei giorni di allenamento, meno nei giorni di riposo, stesso totale settimanale.",
        "Pesati ogni mattina e confronta le medie settimanali: l'obiettivo è circa +0,5% a settimana."
    ])

];
