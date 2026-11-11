package com.aiohentai.cs.compat

/**
 * Genre/category browse sections per source, discovered from each site's own
 * navigation by scripts/gen_genres.py. Rows are static because CloudStream reads
 * `mainPage` synchronously when the provider is built.
 */
object Genres {

    val bySite: Map<String, List<Pair<String, String>>> = mapOf(
        "akih" to listOf("2D" to "/genre/2d", "3D" to "/genre/3d", "Ahegao" to "/genre/ahegao", "Anal" to "/genre/anal", "BDSM" to "/genre/bdsm", "Big Boobs" to "/genre/big-boobs", "Big breast" to "/genre/big-breast", "Bitch" to "/genre/bitch"),
        "animeidhentai" to listOf("Ahegao" to "/genre/ahegao", "Anal" to "/genre/anal", "Big Breasts" to "/genre/big-breasts", "Blowjob" to "/genre/blowjob", "Censored" to "/genre/censored", "Uncensored" to "/genre/hentai-uncensored", "Creampie" to "/genre/creampie", "Subbed" to "/genre/subbed"),
        "animesex" to listOf("Uncensored Hentai" to "/genres/uncensored"),
        "erotichentai" to listOf("Big Tit Step Mom" to "/category/big-tit-step-mom"),
        "hanime" to listOf("English Subbed" to "/genres/english-subbed", "Raw" to "/genres/raw", "Uncensored" to "/genres/uncensored", "Download" to "/index.php/category/hentai-anime", "Large Breasts" to "/genres/large-breasts", "Anal" to "/genres/anal", "Censored" to "/genres/censored", "CREAM PIE" to "/genres/cream-pie"),
        "hentaibros" to listOf("Uncensored" to "/genres/uncensored"),
        "hentaigasm" to listOf("UNCENSORED" to "/genre/uncensored", "PREVIEW" to "/genre/preview", "3D" to "/genre/3d", "Anal" to "/genre/anal", "Blowjob" to "/genre/blowjob", "Bondage" to "/genre/bondage", "Boobjob" to "/genre/boobjob", "Cheating" to "/genre/cheating"),
        "hentaihaven" to listOf("Uncensored Hentai" to "/genre/uncensored-hentai", "Incest" to "/genre/incest-hentai", "Lesbian" to "/genre/yaoi", "Tentacle Hentai" to "/genre/tentacle", "Milf" to "/genre/milf", "Young Hentai" to "/genre/young-hentai", "Yuri" to "/genre/yuri", "Ecchi" to "/genre/ecchi"),
        "hentaihavencom" to listOf("Femdom" to "/genre/femdom", "Milf" to "/genre/milf", "Yaoi" to "/genre/yaoi", "Tentacle" to "/genre/tentacle", "Uncensored Hentai" to "/genre/uncensored-hentai", "Harem" to "/genre/harem", "BDSM" to "/genre/bdsm", "BBW" to "/genre/bbw"),
        "hentaihavenred" to listOf("Uncensored Hentai" to "/hentai/genre/uncensored-hentai", "Milf" to "/hentai/genre/milf", "Harem" to "/hentai/genre/harem", "Tentacle" to "/hentai/genre/tentacle", "Yuri" to "/hentai/genre/yuri"),
        "hentaihorizon" to listOf("Videos" to "/category/videos", "Galleries" to "/category/images", "Hentai Vostfr" to "/category/hentai-vostfr/ "),
        "hentaijp" to listOf("Raw" to "/category/raw", "3D Movies" to "/category/3d-movies"),
        "hentaiking" to listOf("Big Boobs 1,027 titles" to "/genre/big-boobs", "Blow Job 1,000 titles" to "/genre/blow-job", "Creampie 997 titles" to "/genre/creampie", "Censored 895 titles" to "/genre/censored", "School Girl 523 titles" to "/genre/school-girl", "Uncensored 430 titles" to "/genre/uncensored", "Facial 343 titles" to "/genre/facial", "Anal 340 titles" to "/genre/anal"),
        "hentaimama" to listOf("Uncensored" to "/genre/uncensored", "3D" to "/genre/3d", "Creampie" to "/genre/creampie", "Demi-Human" to "/genre/demi-human", "Femdom" to "/genre/femdom", "Kiss" to "/genre/kiss", "Large Breasts" to "/genre/large-breasts", "Vanilla" to "/genre/vanilla"),
        "hentaiocean" to listOf("Milf Hentai" to "/genre/Milf", "Creampie Hentai" to "/genre/Creampie", "Ugly Bastard Hentai" to "/genre/Ugly Bastard"),
        "hentaiplay" to listOf("2026" to "/genre/2026", "Ahegao" to "/genre/ahegao", "School Girls" to "/genre/school-girls", "Virgins" to "/genre/virgins", "Large Breasts" to "/genre/large-breasts", "Lolicon" to "/genre/lolicon", "Housewife" to "/genre/housewife", "NTR" to "/genre/ntr"),
        "hentaisea" to listOf("3D Hentai" to "/genre/3d-hentai-2022", "Uncensored Hentai" to "/genre/uncensored", "Yaoi" to "/genre/yaoi", "Shota" to "/genre/shota", "Trap Hentai" to "/genre/trap-hentai", "Gender Bender" to "/genre/gender-bender", "Incest Hentai" to "/genre/incest", "Femdom Hentai" to "/genre/femdom"),
        "hentaistreamtv" to listOf("Incest Hentai" to "/genre/incest-hentai", "Tentacle" to "/genre/tentacle", "Monster" to "/genre/monster", "Harem" to "/genre/harem", "Uncensored Hentai" to "/genre/uncensored-hentai", "Milf" to "/genre/milf", "Yuri" to "/genre/yuri", "3D Hentai" to "/genre/3d-hentai"),
        "hentaistube" to listOf("Uncensored Hentai" to "/genre/uncensored"),
        "hentaitubeonline" to listOf("Uncensored Hentai" to "/genre/uncensored", "3D" to "/genre/3d", "Action" to "/genre/action", "Adventure" to "/genre/adventure", "Ahegao" to "/genre/ahegao", "Anal" to "/genre/anal", "Beastiality" to "/genre/beastiality", "Blackmail" to "/genre/blackmail"),
        "hentaitv" to listOf("Vanilla" to "/genre/vanilla", "anal" to "/genre/anal", "Romance" to "/genre/romance", "Big Boobs" to "/genre/big-boobs", "Maid" to "/genre/maid", "Comedy" to "/genre/comedy", "incest" to "/genre/incest", "Glasses" to "/genre/glasses"),
        "hentaivideos" to listOf("Ahegao" to "/genre/Ahegao", "Tentacle Adventures" to "/genre/Tentacles", "Futanari" to "/genre/Futanari", "Yuri" to "/genre/Yuri", "Yaoi" to "/genre/Yaoi", "Housewives" to "/genre/Housewives", "Harem Madness" to "/genre/Harem", "Vampires" to "/genre/Vampire"),
        "hentaiyoga" to listOf("Resident Evil" to "/category/32/Resident-Evil", "Jill Valentine" to "/category/78/Jill-Valentine", "Futa" to "/category/10/Futa", "Marvel" to "/category/38/Marvel", "3D" to "/category/2/3D", "Teen" to "/category/12/Teen", "Mom" to "/category/20/Mom", "Soulcalibur" to "/category/163/Soulcalibur"),
        "javguru" to listOf("SUBS" to "/category/english-subbed", "JAV" to "/category/jav", "MR" to "/category/decensored", "AMATEUR" to "/category/amateur", "IV" to "/category/idol", "4K" to "/category/4k", "FC2" to "/category/FC2"),
        "nekopoi" to listOf("Hentai" to "/category/hentai", "2D Animation" to "/category/2d-animation", "3D Hentai" to "/category/3d-hentai", "JAV" to "/category/jav", "JAV Cosplay" to "/category/jav-cosplay"),
        "okioto" to listOf("Niche Porn Sites" to "/category/asian"),
        "oppai" to listOf("uncensored Hentai 260 Episodes" to "/category/uncensored", "rape Hentai 607 Episodes" to "/category/rape", "incest Hentai 325 Episodes" to "/category/incest", "big boobs Hentai 1691 Episodes" to "/category/bigboobs", "shota Hentai 83 Episodes" to "/category/shota", "milf Hentai 244 Episodes" to "/category/milf", "creampie Hentai 1548 Episodes" to "/category/creampie", "succubus Hentai 52 Episodes" to "/category/succubus"),
        "uncensoredhentai" to listOf("blow job" to "/genres/blow-job", "big boobs" to "/genres/big-boobs", "censored" to "/genres/censored", "creampie" to "/genres/creampie", "HD" to "/genres/hd", "rape" to "/genres/rape", "school girl" to "/genres/school-girl", "uncensored" to "/genres/uncensored"),
        "watchanimehentai" to listOf("Uncensored" to "/genre/uncensored"),
        "watchhentai" to listOf("Ahegao" to "/genre/ahegao", "Anal" to "/genre/anal", "Blackmail" to "/genre/blackmail", "Blowjob" to "/genre/blowjob", "Bondage" to "/genre/bondage", "Creampie" to "/genre/creampie", "Cosplay" to "/genre/cosplay", "Dubbed Hentai" to "/genre/dubbed"),
        "zhentube" to listOf("Uncensored Hentai" to "/category/uncensored-hentai"),
    )
}
