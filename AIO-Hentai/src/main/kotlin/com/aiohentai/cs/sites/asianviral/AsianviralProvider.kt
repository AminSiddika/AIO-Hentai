package com.aiohentai.cs.sites.asianviral

import com.aiohentai.cs.compat.AniyomiProvider
import com.lagradost.cloudstream3.TvType

class AsianviralProvider : AniyomiProvider(Asianviral()) {
    override val supportedTypes = setOf(TvType.NSFW, TvType.Movie)
    override val preferredType = TvType.NSFW
}
