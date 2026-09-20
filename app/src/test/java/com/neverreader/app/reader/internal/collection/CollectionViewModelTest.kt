package com.neverreader.app.reader.internal.collection

import com.neverreader.BaseCoroutineTest
import com.neverreader.analytics.ContentOpenTracker
import com.neverreader.analytics.Tracker
import com.neverreader.analytics.appevents.CollectionEvents
import com.neverreader.data.models.Collection
import com.neverreader.data.models.Story
import com.neverreader.repository.ArticleRepository
import com.neverreader.repository.CollectionRepository
import com.neverreader.repository.ItemRepository
import com.neverreader.usecase.Save
import io.mockk.coEvery
import io.mockk.impl.annotations.SpyK
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.BeforeTest
import kotlin.test.Test

class CollectionViewModelTest : BaseCoroutineTest() {

    @SpyK
    private val collectionRepository = mockk<CollectionRepository>(relaxed = true)
    @SpyK
    private val itemRepository = mockk<ItemRepository>(relaxed = true)
    @SpyK
    private val articleRepository = mockk<ArticleRepository>(relaxed = true)
    @SpyK
    private val save = mockk<Save>(relaxed = true)
    @SpyK
    private val tracker = mockk<Tracker>(relaxed = true)
    @SpyK
    private val contentOpenTracker = mockk<ContentOpenTracker>(relaxed = true)

    private lateinit var subject: CollectionViewModel

    @BeforeTest
    fun setup() {
        subject = CollectionViewModel(
            collectionRepository,
            itemRepository,
            articleRepository,
            save,
            tracker,
            contentOpenTracker,
        )
    }

    @Test
    fun `WHEN a card is clicked THEN the proper analytics is tracked`() {
        subject.onCardClicked("url")
        verify(exactly = 1) {
            contentOpenTracker.track(CollectionEvents.contentOpen("url"))
        }
    }

    @Test
    fun `WHEN a card is saved THEN the proper analytics is tracked`() {
        coEvery { collectionRepository.getCollection(any()) } returns Collection(
            authors = listOf(),
            title = "title",
            intro = "intro",
            stories = listOf(
                Story(
                    title = "title",
                    publisher = "publisher",
                    excerpt = "excerpt",
                    isSaved = false,
                    imageUrl = "url",
                    url = "url",
                    isCollection = false
                )
            )
        )

        subject.onInitialized("collectionurl")
        subject.onSaveClicked("url")

        verify(exactly = 1) {
            tracker.track(CollectionEvents.recommendationSaveClicked("url"))
        }
    }
}