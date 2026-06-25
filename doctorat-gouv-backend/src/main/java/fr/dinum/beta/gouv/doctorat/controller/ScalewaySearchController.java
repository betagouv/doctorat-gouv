package fr.dinum.beta.gouv.doctorat.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.dinum.beta.gouv.doctorat.dto.PropositionTheseDto;
import fr.dinum.beta.gouv.doctorat.dto.VectorSearchHit;
import fr.dinum.beta.gouv.doctorat.enums.DomaineScientifique;
import fr.dinum.beta.gouv.doctorat.enums.RegionsFrance;
import fr.dinum.beta.gouv.doctorat.repository.PropositionTheseRepository;
import fr.dinum.beta.gouv.doctorat.repository.SujetEmbeddingRepository;
import fr.dinum.beta.gouv.doctorat.service.EmbeddingIndexationService;
import fr.dinum.beta.gouv.doctorat.service.PropositionTheseService;
import fr.dinum.beta.gouv.doctorat.service.SearchRerankerService;
import fr.dinum.beta.gouv.doctorat.service.VectorSearchService;

@RestController
@RequestMapping("/api/scaleway")
public class ScalewaySearchController {

	private static final Logger log = LoggerFactory.getLogger(ScalewaySearchController.class);

	// Seuils de pertinence basés sur le score composite (vectoriel + lexical)
	// Calibrés avec la distribution observée des scores (juin 2026)
	// Version 1 (juin 2026) : TRES_PERTINENT >= 0.78
	// Version 2 (juin 2026) : TRES_PERTINENT >= 0.91 (seuil relevé après test terrain)
	private static final double SEUIL_TRES_PERTINENT = 0.91;
	private static final double SEUIL_PERTINENT = 0.70;
	private static final double SEUIL_FAIBLEMENT_PERTINENT = 0.60;

	// Mots-clés géographiques FR/EN pour détecter une intention de localisation
	private static final String[] GEO_KEYWORDS = {
		// Français
		"proche", "près", "pas loin", "autour", "alentours", "voisinage",
		"proximité", "proximite", "à coté", "à côté", "aux environs",
		"secteur de", "zone de", "du côté de",
		// English
		"near", "close to", "around", "nearby", "vicinity", "close by",
		"not far", "located in", "located near", "situated in"
	};

	// Détecte les prépositions suivies d'un nom propre (ville, région…)
	// Ex: "à Paris", "dans le Var", "en Île-de-France"
	// NOTE: "sur" est volontairement exclu car trop ambigu (ex: "sujet sur l'IA")
	private static final Pattern GEO_PREPOSITION_PATTERN = Pattern.compile(
		"(?:^|\\s)(?:à|aux|dans|vers|en)\\s+(?:le\\s+|la\\s+|l'|les\\s+)?[A-ZÀ-Ÿ][A-Za-zÀ-ÿ-]+(?:\\s|$)"
	);

	// Extrait le nom de ville après un mot-clé de proximité (fiable)
	// Ex: "proche de Paris" → "Paris"
	// Non ancré : on cherche la dernière occurrence dans toute la requête
	private static final Pattern GEO_CITY_PROXIMITY_PATTERN = Pattern.compile(
		"(?:proche|proches|près|pas loin|autour|voisinage|proximité|proximite|à coté|à côté|aux environs)\\s+(?:de|d')\\s*([A-ZÀ-Ÿ][A-Za-zÀ-ÿ-]+(?:\\s*-\\s*[A-ZÀ-Ÿ][A-Za-zÀ-ÿ]+)?)"
	);

	// Extrait le nom de ville après une préposition simple (moins fiable)
	// Ex: "à Paris", "dans le Var", "en Bretagne"
	// Non ancré : on cherche la dernière occurrence dans toute la requête
	private static final Pattern GEO_CITY_PREPOSITION_PATTERN = Pattern.compile(
		"(?:à|aux|dans|vers|en)\\s+(?:le\\s+|la\\s+|l'|les\\s+)?([A-ZÀ-Ÿ][A-Za-zÀ-ÿ-]+(?:\\s*-\\s*[A-ZÀ-Ÿ][A-Za-zÀ-ÿ]+)?)"
	);

	// Mots qui ne doivent PAS être considérés comme des localisations (safety net)
	private static final java.util.Set<String> NON_LOCATION_WORDS = java.util.Set.of(
		"ia", "ai", "ml", "deep", "data", "big", "climat", "climatique",
		"climatiques", "environnement", "numerique", "numérique", "digital",
		"sante", "santé", "biologie", "medecine", "médecine", "chimie",
		"physique", "mathematiques", "mathématiques", "economie", "économie",
		"droit", "histoire", "sociologie", "psychologie", "philosophie",
		"education", "éducation", "formation", "apprentissage",
		"informatique", "science", "sciences", "recherche", "these", "thèse"
	);

	// Liste blanche des villes et régions françaises (DOM-TOM inclus)
	// Un mot capturé après un marqueur de localisation n'est considéré comme
	// intention de localisation que s'il figure dans cette liste.
	private static final Set<String> FRENCH_LOCATIONS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
		// ---- Régions métropolitaines ----
		"auvergne-rhône-alpes", "auvergne rhone alpes",
		"bourgogne-franche-comte", "bourgogne-franche-comté", "bourgogne franche comte", "bourgogne franche comté",
		"bretagne",
		"centre-val de loire", "centre val de loire",
		"corse",
		"grand est",
		"hauts-de-france", "hauts de france",
		"ile-de-france", "ile de france", "île-de-france", "île de france",
		"normandie",
		"nouvelle-aquitaine", "nouvelle aquitaine",
		"occitanie",
		"pays de la loire",
		"provence-alpes-cote d'azur", "provence-alpes-côte d'azur", "paca",

		// ---- DOM-TOM (régions et collectivités) ----
		"guadeloupe", "martinique", "guyane", "guyane française",
		"la réunion", "la reunion", "réunion", "reunion",
		"mayotte",
		"nouvelle-caledonie", "nouvelle-calédonie", "nouvelle caledonie", "nouvelle calédonie",
		"polynésie française", "polynésie francaise", "polynesie française", "polynesie francaise",
		"polynesie", "polynésie",
		"wallis-et-futuna", "wallis et futuna",
		"saint-pierre-et-miquelon", "saint pierre et miquelon",
		"saint-barthelemy", "saint-barthélemy", "saint barthelemy", "saint barthélemy",
		"saint-martin", "saint martin",
		"terres australes et antarctiques françaises", "taaf",

		// ---- Auvergne-Rhône-Alpes ----
		"lyon", "villeurbanne", "venissieux", "vénissieux", "bron", "caluire-et-cuire", "saint-priest",
		"vaulx-en-velin", "saint-etienne", "saint-étienne", "grenoble",
		"clermont-ferrand", "chambery", "chambéry", "annecy", "valence",
		"bourg-en-bresse", "aix-les-bains", "annemasse", "thanon-les-bains", "thonon-les-bains",
		"montelimar", "montélimar", "romans-sur-isere", "romans-sur-isère",
		"vienne", "roanne", "mâcon", "macôn", "macon",
		"bourgoin-jallieu", "saint-chamond", "firminy",
		"oyonnax", "albertville", "ambérieu-en-bugey", "amberieu-en-bugey",
		"le puy-en-velay", "aurillac", "montlucon", "montluçon",
		"riom", "issoire", "vichy",
		"vizille", "echirolles", "échirolles", "meylan", "fontaine",
		"saint-martin-d'heres", "saint-martin-d'hères",
		"la tour-du-pin", "l'isle-d'abeau", "l'isle d'abeau",
		"tassin-la-demi-lune", "sainte-foy-lès-lyon", "sainte-foy-les-lyon",
		"decines-charpieu", "décines-charpieu", "meyzieu", "genas",
		"givors", "rilleux-la-pape",
		"tarare", "l'Arbresle", "l'arbresle",

		// ---- Bourgogne-Franche-Comté ----
		"dijon", "besançon", "besancon", "belfort", "montbeliard", "montbéliard",
		"chalon-sur-saone", "chalon-sur-saône", "chalon sur saone", "chalon sur saône",
		"nevers", "auxerre", "saint-dizier", "chaumont",
		"macôn", "macon", "le creusot", "autun",
		"vesoul", "lons-le-saunier", "pontarlier",
		"chenove", "chenôve", "quétigny", "quetigny",
		"talant", "longvic", "beaune",
		"morteau", "dole", "sens",

		// ---- Bretagne ----
		"rennes", "brest", "quimper", "lorient", "vannes",
		"saint-brieuc", "saint brieuc", "lannion", "morlaix",
		"douarnenez", "concarneau", "fouesnant",
		"redon", "fougères", "vitré", "vitre",
		"dinan", "dinard", "saint-malo", "saint malo",
		"ploermel", "ploërmel", "guingamp",
		"lamballe", "loudéac", "pontivy",
		"auray", "hennebont", "lanester", "plemeur",
		"cesson-sévigné", "cesson-sevigne", "bruz", "chateaugiron", "châteaugiron",

		// ---- Centre-Val de Loire ----
		"tours", "orléans", "orleans", "blois", "bourges", "châteauroux", "chateauroux",
		"chartres", "dreux", "joue-lès-tours", "joué-lès-tours",
		"vernouillet", "luce", "chambray-lès-tours",
		"saint-jean-de-braye", "saint-jean-de-la-ruelle",
		"fleury-les-aubrais", "olivet",
		"saint-cyr-sur-loire", "fondettes",
		"romorantin-lanthenay", "vendôme", "vendome",
		"montargis", "pithiviers", "givry",

		// ---- Corse ----
		"ajaccio", "bastia", "porto-vecchio", "corte",
		"calvi", "l'île-rousse", "l'ile-rousse", "propriano",
		"ghisonaccia", "biguglia", "borgo",

		// ---- Grand Est ----
		"strasbourg", "reims", "metz", "mets", "nancy", "mulhouse", "colmar",
		"troyes", "charleville-mézières", "charleville-mezieres",
		"chalons-en-champagne", "châlons-en-champagne",
		"epinal", "épinal", "bar-le-duc",
		"saint-dizier", "sedan",
		"thionville", "sarreguemines", "forbach", "saint-avold",
		"haguenau", "selestat", "sélestat", "illkirch-graffenstaden",
		"schiltigheim", "lingolsheim", "ostwald",
		"hoff", "illzach", "wittenheim", "kingersheim", "rixheim",
		"vandoeuvre-lès-nancy", "vandœuvre-lès-nancy",
		"laxou", "maxéville", "saint-max",
		"lunéville", "luneville", "pont-à-mousson", "pont-a-mousson",
		"toul", "verdun",

		// ---- Hauts-de-France ----
		"lille", "roubaix", "tourcoing", "villeneuve-d'ascq", "villeneuve d'ascq",
		"amiens", "beauvais", "compiegne", "compiegne",
		"saint-quentin", "soissons", "laon",
		"douai", "lens", "lievin", "liévin", "béthune", "bethune",
		"dunkerque", "calais", "boulogne-sur-mer", "boulogne sur mer",
		"arras", "cambrai", "valenciennes",
		"creil", "chantilly", "senlis", "noyon",
		"abbeville", "montreuil",
		"halluin", "wattrelos", "marcq-en-baroeul", "marcq-en-barœul",
		"wasquehal", "croix", "loos", "hem",
		"roncq", "lambersart", "sainghin-en-weppes",

		// ---- Île-de-France ----
		"paris", "bobigny", "evry", "evry", "créteil", "creteil",
		"nanterre", "versailles", "melun",
		"boulogne-billancourt", "saint-denis", "saint denis",
		"montreuil", "argenteuil", "colombes",
		"courbevoie", "aubervilliers", "asnières-sur-seine", "asnieres-sur-seine",
		"levallois-perret", "issy-les-moulineaux", "issy les moulineaux",
		"clichy", "neuilly-sur-seine", "neuilly sur seine",
		"champigny-sur-marne", "champigny sur marne",
		"saint-maur-des-fossés", "saint-maur-des-fosses",
		"chelles", "meaux", "pontault-combault",
		"villejuif", "vincennes", "charenton-le-pont",
		"vitry-sur-seine", "vitry sur seine",
		"ivry-sur-seine", "ivry sur seine",
		"nogent-sur-marne", "nogent sur marne",
		"le perreux-sur-marne",
		"fontenay-sous-bois", "fontenay sous bois",
		"saint-mandé", "saint-mande", "joinville-le-pont",
		"cachan", "l'haÿ-les-roses", "l'hay-les-roses", "fresnes",
		"antony", "châtenay-malabry", "chatenay-malabry", "sceaux",
		"bourgelareine", "bourgelareine",
		"rouen", "sotteville-lès-rouen", "sotteville-les-rouen",
		// 92
		"puteaux", "suresnes", "rueil-malmaison",
		"meudon", "clamart", "sèvres", "sevres",
		"boulogne-billancourt", "montrouge", "bagneux",
		"chaville", "ville-d'avray",
		"garchnes", "courbevoie", "la garenne-colombes",
		"bois-colombes", "asnières-sur-seine",
		"gennevilliers", "villeneuve-la-garenne",
		// 93
		"saint-denis", "la courneuve", "stains",
		"aubervilliers", "la plaine saint-denis",
		"bobigny", "drancy", "le bourget",
		"bondy", "noisy-le-sec", "romainville",
		"montreuil", "bagnolet", "les lilas",
		"pantin", "pré-saint-gervais", "pre-saint-gervais",
		"livry-gargan", "sevran", "aunay-sous-bois",
		"tremblay-en-france", "villepinte",
		// 94
		"créteil", "creteil", "maisons-alfort", "saint-maur-des-fossés",
		"champigny-sur-marne", "joinville-le-pont",
		"vincennes", "nogent-sur-marne", "fontenay-sous-bois",
		"vitry-sur-seine", "ivry-sur-seine", "villejuif",
		"chevilly-larue", "l'haÿ-les-roses",
		"sucy-en-brie", "bry-sur-marne", "le perreux-sur-marne",
		"boissy-saint-léger", "bonneuil-sur-marne",
		"valenton", "villeneuve-saint-georges",
		"villeneuve-le-roi", "ablon-sur-seine",
		// 95
		"argenteuil", "cergy", "pontoise",
		"garges-lès-gonesse", "sarcelles",
		"villiers-le-bel", "taverny", "montmorency",
		"enghien-les-bains", "soisy-sous-montmorency",
		"franconville", "ermont", "herblay-sur-seine",
		"beauchamp", "le plessis-bouchard",
		"saint-leu-la-forêt", "saint-gratien",
		"deuil-la-barre", "montmagny", "groslay",
		// 77
		"melun", "dammarie-les-lys", "le mée-sur-seine",
		"chelles", "meaux", "claye-souilly",
		"lagny-sur-marne", "torcy", "bussy-saint-georges",
		"champs-sur-marne", "noisiel",
		"serris", "montevrain", "bailly-romainvilliers",
		"provins", "fontainebleau", "nemours",
		// 78
		"versailles", "saint-germain-en-laye",
		"sartrouville", "houilles", "carrières-sur-seine",
		"le vésinet", "le pecq", "maisons-laffitte",
		"poissy", "achères", "conflans-sainte-honorine",
		"les mureaux", "mantes-la-jolie", "mantes-la-ville",
		"plaisir", "trappes", "bois-d'arcy", "elancourt", "élancourt",
		"montigny-le-bretonneux", "guyancourt", "voisins-le-bretonneux",
		"rambouillet", "aubergenville", "limay",
		// 91
		"evry-courcouronnes", "évry-courcouronnes",
		"corbeil-essonnes", "corbeil-essonnes",
		"saint-michel-sur-orge", "sainte-genevieve-des-bois",
		"brétigny-sur-orge", "bretigny-sur-orge",
		"massy", "palaiseau", "les ulis",
		"orsay", "gif-sur-yvette", "saclay",
		"longjumeau", "savigny-sur-orge", "juvisy-sur-orge",
		"viry-châtillon", "viry-chatillon", "ris-orangis",

		// ---- Normandie ----
		"rouen", "le havre", "caen", "cherbourg-en-cotentin", "cherbourg",
		"alencôn", "alencon", "alençon",
		"evreux", "évreux", "dieppe", "vemreuil",
		"lisieux", "argentan", "flers",
		"granville", "saint-lô", "saint lo", "carentan",
		"bayeux", "honfleur", "deauville",
		"le grand-quevilly", "le petit-quevilly",
		"saint-étienne-du-rouvray", "saint-etienne-du-rouvray",
		"montivilliers", "fécamp",
		"louviers", "bernay", "pont-audemer",
		"gisors", "verneuil-sur-avre", "verneuil d'avre et d'iton",

		// ---- Nouvelle-Aquitaine ----
		"bordeaux", "limoges", "poitiers", "la rochelle",
		"pau", "bayonne", "biarritz", "anglet",
		"bergerac", "perigueux", "périgueux",
		"agen", "mont-de-marsan", "dax",
		"niort", "bressuire", "chatellerault", "châtellerault",
		"brive-la-gaillarde", "tulle",
		"guéret", "gueret",
		"pessac", "merignac", "mérignac", "gradignan", "talence",
		"villenave-d'ornon", "villenave d'ornon",
		"saint-médard-en-jalles", "begles", "bègles",
		"cenon", "floirac", "lormont",
		"la teste-de-buch", "arcachon", "gujan-mestras",
		"tarnos", "boucau", "hendaye",
		"saint-jean-de-luz", "ciboure",
		"morcenx-la-nouvelle", "saint-paul-lès-dax",
		"angoulême", "angouleme", "cognac", "soyaux",
		"jonzac", "saintes", "rochefort", "châtelaillon-plage",
		"royan",
		"boe", "marmande", "villeneuve-sur-lot",
		"lot-et-garonne",

		// ---- Occitanie ----
		"toulouse", "montpellier", "nimes", "nîmes",
		"perpignan", "beziers", "béziers",
		"carcassonne", "albi", "castres",
		"cahors", "rodez", "millau",
		"tarbes", "lourdes",
		"foix", "pamiers",
		"mende", "auch",
		"sete", "sète", "agde", "frontignan",
		"lunel", "mauguio", "castelnau-le-lez",
		"lattes", "jacou", "clapiers",
		"narbonne", "leucate", "gruissan",
		"carcassonne", "limoux", "castelnaudary",
		"blagnac", "colomiers", "tournefeuille",
		"plaisance-du-touch", "muret", "auterive",
		"castanet-tolosan", "ramonville-saint-agne",
		"labège", "saint-orens-de-gameville",
		"auch", "condom",
		"montauban", "montalbanais",

		// ---- Pays de la Loire ----
		"nantes", "angers", "le mans", "laval",
		"la roche-sur-yon", "saint-nazaire", "saint nazaire",
		"cholet", "les sables-d'olonne", "sables d'olonne",
		"sablé-sur-sarthe", "la flèche",
		"challans", "fontenay-le-comte", "montaigu",
		"mayenne", "château-gontier",
		"saumur", "sammur", "bressuire",
		"reze", "rezé", "orvault", "saint-herblain",
		"carquefou", "la chapelle-sur-erdre",
		"vertou", "bouguenais",
		"couëron", "indre",
		"treillieres", "treillières",
		"trélazé", "treize", "avrillé",
		"les ponts-de-cé",
		"allonnes", "arnage", "saint-saturnin",

		// ---- Provence-Alpes-Côte d'Azur ----
		"marseille", "nice", "toulon", "aix-en-provence",
		"avignon", "aix en provence",
		"antibes", "cannes", "grasse", "cagnes-sur-mer",
		"la seyne-sur-mer", "hyères", "hyeres",
		"frejus", "fréjus", "saint-raphaël", "saint-raphael",
		"draguignan", "brignoles",
		"digne-les-bains",
		"gap", "embrun",
		"arles", "tarascon", "salon-de-provence",
		"martigues", "istres", "miramas",
		"vitrolles", "marignane", "gardanne",
		"aubagne", "la ciotat", "cassis",
		"allis", "venelles",
		"vitrolles", "berre-l'étang",
		"les milles",
		"parc national des calanques",
		"cavalaire-sur-mer", "sainte-maxime",
		"saint-tropez", "port-grimaud",
		"orange", "carpentras", "cavaillon",
		"l'isle-sur-la-sorgue", "l'isle-sur-sorgue",
		"pertuis", "apt",
		"manosque", "oraiaon",
		"menton", "monaco",
		"valbonne", "biot", "vence",

		// ---- Grandes villes DOM-TOM ----
		// Guadeloupe
		"les abymes", "baie-mahault", "le gosier", "petit-bourg",
		"sainte-anne", "lamagol", "lamagol",
		"basse-terre", "pointe-à-pitre", "pointe-a-pitre",
		"saint-françois", "saint-francois",
		// Martinique
		"fort-de-france", "le lamentin", "saint-joseph",
		"schoelcher", "schœlcher",
		"sainte-marie", "la trinité", "saint-pierre",
		"le marin", "ducos", "rivière-pilote",
		// Guyane
		"cayenne", "matoury", "saint-laurent-du-maroni",
		"kourou", "sinamary", "mana",
		"sinnamary", "roura", "macouria",
		"st-georges-de-l'oyapock", "saint-georges-de-l'oyapock",
		// Réunion
		"saint-denis", "saint-paul", "saint-pierre",
		"le tampon", "saint-joseph",
		"l'étang-salé", "saint-benoît", "saint-benoi",
		"sainte-rosalie", "saint-andré", "saint-andre",
		"la possession", "le port",
		// Mayotte
		"mamoudzou", "koungou", "dzaoudzi",
		"pamandzi", "bandrele", "bandrélé",
		// Nouvelle-Calédonie
		"nouméa", "noumea", "dumbéa", "dumbea",
		"païta", "paita", "mont-dore", "le mont-dore",
		// Polynésie
		"papeete", "faaa", "punaauia", "pirae",
		// Wallis
		"mata-utu", "mata utu",
		// Saint-Pierre-et-Miquelon
		"saint-pierre",

		// ---- Villes supplémentaires (population +20k) ----
		"aix-les-bains", "ajaccio", "albertville", "albi", "alençon", "alencôn",
		"amiens", "anglet", "angers", "angoulême", "angouleme",
		"annecy", "antony", "antibes", "arles",
		"arras", "avignon", "auch",
		"aubagne", "aubervilliers", "aurillac",
		"auxerre", "bastia", "bayeux",
		"bayonne", "beauvais", "belfort", "besançon", "besancon",
		"béthune", "bethune", "beziers", "béziers",
		"biarritz", "blois", "bordeaux",
		"boulogne-billancourt", "bourges",
		"brest", "brive-la-gaillarde",
		"caen", "cahors", "calais", "cambrai",
		"cannes", "carcassonne", "carpentras",
		"castres", "cayenne",
		"cergy", "chalon-sur-saône", "chalon-sur-saone",
		"chambéry", "chambery",
		"charleville-mézières", "charleville-mezieres",
		"chartres", "châteauroux", "chateauroux",
		"châtellerault", "chatellerault",
		"chaumont", "cherbourg", "cherbourg-en-cotentin",
		"cholet", "clermont-ferrand", "clichy",
		"cognac", "colmar", "colombes",
		"compiegne", "compigne", "concarneau",
		"corbeil-essonnes", "courbevoie",
		"créteil", "creteil",
		"dax", "dieppe", "dijon",
		"digne-les-bains", "dinan",
		"douai", "drancy",
		"draguignan", "dreux", "dunkerque",
		"epinal", "épinal", "evreux", "évreux",
		"evry", "evry",
		"fécamp", "flers",
		"foix", "fontainebleau",
		"forbach", "fort-de-france",
		"frejus", "fréjus",
		"gap", "grasse", "grenoble",
		"guéret", "gueret",
		"guingamp",
		"haguenau", "hennebont",
		"hyères", "hyeres",
		"istres",
		"issoudun",
		"issoire",
		"isy-les-moulineaux",
		"ivry-sur-seine",
		"jonzac",
		"kourou",
		"la ciotat", "la courneuve",
		"la rochelle", "la roche-sur-yon",
		"la seyne-sur-mer",
		"lannion", "laon",
		"laval", "le havre", "le mans",
		"le puy-en-velay", "le tampon",
		"lens", "levallois-perret",
		"liévin", "lievin", "lille",
		"limoges", "limoux",
		"lisieux", "livry-gargan",
		"lons-le-saunier", "lorient",
		"lourdes", "louviers",
		"lunéville", "luneville",
		"lyon", "mâcon", "macon",
		"malakoff",
		"mamoudzou",
		"manosque",
		"mantes-la-jolie",
		"marseille",
		"martigues",
		"massy",
		"matoury",
		"mayenne",
		"mayotte",
		"meaux",
		"melun",
		"mende",
		"menton",
		"merignac", "mérignac",
		"metz", "mets",
		"meudon",
		"miramas",
		"mont-de-marsan",
		"montauban",
		"montbéliard", "montbeliard",
		"montélimar", "montelimar",
		"montluçon", "montlucon",
		"montpellier",
		"montreuil",
		"morlaix",
		"mulhouse",
		"nancy", "nantes",
		"nanterre",
		"narbonne",
		"nevers",
		"nice", "niort",
		"nîmes", "nimes",
		"noisy-le-grand",
		"nouméa", "noumea",
		"noyon",
		"orléans", "orleans",
		"palaiseau",
		"pamiers",
		"papeete",
		"paris",
		"pau",
		"périgueux", "perigueux",
		"perpignan",
		"pessac",
		"poitiers",
		"poissy",
		"ponteins",
		"pontarlier",
		"pontoise",
		"porto-vecchio",
		"provins",
		"puteaux",
		"quimper",
		"rambouillet",
		"redon",
		"reims",
		"rennes",
		"rodez",
		"romans-sur-isère", "romans-sur-isere",
		"roubaix",
		"rouen",
		"royan",
		"sabre-sur-sarthe",
		"saumur",
		"saint-brieuc",
		"saint-chamond",
		"saint-denis",
		"saint-dizier",
		"saint-étienne", "saint-etienne",
		"saint-germain-en-laye",
		"saint-herblain",
		"saint-lo", "saint-lô",
		"saint-malo",
		"saint-maur-des-fossés", "saint-maur-des-fosses",
		"saint-nazaire",
		"saint-ouen-sur-seine",
		"saint-priest",
		"saint-quentin",
		"saint-raphaël", "saint-raphael",
		"saintes",
		"salon-de-provence",
		"sarcelles",
		"saumur",
		"sceaux",
		"schiltigheim",
		"sedan",
		"selestat", "sélestat",
		"senlis",
		"sens",
		"serris",
		"sete", "sète",
		"soissons",
		"strasbourg",
		"suresnes",
		"talence",
		"tarbes",
		"thionville",
		"thonon-les-bains",
		"torcy",
		"toul",
		"toulon",
		"toulouse",
		"tourcoing",
		"tours",
		"trappes",
		"troyes",
		"tulle",
		"valence",
		"valenciennes",
		"vannes",
		"vaulx-en-velin",
		"vendôme", "vendome",
		"verdun",
		"vernouillet",
		"versailles",
		"vesoul",
		"vichy",
		"vienne",
		"villefranche-sur-saône",
		"villejuif",
		"villeneuve-d'ascq", "villeneuve d'ascq",
		"villeneuve-sur-lot",
		"villeurbanne",
		"vincennes",
		"vitre", "vitré",
		"vitry-sur-seine",
		"wattrelos"
	)));

	// Record pour stocker le résultat d'un match d'intention
	// value = valeur extraite (ville, organisme), matchedText = texte brut matché, position = index de début
	private record IntentMatch(String value, String matchedText, int position) {}

	// Mots-clés financement FR/EN
	private static final String[] FUNDING_KEYWORDS = {
		"financé", "financement", "finance", "bourse", "subvention",
		"fundé", "funding",
		"funded", "grant", "scholarship", "sponsored"
	};

	// Pattern de split pour le financement : nécessite "financé/funded/sponsored" + "par/by",
	// ou un mot-clé autonome suffisamment spécifique (bourse, subvention, grant, scholarship).
	private static final Pattern SPLIT_FUNDING_PATTERN = Pattern.compile(
		"(?:financé|funded|sponsored)\\s+(?:par|by)\\s+|bourse\\s+|subvention\\s+|grant\\s+|scholarship\\s+",
		Pattern.CASE_INSENSITIVE
	);

	// Nettoie les prépositions/articles devant un nom d'organisme
	private static final Pattern FUNDING_LEADING_CLEAN = Pattern.compile(
		"^(?:par |pour |by |de |du |des |d'|le |la |l'|les |un |une |aux |au |avec |chez |sans |sous |the |a |an )+",
		Pattern.CASE_INSENSITIVE
	);

	private final VectorSearchService vectorSearchService;
	private final PropositionTheseService propositionService;
	private final SearchRerankerService rerankerService;
	private final SujetEmbeddingRepository sujetEmbeddingRepository;
	private final PropositionTheseRepository propositionTheseRepository;
	private final EmbeddingIndexationService indexationService;

	public ScalewaySearchController(VectorSearchService vectorSearchService,
									PropositionTheseService propositionService,
									SearchRerankerService rerankerService,
									SujetEmbeddingRepository sujetEmbeddingRepository,
									PropositionTheseRepository propositionTheseRepository,
									EmbeddingIndexationService indexationService) {
		this.vectorSearchService = vectorSearchService;
		this.propositionService = propositionService;
		this.rerankerService = rerankerService;
		this.sujetEmbeddingRepository = sujetEmbeddingRepository;
		this.propositionTheseRepository = propositionTheseRepository;
		this.indexationService = indexationService;
	}

	private static String niveauPertinence(double compositeScore) {
		if (compositeScore >= SEUIL_TRES_PERTINENT) return "TRES_PERTINENT";
		if (compositeScore >= SEUIL_PERTINENT) return "PERTINENT";
		if (compositeScore >= SEUIL_FAIBLEMENT_PERTINENT) return "FAIBLEMENT_PERTINENT";
		return "MASQUE";
	}

	/**
	 * Filtre un DTO selon les paramètres de filtre passés dans la requête.
	 * Seuls les paramètres de filtre reconnus sont appliqués (discipline, localisation,
	 * defisSociete, laboratoire, ecole, annee, typeProposition).
	 * Les paramètres inconnus (query, limit, sortField…) sont ignorés.
	 */
	private boolean matchesFilters(PropositionTheseDto dto, Map<String, String> params) {
		if (dto == null) return false;

		// 1. Localisation
		String localisation = params.get("localisation");
		if (localisation != null && !localisation.isBlank()) {
			String postalCode = dto.getUniteRechercheCodePostal();
			if (postalCode == null || postalCode.isBlank()) return false;
			boolean matchesRegion = false;
			for (String region : localisation.split(";")) {
				region = region.trim();
				if (region.isEmpty()) continue;
				for (String dept : RegionsFrance.departementsFromRegion(region)) {
					if (postalCode.startsWith(dept)) { matchesRegion = true; break; }
				}
				if (matchesRegion) break;
			}
			if (!matchesRegion) return false;
		}

		// 2. Discipline
		String discipline = params.get("discipline");
		if (discipline != null && !discipline.isBlank()) {
			boolean matches = false;
			for (String val : discipline.split(";")) {
				String code = DomaineScientifique.codeFromLabel(val.trim());
				if (code != null && code.equals(dto.getDomaineScientifique())) {
					matches = true;
					break;
				}
			}
			if (!matches) return false;
		}

		// 3. Défis de société (combo domainesImpactListe + objectifsDeveloppementDurableListe)
		String defisSociete = params.get("defisSociete");
		if (defisSociete != null && !defisSociete.isBlank()) {
			for (String val : defisSociete.split(";")) {
				String lower = val.trim().toLowerCase();
				if (lower.isEmpty()) continue;
				boolean matchDomaines = dto.getDomainesImpactListe() != null
					&& dto.getDomainesImpactListe().stream().anyMatch(d -> d.toLowerCase().contains(lower));
				boolean matchOdd = dto.getObjectifsDeveloppementDurableListe() != null
					&& dto.getObjectifsDeveloppementDurableListe().stream().anyMatch(o -> o.toLowerCase().contains(lower));
				if (!matchDomaines && !matchOdd) return false;
			}
		}

		// 4. Laboratoire (LIKE)
		String laboratoire = params.get("laboratoire");
		if (laboratoire != null && !laboratoire.isBlank()) {
			String labo = dto.getUniteRechercheLibelle();
			if (labo == null) return false;
			boolean matches = false;
			for (String val : laboratoire.split(";")) {
				if (labo.toLowerCase().contains(val.trim().toLowerCase())) {
					matches = true;
					break;
				}
			}
			if (!matches) return false;
		}

		// 5. École (IN)
		String ecole = params.get("ecole");
		if (ecole != null && !ecole.isBlank()) {
			String ecoleVal = dto.getEtablissementLibelle();
			if (ecoleVal == null) return false;
			boolean matches = false;
			for (String val : ecole.split(";")) {
				if (ecoleVal.equals(val.trim())) {
					matches = true;
					break;
				}
			}
			if (!matches) return false;
		}

		// 6. Année
		String annee = params.get("annee");
		if (annee != null && !annee.isBlank()) {
			String anneeUniv = dto.getAnneeUniversitaire();
			if (anneeUniv == null) return false;
			boolean matches = false;
			for (String val : annee.split(";")) {
				if (anneeUniv.startsWith(val.trim())) {
					matches = true;
					break;
				}
			}
			if (!matches) return false;
		}

		// 7. Type proposition
		String typeProposition = params.get("typeProposition");
		if (typeProposition != null && !typeProposition.isBlank()) {
			String type = dto.getTypeProposition();
			if (type == null || !type.equals(typeProposition.trim())) return false;
		}

		return true;
	}

	/**
	 * Détecte si la requête contient une intention géographique :
	 * - mots-clés explicites (proche, près, near…)
	 * - préposition + nom de ville/région (à Paris, dans le Var, en Bretagne…)
	 */
	private boolean containsGeoIntent(String query) {
		if (query == null || query.isBlank()) return false;

		// Mots-clés insensibles à la casse
		String lower = query.toLowerCase().trim();
		for (String kw : GEO_KEYWORDS) {
			if (lower.contains(kw)) return true;
		}

		// Préposition + nom propre (ex: "à Paris", "dans le Var")
		Matcher m = GEO_PREPOSITION_PATTERN.matcher(query);
		return m.find();
	}

	/**
	 * Extrait la DERNIÈRE intention de localisation dans toute la requête.
	 * Scan indépendamment les patterns de proximité et les prépositions simples,
	 * et retourne celle qui apparaît le plus tard (position la plus élevée).
	 * Retourne un IntentMatch avec la valeur, le texte brut matché, et sa position.
	 */
	private IntentMatch extractCityFromGeoQuery(String query) {
		if (query == null || query.isBlank()) return null;

		// 1. Patterns de proximité : on prend la dernière occurrence valide
		IntentMatch lastProximity = null;
		Matcher proximityM = GEO_CITY_PROXIMITY_PATTERN.matcher(query);
		while (proximityM.find()) {
			String candidate = proximityM.group(1).trim().toLowerCase();
			if (FRENCH_LOCATIONS.contains(candidate) && !NON_LOCATION_WORDS.contains(candidate)) {
				lastProximity = new IntentMatch(candidate, proximityM.group(), proximityM.start());
			}
		}

		// 2. Prépositions simples : on prend la dernière occurrence valide
		IntentMatch lastPrep = null;
		Matcher prepM = GEO_CITY_PREPOSITION_PATTERN.matcher(query);
		while (prepM.find()) {
			String candidate = prepM.group(1).trim().toLowerCase();
			if (FRENCH_LOCATIONS.contains(candidate) && !NON_LOCATION_WORDS.contains(candidate)) {
				lastPrep = new IntentMatch(candidate, prepM.group(), prepM.start());
			}
		}

		// 3. On prend celle qui apparaît le plus tard dans la requête
		if (lastProximity != null && lastPrep != null) {
			return lastProximity.position() >= lastPrep.position() ? lastProximity : lastPrep;
		}
		if (lastProximity != null) return lastProximity;
		if (lastPrep != null) return lastPrep;
		return null;
	}

	/**
	 * Vérifie si un DTO correspond à la ville extraite
	 * (dans uniteRechercheVille ou etablissementVille)
	 */
	private boolean dtoMatchesCity(PropositionTheseDto dto, String cityLower) {
		if (dto == null || cityLower == null) return false;
		String urVille = dto.getUniteRechercheVille();
		String etabVille = dto.getEtablissementVille();
		return (urVille != null && urVille.toLowerCase().contains(cityLower))
			|| (etabVille != null && etabVille.toLowerCase().contains(cityLower));
	}

	/**
	 * Détecte si la requête contient une intention de financement
	 */
	private boolean containsFundingIntent(String query) {
		if (query == null || query.isBlank()) return false;
		String lower = query.toLowerCase().trim();
		for (String kw : FUNDING_KEYWORDS) {
			if (lower.contains(kw)) return true;
		}
		return false;
	}

	/**
	 * Extrait la DERNIÈRE intention de financement dans toute la requête.
	 * Trouve le dernier mot-clé funding, puis délimite l'organisme jusqu'au
	 * prochain mot-clé géographique (ou fin de chaîne), puis nettoie
	 * les prépositions/articles en tête.
	 * Ex: "thèses en France financé par établissement public proche de Paris"
	 *     → IntentMatch("établissement public", "financé par établissement public", pos)
	 */
	private IntentMatch extractFundingOrgFromQuery(String query) {
		if (query == null || query.isBlank()) return null;
		String lower = query.toLowerCase();

		// Trouver le DERNIER mot-clé funding
		int bestIdx = -1;
		String bestKw = null;
		for (String kw : FUNDING_KEYWORDS) {
			int idx = lower.lastIndexOf(kw);
			if (idx >= 0 && idx > bestIdx) {
				bestIdx = idx;
				bestKw = kw;
			}
		}
		if (bestKw == null) return null;

		// Texte après le mot-clé funding (dans la requête originale, préservant la casse)
		int afterStart = bestIdx + bestKw.length();
		String after = query.substring(afterStart);

		// Trouver le prochain mot-clé géo dans la suite du texte
		int boundary = after.length();
		Matcher geoM = GEO_CITY_PROXIMITY_PATTERN.matcher(after);
		if (geoM.find()) {
			boundary = geoM.start();
		} else {
			Matcher prepM = GEO_CITY_PREPOSITION_PATTERN.matcher(after);
			if (prepM.find()) {
				boundary = prepM.start();
			}
		}

		// Extraire la partie organisme (sans le mot-clé funding)
		String orgPart = after.substring(0, boundary).trim();

		// Nettoyer les prépositions/articles en tête
		Matcher clean = FUNDING_LEADING_CLEAN.matcher(orgPart);
		String cleanedOrg = clean.replaceAll("").trim();

		if (cleanedOrg.isEmpty()) return null;

		// matchedText = texte brut depuis le mot-clé funding jusqu'à la limite
		String matchedText = query.substring(bestIdx, bestIdx + bestKw.length() + boundary).trim();

		return new IntentMatch(cleanedOrg.toLowerCase(), matchedText, bestIdx);
	}

	/**
	 * Vérifie si un DTO correspond à l'organisme financeur extrait.
	 * Pour gérer les variantes de conjonction (ex: "collectivité locale ou territoriale"
	 * vs "collectivité locale et territorial"), l'org est splitté sur ou/et et
	 * chaque partie est testée individuellement.
	 */
	private boolean dtoMatchesFunding(PropositionTheseDto dto, String orgLower) {
		if (dto == null || orgLower == null) return false;
		String origine = dto.getFinancementOrigine();
		String employeur = dto.getFinancementEmployeur();
		String details = dto.getFinancementDetails();
		List<String> types = dto.getFinancementTypes();
		if (origine == null && employeur == null && details == null && (types == null || types.isEmpty())) return false;

		// 1. Split sur les conjonctions pour matcher des sous-parties
		String[] parts = orgLower.split("\\s+(?:ou|et|or|and)\\s+|\\s*,\\s*");
		for (String part : parts) {
			String trimmed = part.trim();
			if (trimmed.isEmpty()) continue;
			if (fieldContains(origine, trimmed) || fieldContains(employeur, trimmed) || fieldContains(details, trimmed) || typeListContains(types, trimmed)) {
				return true;
			}
		}

		// 2. Appariement progressif : "établissement public français" > 2 mots
		//    On retire le dernier mot à chaque itération pour matcher
		//    "Établissement public" stocké en base.
		String[] words = orgLower.split("\\s+");
		for (int end = words.length - 1; end >= 2; end--) {
			String sub = String.join(" ", java.util.Arrays.copyOfRange(words, 0, end)).trim();
			if (sub.length() < 4) continue;
			if (fieldContains(origine, sub) || fieldContains(employeur, sub) || fieldContains(details, sub) || typeListContains(types, sub)) {
				return true;
			}
		}

		return false;
	}

	/** Vérifie si la liste de types de financement contient la valeur (lowercase). */
	private boolean typeListContains(List<String> types, String value) {
		if (types == null) return false;
		for (String t : types) {
			if (t != null && t.toLowerCase().contains(value)) return true;
		}
		return false;
	}

	/** Vérifie si un champ texte (nullable) contient la valeur (lowercase). */
	private boolean fieldContains(String field, String value) {
		return field != null && field.toLowerCase().contains(value);
	}

	/**
	 * Nettoie la requête en retirant les textes matchés des intentions
	 * (localisation, financement), pour obtenir le cœur de la recherche.
	 * Ex: "sujets santé proche de Paris financé par le CNRPS"
	 *     → "sujets santé"
	 * Si le cœur obtenu est vide ou trop court (< 3 car), retourne la requête originale.
	 */
	private String cleanCoreQuery(String query, IntentMatch... matches) {
		if (query == null || query.isBlank()) return query;
		if (matches == null || matches.length == 0) return query;

		// Trier par position décroissante pour ne pas décaler les indices
		List<IntentMatch> list = java.util.Arrays.stream(matches)
			.filter(m -> m != null)
			.sorted((a, b) -> Integer.compare(b.position(), a.position()))
			.collect(Collectors.toList());

		if (list.isEmpty()) return query;

		String result = query;
		for (IntentMatch m : list) {
			int start = m.position();
			int end = start + m.matchedText().length();
			if (start >= 0 && end <= result.length()) {
				result = result.substring(0, start) + result.substring(end);
			}
		}
		result = result.trim();

		// Si le cœur est trop court, on garde la requête originale
		if (result.length() < 3) return query;
		return result;
	}

	@GetMapping("/propositions")
	public ResponseEntity<Map<String, Object>> search(
			@RequestParam Map<String, String> allParams) {

		String query = allParams.get("query");
		if (query == null || query.isBlank()) {
			return ResponseEntity.badRequest().body(Map.of("error", "Le paramètre query est requis"));
		}
		query = query.trim();
		int limit = Integer.parseInt(allParams.getOrDefault("limit", "100"));

		long startTime = System.currentTimeMillis();
		log.info("Recherche vectorielle via /api/scaleway/propositions (limit={})", limit);

		// Retirer le "?" final qui parasiterait les intentions
		boolean hasTrailingQuestionMark = query.endsWith("?");
		String cleanQuery = hasTrailingQuestionMark ? query.substring(0, query.length() - 1).trim() : query;

		// Extraire les intentions indépendamment dans toute la requête
		IntentMatch cityMatch = extractCityFromGeoQuery(cleanQuery);
		IntentMatch fundingMatch = extractFundingOrgFromQuery(cleanQuery);

		// Valider chaque intention indépendamment (0, 1 ou 2 possibles)
		String extractedCity = null;
		String extractedFundingOrg = null;
		if (cityMatch != null) {
			extractedCity = cityMatch.value();
		}
		if (fundingMatch != null) {
			extractedFundingOrg = fundingMatch.value();
			int fundingWordCount = extractedFundingOrg.split("\\s+").length;
			if (fundingWordCount > 8) {
				log.warn("Intention de financement invalide: \"{}\" ({} mots) — trop long, ignorée",
					extractedFundingOrg, fundingWordCount);
				fundingMatch = null;
				extractedFundingOrg = null;
			}
		}

		// Nettoyer la requête en retirant les textes matchés pour l'embedding
		String core = cleanCoreQuery(cleanQuery, cityMatch, fundingMatch);
		String vectorQuery = hasTrailingQuestionMark ? core + " ?" : core;
		if (!vectorQuery.equals(query)) {
			log.info("Requête nettoyée pour l'embedding: \"{}\" → \"{}\"", query, vectorQuery);
		}

		// Construire la map des intentions pour la réponse
		Map<String, String> intents = new LinkedHashMap<>();
		intents.put("core", vectorQuery);
		if (extractedCity != null) intents.put("location", extractedCity);
		if (extractedFundingOrg != null) intents.put("funding", extractedFundingOrg);

		// 1. Recherche vectorielle (sur la requête nettoyée)
		List<VectorSearchHit> hits = vectorSearchService.search(vectorQuery, limit);

		if (hits.isEmpty()) {
			return ResponseEntity.ok(Map.of(
				"query", query,
				"intents", intents,
				"results", List.of(),
				"totalResults", 0,
				"durationMs", System.currentTimeMillis() - startTime
			));
		}

		// 2. Récupérer les DTO des sujets trouvés
		List<Long> ids = hits.stream().map(VectorSearchHit::getSujetId).collect(Collectors.toList());
		Map<Long, PropositionTheseDto> theseMap = propositionService.findByIdInAsMap(ids);

		// 3. Calculer les scores composites (vectoriel + keywords)
		// Les tokens sont extraits du core nettoyé, pas de la requête brute
		// avec intentions, pour que le scoring ne dépende pas du financement/localisation
		List<String> tokens = rerankerService.extractTokens(vectorQuery);
		Map<Long, Double> compositeScores = new HashMap<>();
		Map<Long, Double> vectorScores = new HashMap<>();
		for (VectorSearchHit hit : hits) {
			PropositionTheseDto dto = theseMap.get(hit.getSujetId());
			vectorScores.put(hit.getSujetId(), hit.getScore());
			double score = hit.getScore();
			if (dto != null && !tokens.isEmpty()) {
				double kwScore = rerankerService.computeKeywordScore(tokens, dto);
				score = Math.min(score + kwScore * 1.2, 1.0);
			}
			compositeScores.put(hit.getSujetId(), score);
		}

		// 4. Trier par score vectoriel brut (similarité sémantique réelle)
		// et calculer le niveau de pertinence basé sur le composite (vectoriel + keywords)
		Map<Long, String> relevanceLevels = new HashMap<>();
		List<Long> sortedIds = vectorScores.entrySet().stream()
			.sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
			.limit(limit)
			.peek(e -> relevanceLevels.put(e.getKey(),
				niveauPertinence(compositeScores.getOrDefault(e.getKey(), 0.0))))
			.map(Map.Entry::getKey)
			.filter(id -> matchesFilters(theseMap.get(id), allParams))
			.collect(Collectors.toList());

		// 5. Construire la réponse
		List<PropositionTheseDto> results = new ArrayList<>();
		Map<Long, String> matchedTypes = new HashMap<>();
		Map<Long, String> matchedContent = new HashMap<>();
		for (Long id : sortedIds) {
			PropositionTheseDto dto = theseMap.get(id);
			if (dto != null) {
				results.add(dto);
				for (VectorSearchHit hit : hits) {
					if (id.equals(hit.getSujetId())) {
						matchedTypes.put(id, hit.getBlocType());
						matchedContent.put(id, hit.getContenuMatche());
						break;
					}
				}
			}
		}

		// 6. Détection localisation : construction de la map par résultat
		Map<String, Boolean> locationMatchedMap = new HashMap<>();
		boolean anyLocationMatched = false;
		if (extractedCity != null) {
			for (PropositionTheseDto dto : results) {
				boolean matches = dtoMatchesCity(dto, extractedCity);
				if (dto.getId() != null) {
					locationMatchedMap.put(String.valueOf(dto.getId()), matches);
				}
				if (matches) anyLocationMatched = true;
			}
		}
		boolean locationNotMatched = containsGeoIntent(query)
			&& extractedCity != null
			&& !anyLocationMatched;

		// 7. Détection financement : construction de la map par résultat
		Map<String, Boolean> fundingMatchedMap = new HashMap<>();
		int fundingMatchCount = 0;
		if (extractedFundingOrg != null) {
			for (PropositionTheseDto dto : results) {
				boolean matches = dtoMatchesFunding(dto, extractedFundingOrg);
				if (dto.getId() != null) {
					fundingMatchedMap.put(String.valueOf(dto.getId()), matches);
					if (matches) fundingMatchCount++;
				}
			}
			log.info("Funding: org=\"{}\", {}/{} résultats matchés", extractedFundingOrg, fundingMatchCount, results.size());
		}

		long duration = System.currentTimeMillis() - startTime;
		log.info("{} résultat(s) retourné(s) pour la recherche vectorielle (durée={}ms, locationNotMatched={})",
			results.size(), duration, locationNotMatched);
		if (extractedCity != null) {
			log.info("Location: city=\"{}\", {}/{} résultats matchés",
				extractedCity,
				locationMatchedMap.values().stream().filter(v -> v).count(),
				results.size());
		}

		return ResponseEntity.ok(Map.ofEntries(
			Map.entry("query", query),
			Map.entry("intents", intents),
			Map.entry("results", results),
			Map.entry("scores", compositeScores),
			Map.entry("vectorScores", vectorScores),
			Map.entry("relevanceLevels", relevanceLevels),
			Map.entry("matchedTypes", matchedTypes),
			Map.entry("matchedContent", matchedContent),
			Map.entry("totalResults", results.size()),
			Map.entry("durationMs", duration),
			Map.entry("locationNotMatched", locationNotMatched),
			Map.entry("locationMatchedMap", locationMatchedMap),
			Map.entry("fundingMatchedMap", fundingMatchedMap)
		));
	}

	@Transactional
	@PostMapping("/index/delete")
	public ResponseEntity<Map<String, Object>> deleteAllIndexes() {
		log.info("Suppression de tous les embeddings Scaleway demandée");
		int deleted = sujetEmbeddingRepository.findAll().size();
		sujetEmbeddingRepository.deleteAllInBatch();
		propositionTheseRepository.clearDateIndexationScaleway();
		log.info("{} embedding(s) Scaleway supprimé(s)", deleted);
		return ResponseEntity.ok(Map.of(
			"deleted", deleted,
			"message", deleted + " embeddings supprimés. Les index seront recréés au prochain passage du scheduler."
		));
	}

	@Transactional
	@PostMapping("/index/reindex")
	public ResponseEntity<Map<String, Object>> reindexAll() {
		log.info("Réindexation complète Scaleway demandée");
		sujetEmbeddingRepository.deleteAllInBatch();
		propositionTheseRepository.clearDateIndexationScaleway();
		indexationService.indexerTout();
		log.info("Réindexation Scaleway terminée");
		return ResponseEntity.ok(Map.of(
			"message", "Réindexation Scaleway terminée."
		));
	}
}
