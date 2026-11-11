package com.aiohentai.cs.compat

/**
 * Genre/category browse sections per source, discovered from each site's own
 * navigation by scripts/gen_genres.py. Rows are static because CloudStream reads
 * `mainPage` synchronously when the provider is built.
 */
object Genres {

    val bySite: Map<String, List<Pair<String, String>>> = mapOf(
        "animeidhentai" to listOf("Ahegao" to "/genre/ahegao", "Anal" to "/genre/anal", "Big Breasts" to "/genre/big-breasts", "Blowjob" to "/genre/blowjob", "Censored" to "/genre/censored", "Uncensored" to "/genre/hentai-uncensored", "Creampie" to "/genre/creampie", "Subbed" to "/genre/subbed"),
        "animesex" to listOf("Uncensored Hentai" to "/genres/uncensored"),
        "erotichentai" to listOf("Big Tit Step Mom" to "/category/big-tit-step-mom"),
        "hanimered" to listOf("See all" to "/tag/uncensored", "Incest hentai" to "/tag/incest", "Milf hentai" to "/tag/milf", "Lolicon Hentai" to "/tag/loli", "Anal Hentai" to "/tag/anal", "Creampie Hentai" to "/tag/creampie"),
        "hentaibros" to listOf("Uncensored" to "/genres/uncensored"),
        "hentaifreak" to listOf("IOS Sex Games" to "/tag/ios"),
        "hentaihaven" to listOf("Uncensored Hentai" to "/genre/uncensored-hentai", "Incest" to "/genre/incest-hentai", "Lesbian" to "/genre/yaoi", "Tentacle Hentai" to "/genre/tentacle", "Milf" to "/genre/milf", "Young Hentai" to "/genre/young-hentai", "Yuri" to "/genre/yuri", "Ecchi" to "/genre/ecchi"),
        "hentaihavencom" to listOf("Femdom" to "/genre/femdom", "Milf" to "/genre/milf", "Yaoi" to "/genre/yaoi", "Tentacle" to "/genre/tentacle", "Uncensored Hentai" to "/genre/uncensored-hentai", "Harem" to "/genre/harem", "BDSM" to "/genre/bdsm", "BBW" to "/genre/bbw"),
        "hentaihavenred" to listOf("Uncensored Hentai" to "/hentai/genre/uncensored-hentai", "Milf" to "/hentai/genre/milf", "Harem" to "/hentai/genre/harem", "Tentacle" to "/hentai/genre/tentacle", "Yuri" to "/hentai/genre/yuri"),
        "hentaihavenxxx" to listOf("3D Browse videos" to "/rule34/category/3d", "2D Browse videos" to "/rule34/category/2d", "Overwatch Browse videos" to "/rule34/category/overwatch", "Dead or Alive Browse videos" to "/rule34/category/dead-or-alive", "Final Fantasy Browse videos" to "/rule34/category/final-fantasy", "Hentai Browse videos" to "/rule34/category/hentai"),
        "hentaihd" to listOf("Action" to "/genre/action"),
        "hentaihorizon" to listOf("Videos" to "/category/videos", "Galleries" to "/category/images"),
        "hentaiking" to listOf("Big Boobs 1,027 titles" to "/genre/big-boobs", "Blow Job 1,000 titles" to "/genre/blow-job", "Creampie 997 titles" to "/genre/creampie", "Censored 895 titles" to "/genre/censored", "School Girl 523 titles" to "/genre/school-girl", "Uncensored 430 titles" to "/genre/uncensored", "Facial 343 titles" to "/genre/facial", "Anal 340 titles" to "/genre/anal"),
        "hentaimama" to listOf("Uncensored" to "/genre/uncensored", "3D" to "/genre/3d", "Creampie" to "/genre/creampie", "Demi-Human" to "/genre/demi-human", "Femdom" to "/genre/femdom", "Kiss" to "/genre/kiss", "Large Breasts" to "/genre/large-breasts", "Vanilla" to "/genre/vanilla"),
        "hentaiocean" to listOf("Milf Hentai" to "/genre/Milf", "Creampie Hentai" to "/genre/Creampie", "Ugly Bastard Hentai" to "/genre/Ugly Bastard"),
        "hentaiplay" to listOf("2026" to "/genre/2026", "Ahegao" to "/genre/ahegao", "School Girls" to "/genre/school-girls", "Virgins" to "/genre/virgins", "Large Breasts" to "/genre/large-breasts", "Lolicon" to "/genre/lolicon", "Housewife" to "/genre/housewife", "NTR" to "/genre/ntr"),
        "hentaisea" to listOf("3D Hentai" to "/genre/3d-hentai-2022", "Uncensored Hentai" to "/genre/uncensored", "Yaoi" to "/genre/yaoi", "Shota" to "/genre/shota", "Trap Hentai" to "/genre/trap-hentai", "Gender Bender" to "/genre/gender-bender", "Incest Hentai" to "/genre/incest", "Femdom Hentai" to "/genre/femdom"),
        "hentaistreamtv" to listOf("Incest Hentai" to "/genre/incest-hentai", "Tentacle" to "/genre/tentacle", "Monster" to "/genre/monster", "Harem" to "/genre/harem", "Uncensored Hentai" to "/genre/uncensored-hentai", "Milf" to "/genre/milf", "Yuri" to "/genre/yuri", "3D Hentai" to "/genre/3d-hentai"),
        "hentaistube" to listOf("Uncensored Hentai" to "/genre/uncensored"),
        "hentaitubeonline" to listOf("Uncensored Hentai" to "/genre/uncensored", "Action" to "/genre/action", "Adventure" to "/genre/adventure", "Ahegao" to "/genre/ahegao", "Anal" to "/genre/anal", "Beastiality" to "/genre/beastiality", "Blackmail" to "/genre/blackmail"),
        "hentaitv" to listOf("Vanilla" to "/genre/vanilla", "anal" to "/genre/anal", "Romance" to "/genre/romance", "Big Boobs" to "/genre/big-boobs", "Maid" to "/genre/maid", "Comedy" to "/genre/comedy", "incest" to "/genre/incest", "Glasses" to "/genre/glasses"),
        "hentaiyes" to listOf("HentaiHaven" to "/tag/hentaihaven", "Hanime" to "/tag/hanime", "Nhentai" to "/tag/nhentai", "Gelbooru" to "/tag/gelbooru", "Rule34" to "/tag/rule34"),
        "hentaiyoga" to listOf("Resident Evil" to "/category/32/Resident-Evil", "Jill Valentine" to "/category/78/Jill-Valentine", "Futa" to "/category/10/Futa", "Marvel" to "/category/38/Marvel", "3D" to "/category/2/3D", "Teen" to "/category/12/Teen", "Mom" to "/category/20/Mom", "Soulcalibur" to "/category/163/Soulcalibur"),
        "javguru" to listOf("SUBS" to "/category/english-subbed", "JAV" to "/category/jav", "MR" to "/category/decensored", "AMATEUR" to "/category/amateur", "IV" to "/category/idol", "4K" to "/category/4k", "FC2" to "/category/FC2"),
        "miohentai" to listOf("Show All" to "/tag/uncensored", "Blowjob 1467" to "/tag/blowjob", "Big Boobs 1119" to "/tag/big-boobs", "Creampie 1069" to "/tag/creampie", "3D hentai 681" to "/tag/3d-hentai-0", "Harem 599" to "/tag/harem"),
        "nekopoi" to listOf("Hentai" to "/category/hentai"),
        "okioto" to listOf("Niche Porn Sites" to "/category/asian"),
        "oppai" to listOf("uncensored Hentai 260 Episodes" to "/category/uncensored", "rape Hentai 607 Episodes" to "/category/rape", "incest Hentai 325 Episodes" to "/category/incest", "big boobs Hentai 1691 Episodes" to "/category/bigboobs", "shota Hentai 83 Episodes" to "/category/shota", "milf Hentai 244 Episodes" to "/category/milf", "creampie Hentai 1548 Episodes" to "/category/creampie", "succubus Hentai 52 Episodes" to "/category/succubus"),
        "uncensoredhentai" to listOf("blow job" to "/genres/blow-job", "big boobs" to "/genres/big-boobs", "censored" to "/genres/censored", "creampie" to "/genres/creampie", "HD" to "/genres/hd", "rape" to "/genres/rape", "school girl" to "/genres/school-girl", "uncensored" to "/genres/uncensored"),
        "watchanimehentai" to listOf("Uncensored" to "/genre/uncensored"),
        "watchhentai" to listOf("Ahegao" to "/genre/ahegao", "Anal" to "/genre/anal", "Blackmail" to "/genre/blackmail", "Blowjob" to "/genre/blowjob", "Bondage" to "/genre/bondage", "Creampie" to "/genre/creampie", "Cosplay" to "/genre/cosplay", "Dubbed Hentai" to "/genre/dubbed"),
        "xanimeporn" to listOf("Anime Sex Games" to "/tag/anime"),
        "zhentube" to listOf("Uncensored Hentai" to "/category/uncensored-hentai"),
    )
}
