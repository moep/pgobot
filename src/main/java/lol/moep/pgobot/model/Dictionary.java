package lol.moep.pgobot.model;

import POGOProtos.Enums.PokemonIdOuterClass;
import POGOProtos.Inventory.Item.ItemIdOuterClass;

import static POGOProtos.Inventory.Item.ItemIdOuterClass.ItemId.ITEM_TROY_DISK;

/**
 * Created by karsten.groll on 29.07.2016.
 */
public class Dictionary {
    private static final String[] pokemonNames = initPokemonNames();

    private static String[] initPokemonNames() {
        String[] names = new String[152];
        names[0] = "";

        // Pokemon Basic";
        names[1] = "Bisasam";
        names[2] = "Bisaknosp";
        names[3] = "Bisaflor";
        names[4] = "Glumanda";
        names[5] = "Glutexo";
        names[6] = "Glurak";
        names[7] = "Schiggy";
        names[8] = "Schillok";
        names[9] = "Turtok";
        names[10] = "Raupy";
        names[11] = "Safcon";
        names[12] = "Smettbo";
        names[13] = "Hornliu";
        names[14] = "Kokuna";
        names[15] = "Bibor";
        names[16] = "Taubsi";
        names[17] = "Tauboga";
        names[18] = "Tauboss";
        names[19] = "Rattfratz";
        names[20] = "Rattikarl";
        names[21] = "Habitak";
        names[22] = "Ibitak";
        names[23] = "Rettan";
        names[24] = "Arbok";
        names[25] = "Pikachu";
        names[26] = "Raichu";
        names[27] = "Sandan";
        names[28] = "Sandamer";
        names[29] = "Nidoran (w)";
        names[30] = "Nidorina";
        names[31] = "Nidoqueen";
        names[32] = "Nidoran (m)";
        names[33] = "Nidorino";
        names[34] = "Nidoking";
        names[35] = "Piepi";
        names[36] = "Pixi";
        names[37] = "Vulpix";
        names[38] = "Vulnona";
        names[39] = "Pummeluff";
        names[40] = "Knuddeluff";
        names[41] = "Zubat";
        names[42] = "Golbat";
        names[43] = "Myrapla";
        names[44] = "Duflor";
        names[45] = "Giflor";
        names[46] = "Paras";
        names[47] = "Parasek";
        names[48] = "Bluzuk";
        names[49] = "Omot";
        names[50] = "Digda";
        names[51] = "Digdri";
        names[52] = "Mauzi";
        names[53] = "Snobilikat";
        names[54] = "Enton";
        names[55] = "Entoron";
        names[56] = "Menki";
        names[57] = "Rasaff";
        names[58] = "Fukano";
        names[59] = "Arkani";
        names[60] = "Quapsel";
        names[61] = "Quaputzi";
        names[62] = "Quappo";
        names[63] = "Abra";
        names[64] = "Kadabra";
        names[65] = "Simsala";
        names[66] = "Machollo";
        names[67] = "Maschock";
        names[68] = "Machomei";
        names[69] = "Knofensa";
        names[70] = "Ultrigaria";
        names[71] = "Sarzenia";
        names[72] = "Tentacha";
        names[73] = "Tentoxa";
        names[74] = "Kleinstein";
        names[75] = "Georok";
        names[76] = "Geowaz";
        names[77] = "Ponita";
        names[78] = "Gallopa";
        names[79] = "Flegmon";
        names[80] = "Lahmus";
        names[81] = "Magnetilo";
        names[82] = "Magneton";
        names[83] = "Porenta";
        names[84] = "Dodu";
        names[85] = "Dodri";
        names[86] = "Jurob";
        names[87] = "Jugong";
        names[88] = "Sleima";
        names[89] = "Sleimok";
        names[90] = "Muschas";
        names[91] = "Austos";
        names[92] = "Nebulak";
        names[93] = "Alpollo";
        names[94] = "Gengar";
        names[95] = "Onix";
        names[96] = "Traumato";
        names[97] = "Hypno";
        names[98] = "Krabby";
        names[99] = "Kingler";
        names[100] = "Voltobal";
        names[101] = "Lektrobal";
        names[102] = "Owei";
        names[103] = "Kokowei";
        names[104] = "Tragosso";
        names[105] = "Knogga";
        names[106] = "Kicklee";
        names[107] = "Nockchan";
        names[108] = "Schlurp";
        names[109] = "Smogon";
        names[110] = "Smogmog";
        names[111] = "Rihorn";
        names[112] = "Rizeros";
        names[113] = "Chaneira";
        names[114] = "Tangela";
        names[115] = "Kangama";
        names[116] = "Seeper";
        names[117] = "Seemon";
        names[118] = "Goldini";
        names[119] = "Golking";
        names[120] = "Sterndu";
        names[121] = "Starmie";
        names[122] = "Pantimos";
        names[123] = "Sichlor";
        names[124] = "Rossana";
        names[125] = "Elektek";
        names[126] = "Magmar";
        names[127] = "Pinsir";
        names[128] = "Tauros";
        names[129] = "Karpador";
        names[130] = "Garados";
        names[131] = "Lapras";
        names[132] = "Ditto";
        names[133] = "Evoli";
        names[134] = "Aquana";
        names[135] = "Blitza";
        names[136] = "Flamara";
        names[137] = "Porygon";
        names[138] = "Amonitas";
        names[139] = "Amoroso";
        names[140] = "Kabuto";
        names[141] = "Kabutops";
        names[142] = "Aerodactyl";
        names[143] = "Relaxo";
        names[144] = "Arktos";
        names[145] = "Zapdos";
        names[146] = "Lavados";
        names[147] = "Dratini";
        names[148] = "Dragonir";
        names[149] = "Dragoran";
        names[150] = "Mewtu";
        names[151] = "Mew";

        return names;
    }

    public static String getNameFromPokemonId(PokemonIdOuterClass.PokemonId id) {
        return pokemonNames[id.getNumber()];
    }

    public static String getNameFromItemId(ItemIdOuterClass.ItemId id) {
        switch(id.getNumber()) {
            case 0:
                return "unbekannt";
            case 1:
                return "Pokéball";
            case 2:
                return "Superball";
            case 3:
                return "Ultraball";
            case 4:
                return "Meisterball";
            case 101:
                return "Trank";
            case 102:
                return "Supertrank";
            case 103:
                return "Hypertrank";
            case 104:
                return "Top-Trank";
            case 201:
                return "Beleber";
            case 202:
                // TODO Prüfen, wenn in der App gefunden
                return "Top-Beleber";
            case 301:
                return "Glücksei";
            case 401:
                return "Rauch";
            case 402:
                // TODO Prüfen, wenn in der App gefunden
                return "Rauch (würzig)";
            case 403:
                // TODO Prüfen, wenn in der App gefunden
                return "Rauch (cool)";
            case 404:
                // TODO Prüfen, wenn in der App gefunden
                return "Rauch (blumig";
//            case 501:
//                return ITEM_TROY_DISK.toString();
//            case 602:
//                return ITEM_X_ATTACK;
//            case 603:
//                return ITEM_X_DEFENSE;
//            case 604:
//                return ITEM_X_MIRACLE;
            case 701:
                return "Himmihbeere";
//            case 702:
//                return ITEM_BLUK_BERRY;
//            case 703:
//                return ITEM_NANAB_BERRY;
//            case 704:
//                return ITEM_WEPAR_BERRY;
//            case 705:
//                return ITEM_PINAP_BERRY;
//            case 801:
//                return ITEM_SPECIAL_CAMERA;
            case 901:
                return "Ei-Brutmaschine ∞";
            case 902:
                return "Ei-Brutmaschine";
//            case 1001:
//                return ITEM_POKEMON_STORAGE_UPGRADE;
//            case 1002:
//                return ITEM_ITEM_STORAGE_UPGRADE;
            default:
                return id.name();
        }
    }

}
