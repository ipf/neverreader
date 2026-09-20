package com.neverreader.app.reader.internal.article.recommendations

import com.neverreader.BaseCoroutineTest
import com.neverreader.analytics.ContentOpenTracker
import com.neverreader.analytics.Tracker
import com.neverreader.analytics.appevents.ArticleViewEvents
import com.neverreader.repository.ItemRepository
import com.neverreader.repository.RecommendationsRepository
import com.neverreader.usecase.Save
import io.mockk.impl.annotations.SpyK
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.BeforeTest
import kotlin.test.Test

class EndOfArticleRecommendationsViewModelTest : BaseCoroutineTest() {

    @SpyK
    private val recommendationsRepository = mockk<RecommendationsRepository>(relaxed = true)

    @SpyK
    private val itemRepository = mockk<ItemRepository>(relaxed = true)
    @SpyK
    private val save = mockk<Save>(relaxed = true)
    @SpyK
    private val tracker = mockk<Tracker>(relaxed = true)

    @SpyK
    private val contentOpenTracker = mockk<ContentOpenTracker>(relaxed = true)

    private lateinit var subject: EndOfArticleRecommendationsViewModel

    @BeforeTest
    fun setup() {
        subject = EndOfArticleRecommendationsViewModel(
            recommendationsRepository,
            itemRepository,
            save,
            tracker,
            contentOpenTracker,
        )
    }

    @Test
    fun `WHEN a card is clicked THEN we track analytics`() {
        subject.onCardClicked("url", "recId")
        verify(exactly = 1) {
            contentOpenTracker.track(
                ArticleViewEvents.endOfArticleContentOpen(
                    "url",
                    "recId",
                )
            )
        }
    }
}